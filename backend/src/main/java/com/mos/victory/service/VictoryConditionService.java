package com.mos.victory.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.victory.checker.VictoryConditionChecker;
import com.mos.victory.dto.CreateVictoryConditionRequest;
import com.mos.victory.dto.VictoryBroadcastMessage;
import com.mos.victory.dto.VictoryConditionAdminStatusResponse;
import com.mos.victory.dto.VictoryConditionResponse;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.entity.VictoryCondition;
import com.mos.victory.repository.VictoryConditionRepository;
import com.mos.victory.websocket.VictoryWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VictoryConditionService {

    private static final String FINISH_SESSION_ON_VICTORY_KEY = "finishSessionOnVictory";

    private final VictoryConditionRepository victoryConditionRepository;
    private final VictoryConditionChecker victoryConditionChecker;
    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AuditService auditService;
    private final VictoryWebSocketPublisher victoryWebSocketPublisher;

    @Transactional
    public VictoryConditionResponse createCondition(UUID gameSessionId, CreateVictoryConditionRequest request) {
        VictoryCondition condition = victoryConditionRepository.save(VictoryCondition.builder()
                .gameSessionId(gameSessionId)
                .type(request.type())
                .targetValue(request.targetValue() != null ? new HashMap<>(request.targetValue()) : new HashMap<>())
                .description(request.description())
                .active(request.active() != null ? request.active() : true)
                .build());

        return VictoryConditionResponse.forAdmin(condition);
    }

    @Transactional(readOnly = true)
    public List<VictoryConditionResponse> getConditionsForPlayer(UUID userId, UUID gameSessionId) {
        return victoryConditionRepository.findByGameSessionIdOrderByCreatedAtAsc(gameSessionId).stream()
                .map(condition -> {
                    VictoryProgress progress = victoryConditionChecker.evaluateCondition(
                            userId, gameSessionId, condition);
                    return VictoryConditionResponse.forPlayer(condition, userId, progress);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public VictoryConditionAdminStatusResponse getAdminStatus(UUID gameSessionId) {
        List<VictoryCondition> conditions = victoryConditionRepository.findByGameSessionIdOrderByCreatedAtAsc(gameSessionId);
        boolean anyAchieved = conditions.stream().anyMatch(c -> c.getAchievedAt() != null);

        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new BusinessException("Game session not found"));

        return new VictoryConditionAdminStatusResponse(
                gameSessionId,
                anyAchieved,
                session.getStatus() == GameSessionStatus.FINISHED,
                conditions.stream().map(VictoryConditionResponse::forAdmin).toList()
        );
    }

    @Transactional
    public void checkAfterGameDataChange(UUID userId, UUID gameSessionId) {
        List<VictoryCondition> pending = victoryConditionRepository
                .findByGameSessionIdAndActiveTrueAndAchievedAtIsNullOrderByCreatedAtAsc(gameSessionId);

        for (VictoryCondition condition : pending) {
            if (victoryConditionChecker.isSatisfied(userId, gameSessionId, condition)) {
                markAchieved(condition.getId(), userId, gameSessionId);
            }
        }
    }

    @Transactional
    public void markAchieved(UUID conditionId, UUID userId, UUID gameSessionId) {
        VictoryCondition condition = victoryConditionRepository.findByIdForUpdate(conditionId)
                .orElseThrow(() -> new BusinessException("Victory condition not found"));

        if (!condition.getGameSessionId().equals(gameSessionId)) {
            throw new BusinessException("Victory condition does not belong to this game session");
        }

        if (condition.getAchievedAt() != null) {
            return;
        }

        Instant achievedAt = Instant.now();
        condition.setAchievedAt(achievedAt);
        condition.setAchievedByUserId(userId);
        victoryConditionRepository.save(condition);

        boolean sessionFinished = maybeFinishSession(gameSessionId);

        auditService.log(
                userId,
                gameSessionId,
                AuditAction.VICTORY_ACHIEVED,
                "VictoryCondition",
                condition.getId().toString(),
                "Victory condition achieved: " + condition.getDescription(),
                Map.of(
                        "conditionId", condition.getId().toString(),
                        "type", condition.getType().name(),
                        "achievedByUserId", userId.toString(),
                        "sessionFinished", sessionFinished
                )
        );

        victoryWebSocketPublisher.publishVictory(new VictoryBroadcastMessage(
                gameSessionId,
                condition.getId(),
                condition.getType(),
                condition.getDescription(),
                userId,
                achievedAt,
                sessionFinished
        ));
    }

    private boolean maybeFinishSession(UUID gameSessionId) {
        boolean finishOnVictory = gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> config.getCustomSettings())
                .map(settings -> settings.get(FINISH_SESSION_ON_VICTORY_KEY))
                .map(value -> Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value)))
                .orElse(false);

        if (!finishOnVictory) {
            return false;
        }

        GameSession session = gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new BusinessException("Game session not found"));

        if (session.getStatus() != GameSessionStatus.FINISHED) {
            session.setStatus(GameSessionStatus.FINISHED);
            gameSessionRepository.save(session);
            return true;
        }

        return session.getStatus() == GameSessionStatus.FINISHED;
    }
}
