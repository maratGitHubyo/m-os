package com.mos.quest.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.QuestAlreadyCompletedException;
import com.mos.common.exception.QuestNotActiveException;
import com.mos.common.exception.QuestNotFoundException;
import com.mos.common.exception.QuestSessionMismatchException;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.service.ItemService;
import com.mos.quest.dto.CreateQuestRequest;
import com.mos.quest.dto.PlayerQuestResponse;
import com.mos.quest.dto.QuestResponse;
import com.mos.quest.dto.StartQuestResponse;
import com.mos.quest.dto.UpdateQuestRequest;
import com.mos.quest.entity.PlayerQuest;
import com.mos.quest.entity.Quest;
import com.mos.quest.enums.PlayerQuestStatus;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import com.mos.quest.repository.PlayerQuestRepository;
import com.mos.quest.repository.QuestRepository;
import com.mos.score.enums.ScoreCategory;
import com.mos.score.repository.PlayerScoreRepository;
import com.mos.score.service.ScoreService;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.service.WalletService;
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
public class QuestService {

    private final QuestRepository questRepository;
    private final PlayerQuestRepository playerQuestRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final PlayerScoreRepository playerScoreRepository;
    private final AuditService auditService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final ScoreService scoreService;

    @Transactional
    public QuestResponse createQuest(UUID gameSessionId, CreateQuestRequest request, UUID performedByUserId) {
        Quest quest = questRepository.save(Quest.builder()
                .gameSessionId(gameSessionId)
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(request.status() != null ? request.status() : QuestDefinitionStatus.ACTIVE)
                .targetConfig(request.targetConfig() != null ? new HashMap<>(request.targetConfig()) : new HashMap<>())
                .rewardConfig(request.rewardConfig() != null ? new HashMap<>(request.rewardConfig()) : null)
                .build());

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.QUEST_CREATE,
                "Quest",
                quest.getId().toString(),
                "Quest created: " + quest.getTitle(),
                Map.of(
                        "questId", quest.getId().toString(),
                        "type", quest.getType().name(),
                        "status", quest.getStatus().name()
                )
        );

