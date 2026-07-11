package com.mos.score.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.InsufficientScoreException;
import com.mos.common.exception.LeaderboardDisabledException;
import com.mos.score.dto.LeaderboardEntryResponse;
import com.mos.score.dto.PlayerScoreResponse;
import com.mos.score.dto.ScoreTransactionResponse;
import com.mos.score.entity.PlayerScore;
import com.mos.score.entity.ScoreTransaction;
import com.mos.score.enums.ScoreCategory;
import com.mos.score.repository.PlayerScoreRepository;
import com.mos.score.repository.ScoreTransactionRepository;
import com.mos.quest.service.QuestService;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.victory.service.VictoryConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final PlayerScoreRepository playerScoreRepository;
    private final ScoreTransactionRepository scoreTransactionRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AuditService auditService;
    private final VictoryConditionService victoryConditionService;
    private final QuestService questService;

    public ScoreService(
            PlayerScoreRepository playerScoreRepository,
            ScoreTransactionRepository scoreTransactionRepository,
            SessionParticipantRepository sessionParticipantRepository,
            GameConfigRepository gameConfigRepository,
            AuditService auditService,
            VictoryConditionService victoryConditionService,
            @Lazy QuestService questService
    ) {
        this.playerScoreRepository = playerScoreRepository;
        this.scoreTransactionRepository = scoreTransactionRepository;
        this.sessionParticipantRepository = sessionParticipantRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.auditService = auditService;
        this.victoryConditionService = victoryConditionService;
        this.questService = questService;
    }

    @Transactional
    public ScoreTransactionResponse addPoints(
            UUID targetUserId,
            UUID gameSessionId,
            ScoreCategory category,
            long points,
            String reason,
            UUID performedByUserId
    ) {
        return applyScoreChange(
                targetUserId,
                gameSessionId,
                category,
                points,
                reason,
                performedByUserId,
                AuditAction.SCORE_ADD
        );
    }

    @Transactional
    public ScoreTransactionResponse subtractPoints(
            UUID targetUserId,
            UUID gameSessionId,
            ScoreCategory category,
            long points,
            String reason,
            UUID performedByUserId
    ) {
        return applyScoreChange(
                targetUserId,
                gameSessionId,
                category,
                -points,
                reason,
                performedByUserId,
                AuditAction.SCORE_SUBTRACT
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerScoreResponse> getMyScores(UUID userId, UUID gameSessionId) {
        Map<ScoreCategory, Long> pointsByCategory = playerScoreRepository
                .findByUserIdAndGameSessionId(userId, gameSessionId)
                .stream()
                .collect(Collectors.toMap(PlayerScore::getCategory, PlayerScore::getPoints));

        return java.util.Arrays.stream(ScoreCategory.values())
                .map(category -> new PlayerScoreResponse(
                        userId,
                        gameSessionId,
                        category,
                        pointsByCategory.getOrDefault(category, 0L)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(UUID gameSessionId) {
        ensureLeaderboardEnabled(gameSessionId);

        Map<UUID, Long> totalPoints = playerScoreRepository
                .findByGameSessionIdAndCategory(gameSessionId, ScoreCategory.TOTAL)
                .stream()
                .collect(Collectors.toMap(PlayerScore::getUserId, PlayerScore::getPoints));

        List<LeaderboardEntryResponse> sorted = sessionParticipantRepository.findByGameSessionId(gameSessionId).stream()
                .map(participant -> new LeaderboardEntryResponse(
                        0,
                        participant.getUser().getId(),
                        participant.getNicknameSnapshot(),
                        totalPoints.getOrDefault(participant.getUser().getId(), 0L)
                ))
                .sorted(Comparator.comparingLong(LeaderboardEntryResponse::points).reversed()
                        .thenComparing(LeaderboardEntryResponse::nickname))
                .toList();

        var ranked = new java.util.ArrayList<LeaderboardEntryResponse>();
        int rank = 0;
        long previousPoints = -1;

        for (int i = 0; i < sorted.size(); i++) {
            LeaderboardEntryResponse entry = sorted.get(i);
            if (i == 0 || entry.points() != previousPoints) {
                rank = i + 1;
            }
            previousPoints = entry.points();
            ranked.add(new LeaderboardEntryResponse(rank, entry.userId(), entry.nickname(), entry.points()));
        }

        return ranked;
    }

    private ScoreTransactionResponse applyScoreChange(
            UUID targetUserId,
            UUID gameSessionId,
            ScoreCategory category,
            long signedDelta,
            String reason,
            UUID performedByUserId,
            AuditAction auditAction
    ) {
        if (signedDelta == 0) {
            throw new BusinessException("Score change must be non-zero");
        }

        ensureParticipant(targetUserId, gameSessionId);

        PlayerScore score = playerScoreRepository
                .findByUserIdAndGameSessionIdAndCategory(targetUserId, gameSessionId, category)
                .orElseGet(() -> playerScoreRepository.save(PlayerScore.builder()
                        .userId(targetUserId)
                        .gameSessionId(gameSessionId)
                        .category(category)
                        .points(0L)
                        .build()));

        long newPoints = score.getPoints() + signedDelta;
        if (newPoints < 0) {
            throw new InsufficientScoreException();
        }

        score.setPoints(newPoints);
        playerScoreRepository.save(score);

        ScoreTransaction transaction = scoreTransactionRepository.save(ScoreTransaction.builder()
                .userId(targetUserId)
                .gameSessionId(gameSessionId)
                .category(category)
                .delta(signedDelta)
                .reason(reason)
                .build());

        auditService.log(
                performedByUserId,
                gameSessionId,
                auditAction,
                "PlayerScore",
                score.getId().toString(),
                reason,
                Map.of(
                        "targetUserId", targetUserId.toString(),
                        "category", category.name(),
                        "delta", signedDelta,
                        "pointsAfter", newPoints,
                        "transactionId", transaction.getId().toString()
                )
        );

        victoryConditionService.checkAfterGameDataChange(targetUserId, gameSessionId);
        questService.updateProgressAfterScoreChange(targetUserId, gameSessionId, category);

        return ScoreTransactionResponse.from(transaction);
    }

    private void ensureLeaderboardEnabled(UUID gameSessionId) {
        boolean enabled = gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> Boolean.TRUE.equals(config.getLeaderboardEnabled()))
                .orElse(false);

        if (!enabled) {
            throw new LeaderboardDisabledException();
        }
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }
}
