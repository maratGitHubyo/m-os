package com.mos.numbers.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.DuplicateNumberValueException;
import com.mos.common.exception.NumberNotFoundException;
import com.mos.common.exception.NumberSessionMismatchException;
import com.mos.numbers.dto.CollectibleNumberResponse;
import com.mos.numbers.dto.CreateCollectibleNumberRequest;
import com.mos.numbers.dto.NumberCollectionProgressResponse;
import com.mos.numbers.dto.PlayerNumberResponse;
import com.mos.numbers.entity.CollectibleNumber;
import com.mos.numbers.entity.PlayerNumber;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.numbers.repository.PlayerNumberRepository;
import com.mos.quest.service.QuestService;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NumberService {

    private final CollectibleNumberRepository collectibleNumberRepository;
    private final PlayerNumberRepository playerNumberRepository;
    private final GameConfigRepository gameConfigRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final AuditService auditService;
    private final QuestService questService;

    public NumberService(
            CollectibleNumberRepository collectibleNumberRepository,
            PlayerNumberRepository playerNumberRepository,
            GameConfigRepository gameConfigRepository,
            SessionParticipantRepository sessionParticipantRepository,
            AuditService auditService,
            @Lazy QuestService questService
    ) {
        this.collectibleNumberRepository = collectibleNumberRepository;
        this.playerNumberRepository = playerNumberRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.sessionParticipantRepository = sessionParticipantRepository;
        this.auditService = auditService;
        this.questService = questService;
    }

    @Transactional
    public CollectibleNumberResponse createNumber(UUID gameSessionId, CreateCollectibleNumberRequest request) {
        if (collectibleNumberRepository.existsByGameSessionIdAndNumberValue(gameSessionId, request.numberValue())) {
            throw new DuplicateNumberValueException();
        }

        CollectibleNumber number = collectibleNumberRepository.save(CollectibleNumber.builder()
                .gameSessionId(gameSessionId)
                .numberValue(request.numberValue())
                .build());

        return CollectibleNumberResponse.from(number);
    }

    @Transactional
    public PlayerNumberResponse grantNumber(
            UUID targetUserId,
            UUID numberId,
            UUID gameSessionId,
            UUID performedByUserId
    ) {
        CollectibleNumber number = getNumberForSession(numberId, gameSessionId);
        ensureParticipant(targetUserId, gameSessionId);

        return playerNumberRepository.findByUserIdAndNumberId(targetUserId, numberId)
                .map(PlayerNumberResponse::from)
                .orElseGet(() -> grantNumberFirstTime(targetUserId, gameSessionId, performedByUserId, number));
    }

    @Transactional(readOnly = true)
    public List<PlayerNumberResponse> getMyNumbers(UUID userId, UUID gameSessionId) {
        return playerNumberRepository.findByUserIdAndGameSessionIdOrderByAcquiredAtDesc(userId, gameSessionId).stream()
                .map(PlayerNumberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NumberCollectionProgressResponse getProgress(UUID userId, UUID gameSessionId) {
        long total = gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> config.getNumbersTotal() != null ? config.getNumbersTotal().longValue() : 0L)
                .orElse(0L);
        long collected = playerNumberRepository.countByUserIdAndGameSessionId(userId, gameSessionId);

        return new NumberCollectionProgressResponse(collected, total);
    }

    private PlayerNumberResponse grantNumberFirstTime(
            UUID targetUserId,
            UUID gameSessionId,
            UUID performedByUserId,
            CollectibleNumber number
    ) {
        PlayerNumber playerNumber = playerNumberRepository.save(PlayerNumber.builder()
                .userId(targetUserId)
                .gameSessionId(gameSessionId)
                .number(number)
                .build());

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.NUMBER_GRANT,
                "PlayerNumber",
                playerNumber.getId().toString(),
                "Number granted: " + number.getNumberValue(),
                Map.of(
                        "numberId", number.getId().toString(),
                        "numberValue", number.getNumberValue(),
                        "targetUserId", targetUserId.toString()
                )
        );

        questService.updateProgressAfterNumberGrant(targetUserId, gameSessionId);

        return PlayerNumberResponse.from(playerNumber);
    }

    private CollectibleNumber getNumberForSession(UUID numberId, UUID gameSessionId) {
        CollectibleNumber number = collectibleNumberRepository.findById(numberId)
                .orElseThrow(NumberNotFoundException::new);

        if (!number.getGameSessionId().equals(gameSessionId)) {
            throw new NumberSessionMismatchException();
        }

        return number;
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }
}