        return QuestResponse.from(quest);
    }

    @Transactional
    public QuestResponse updateQuest(UUID questId, UUID gameSessionId, UpdateQuestRequest request) {
        Quest quest = getQuestForSession(questId, gameSessionId);

        if (request.title() != null) {
            quest.setTitle(request.title());
        }
        if (request.description() != null) {
            quest.setDescription(request.description());
        }
        if (request.targetConfig() != null) {
            quest.setTargetConfig(new HashMap<>(request.targetConfig()));
        }
        if (request.rewardConfig() != null) {
            quest.setRewardConfig(new HashMap<>(request.rewardConfig()));
        }
        if (request.status() != null) {
            quest.setStatus(request.status());
        }

        return QuestResponse.from(questRepository.save(quest));
    }

    @Transactional
    public StartQuestResponse startQuest(UUID questId, UUID userId, UUID gameSessionId) {
        Quest quest = getQuestForSession(questId, gameSessionId);
        ensureQuestAvailable(quest);
        ensureParticipant(userId, gameSessionId);

        return playerQuestRepository.findByQuestIdAndUserId(questId, userId)
                .map(playerQuest -> StartQuestResponse.from(PlayerQuestResponse.from(playerQuest)))
                .orElseGet(() -> StartQuestResponse.from(PlayerQuestResponse.from(createPlayerQuest(quest, userId, gameSessionId))));
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> getAvailableQuests(UUID gameSessionId) {
        return questRepository.findByGameSessionIdAndStatusOrderByCreatedAtAsc(gameSessionId, QuestDefinitionStatus.ACTIVE)
                .stream()
                .map(QuestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerQuestResponse> getMyQuests(UUID userId, UUID gameSessionId) {
        return playerQuestRepository.findByUserIdAndGameSessionIdOrderByCreatedAtDesc(userId, gameSessionId).stream()
                .map(PlayerQuestResponse::from)
                .toList();
    }

    @Transactional
    public void updateProgressAfterItemGrant(UUID userId, UUID gameSessionId) {
        updateProgress(userId, gameSessionId, QuestType.COLLECT_ITEMS);
    }

    @Transactional
    public void updateProgressAfterLocationDiscover(UUID userId, UUID gameSessionId) {
        updateProgress(userId, gameSessionId, QuestType.FIND_LOCATIONS);
    }

    @Transactional
    public void updateProgressAfterNumberGrant(UUID userId, UUID gameSessionId) {
        updateProgress(userId, gameSessionId, QuestType.COLLECT_NUMBERS);
    }

    @Transactional
    public void updateProgressAfterScoreChange(UUID userId, UUID gameSessionId, ScoreCategory category) {
        updateProgress(userId, gameSessionId, QuestType.REACH_SCORE, category);
    }

    private void updateProgress(UUID userId, UUID gameSessionId, QuestType questType) {
        updateProgress(userId, gameSessionId, questType, null);
    }

    private void updateProgress(UUID userId, UUID gameSessionId, QuestType questType, ScoreCategory scoreCategory) {
        List<PlayerQuest> activeQuests = playerQuestRepository.findByUserIdAndGameSessionIdAndStatus(
                userId, gameSessionId, PlayerQuestStatus.ACTIVE);

        for (PlayerQuest playerQuest : activeQuests) {
            Quest quest = playerQuest.getQuest();
            if (quest.getStatus() != QuestDefinitionStatus.ACTIVE || quest.getType() != questType) {
                continue;
            }

            ProgressSnapshot snapshot = calculateProgress(quest, playerQuest, userId, gameSessionId, scoreCategory);
            if (!snapshot.changed()) {
                continue;
            }

            playerQuest.setProgress(snapshot.progress());
            playerQuestRepository.save(playerQuest);

            auditService.log(
                    userId,
                    gameSessionId,
                    AuditAction.QUEST_PROGRESS,
                    "PlayerQuest",
                    playerQuest.getId().toString(),
                    "Quest progress updated: " + quest.getTitle(),
                    Map.of(
                            "questId", quest.getId().toString(),
                            "playerQuestId", playerQuest.getId().toString(),
                            "current", snapshot.current(),
                            "target", snapshot.target(),
                            "type", quest.getType().name()
                    )
            );

            if (snapshot.satisfied()) {
                completeQuest(playerQuest, quest, userId);
            }
        }
    }

    private void completeQuest(PlayerQuest playerQuest, Quest quest, UUID userId) {
        if (playerQuest.getStatus() == PlayerQuestStatus.COMPLETED) {
            throw new QuestAlreadyCompletedException();
        }

        playerQuest.setStatus(PlayerQuestStatus.COMPLETED);
        playerQuest.setCompletedAt(Instant.now());
        playerQuestRepository.save(playerQuest);

        auditService.log(
                userId,
                playerQuest.getGameSessionId(),
                AuditAction.QUEST_COMPLETE,
                "PlayerQuest",
                playerQuest.getId().toString(),
                "Quest completed: " + quest.getTitle(),
                Map.of(
                        "questId", quest.getId().toString(),
                        "playerQuestId", playerQuest.getId().toString(),
                        "type", quest.getType().name()
                )
        );

        grantRewards(quest, userId, playerQuest);
    }

    private void grantRewards(Quest quest, UUID userId, PlayerQuest playerQuest) {
        Map<String, Object> rewardConfig = quest.getRewardConfig();
        if (rewardConfig == null || rewardConfig.isEmpty()) {
            return;
        }

        String rewardType = String.valueOf(rewardConfig.get("type")).toUpperCase();
        UUID gameSessionId = playerQuest.getGameSessionId();
        String referenceId = playerQuest.getId().toString();

        switch (rewardType) {
            case "COIN" -> walletService.creditWithoutAudit(
                    userId,
                    gameSessionId,
                    getLongValue(rewardConfig, "amount"),
                    CoinTransactionType.QUEST,
                    referenceId,
                    "Quest reward: " + quest.getTitle()
            );
            case "ITEM" -> itemService.grantItemFromReward(
                    userId,
                    UUID.fromString(rewardConfig.get("itemTemplateId").toString()),
                    gameSessionId,
                    ItemAcquisitionSource.QUEST
            );
            case "SCORE" -> scoreService.addPoints(
                    userId,
                    gameSessionId,
                    parseScoreCategory(rewardConfig),
                    getLongValue(rewardConfig, "points"),
                    "Quest reward: " + quest.getTitle(),
                    userId
            );
            default -> throw new BusinessException("Unsupported quest reward type: " + rewardType);
        }
    }

    private PlayerQuest createPlayerQuest(Quest quest, UUID userId, UUID gameSessionId) {
        PlayerQuest playerQuest = playerQuestRepository.save(PlayerQuest.builder()
                .quest(quest)
                .userId(userId)
                .gameSessionId(gameSessionId)
                .status(PlayerQuestStatus.ACTIVE)
                .progress(buildInitialProgress(quest, userId, gameSessionId))
                .build());

        auditService.log(
                userId,
                gameSessionId,
                AuditAction.QUEST_START,
                "PlayerQuest",
                playerQuest.getId().toString(),
                "Quest started: " + quest.getTitle(),
                Map.of(
                        "questId", quest.getId().toString(),
                        "playerQuestId", playerQuest.getId().toString(),
                        "type", quest.getType().name()
                )
        );

        return playerQuest;
    }

    private Map<String, Object> buildInitialProgress(Quest quest, UUID userId, UUID gameSessionId) {
        long target = resolveTarget(quest);
        long current = quest.getType() == QuestType.REACH_SCORE
                ? resolveCurrentScore(quest, userId, gameSessionId)
                : 0L;

        Map<String, Object> progress = new HashMap<>();
        progress.put("current", current);
        progress.put("target", target);
        return progress;
    }

    private ProgressSnapshot calculateProgress(
            Quest quest,
            PlayerQuest playerQuest,
            UUID userId,
            UUID gameSessionId,
            ScoreCategory scoreCategory
    ) {
        Map<String, Object> progress = playerQuest.getProgress() != null
                ? new HashMap<>(playerQuest.getProgress())
                : buildInitialProgress(quest, userId, gameSessionId);

        long target = getLongValue(progress, "target", resolveTarget(quest));
        long previousCurrent = getLongValue(progress, "current", 0L);
        long current = previousCurrent;

        switch (quest.getType()) {
            case COLLECT_ITEMS, FIND_LOCATIONS, COLLECT_NUMBERS -> current = previousCurrent + 1;
            case REACH_SCORE -> {
                ScoreCategory category = parseScoreCategory(quest.getTargetConfig());
                if (scoreCategory != null && scoreCategory != category) {
                    return new ProgressSnapshot(progress, previousCurrent, target, false, previousCurrent >= target);
                }
                current = resolveCurrentScore(quest, userId, gameSessionId);
            }
            case CUSTOM -> {
                return new ProgressSnapshot(progress, previousCurrent, target, false, false);
            }
        }

        progress.put("current", current);
        progress.put("target", target);

        boolean changed = current != previousCurrent;
        boolean satisfied = target > 0 && current >= target;
        return new ProgressSnapshot(progress, current, target, changed, satisfied);
    }

    private long resolveCurrentScore(Quest quest, UUID userId, UUID gameSessionId) {
        ScoreCategory category = parseScoreCategory(quest.getTargetConfig());
        return playerScoreRepository
                .findByUserIdAndGameSessionIdAndCategory(userId, gameSessionId, category)
                .map(score -> score.getPoints())
                .orElse(0L);
    }

    private long resolveTarget(Quest quest) {
        Map<String, Object> targetConfig = quest.getTargetConfig();
        return switch (quest.getType()) {
            case REACH_SCORE -> getLongValue(targetConfig, "threshold");
            case COLLECT_ITEMS, FIND_LOCATIONS, COLLECT_NUMBERS -> getLongValue(targetConfig, "count");
            case CUSTOM -> getLongValue(targetConfig, "target", 0L);
        };
    }

    private ScoreCategory parseScoreCategory(Map<String, Object> config) {
        Object raw = config.get("category");
        if (raw == null) {
            return ScoreCategory.TOTAL;
        }
        try {
            return ScoreCategory.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid score category in quest config: " + raw);
        }
    }

    private long getLongValue(Map<String, Object> config, String key) {
        return getLongValue(config, key, null);
    }

    private long getLongValue(Map<String, Object> config, String key, Long defaultValue) {
        Object raw = config.get(key);
        if (raw == null) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new BusinessException("Missing required quest config value: " + key);
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid quest config value for " + key + ": " + raw);
        }
    }

    private Quest getQuestForSession(UUID questId, UUID gameSessionId) {
        return questRepository.findByIdAndGameSessionId(questId, gameSessionId)
                .orElseThrow(QuestNotFoundException::new);
    }

    private void ensureQuestAvailable(Quest quest) {
        if (quest.getStatus() != QuestDefinitionStatus.ACTIVE) {
            throw new QuestNotActiveException();
        }
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }

    private record ProgressSnapshot(
            Map<String, Object> progress,
            long current,
            long target,
            boolean changed,
            boolean satisfied
    ) {
    }
}
