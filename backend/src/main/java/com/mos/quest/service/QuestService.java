package com.mos.quest.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.QuestAlreadyCompletedException;
import com.mos.common.exception.QuestLimitReachedException;
import com.mos.common.exception.QuestNotActiveException;
import com.mos.common.exception.QuestNotAssignedException;
import com.mos.common.exception.QuestNotFoundException;
import com.mos.common.exception.QuestNotStartedException;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.service.ItemService;
import com.mos.notification.enums.AppNotificationType;
import com.mos.notification.websocket.AppNotificationPublisher;
import com.mos.quest.dto.CompleteQuestRequest;
import com.mos.quest.dto.CreateQuestRequest;
import com.mos.quest.dto.PlayerQuestResponse;
import com.mos.quest.dto.QuestResponse;
import com.mos.quest.dto.StartQuestResponse;
import com.mos.quest.dto.UpdateQuestRequest;
import com.mos.quest.entity.PlayerQuest;
import com.mos.quest.entity.Quest;
import com.mos.quest.enums.PlayerQuestStatus;
import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import com.mos.quest.repository.PlayerQuestRepository;
import com.mos.quest.repository.QuestRepository;
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
    private final AuditService auditService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final AppNotificationPublisher appNotificationPublisher;

    @Transactional
    public QuestResponse createQuest(UUID gameSessionId, CreateQuestRequest request, UUID performedByUserId) {
        QuestCompletionPolicy policy = request.completionPolicy() != null
                ? request.completionPolicy()
                : QuestCompletionPolicy.EVERY_PLAYER;
        validateCompletionPolicy(policy, request.completionLimit());
        validateRewardConfig(request.rewardConfig());
        UUID assigneeUserId = resolveAssignee(gameSessionId, request.assigneeUserId());

        Quest quest = questRepository.save(Quest.builder()
                .gameSessionId(gameSessionId)
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(request.status() != null ? request.status() : QuestDefinitionStatus.ACTIVE)
                .completionPolicy(policy)
                .completionLimit(policy == QuestCompletionPolicy.LIMITED ? request.completionLimit() : null)
                .assigneeUserId(assigneeUserId)
                .targetConfig(request.targetConfig() != null ? new HashMap<>(request.targetConfig()) : new HashMap<>())
                .rewardConfig(request.rewardConfig() != null ? new HashMap<>(request.rewardConfig()) : null)
                .build());

        Map<String, Object> createMeta = new HashMap<>();
        createMeta.put("questId", quest.getId().toString());
        createMeta.put("type", quest.getType().name());
        createMeta.put("status", quest.getStatus().name());
        createMeta.put("completionPolicy", quest.getCompletionPolicy().name());
        if (assigneeUserId != null) {
            createMeta.put("assigneeUserId", assigneeUserId.toString());
        }

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.QUEST_CREATE,
                "Quest",
                quest.getId().toString(),
                "Quest created: " + quest.getTitle(),
                createMeta
        );

        if (quest.getStatus() == QuestDefinitionStatus.ACTIVE) {
            if (assigneeUserId != null) {
                appNotificationPublisher.publish(
                        AppNotificationType.QUEST_ASSIGNED,
                        gameSessionId,
                        assigneeUserId,
                        "Новый квест",
                        "Вам назначен квест «" + quest.getTitle() + "»"
                );
            } else {
                appNotificationPublisher.publish(
                        AppNotificationType.QUEST_ASSIGNED,
                        gameSessionId,
                        null,
                        "Новый квест",
                        "Доступен квест «" + quest.getTitle() + "»"
                );
            }
        }

        return QuestResponse.from(quest, 0L, true);
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
            validateRewardConfig(request.rewardConfig());
            quest.setRewardConfig(new HashMap<>(request.rewardConfig()));
        }
        if (request.status() != null) {
            quest.setStatus(request.status());
        }
        if (request.completionPolicy() != null || request.completionLimit() != null) {
            QuestCompletionPolicy policy = request.completionPolicy() != null
                    ? request.completionPolicy()
                    : quest.getCompletionPolicy();
            Integer limit = request.completionLimit() != null
                    ? request.completionLimit()
                    : quest.getCompletionLimit();
            validateCompletionPolicy(policy, limit);
            quest.setCompletionPolicy(policy);
            quest.setCompletionLimit(policy == QuestCompletionPolicy.LIMITED ? limit : null);
        }
        if (Boolean.TRUE.equals(request.assignToAll())) {
            quest.setAssigneeUserId(null);
        } else if (request.assigneeUserId() != null) {
            quest.setAssigneeUserId(resolveAssignee(gameSessionId, request.assigneeUserId()));
        }

        long completedCount = countCompleted(quest.getId());
        return QuestResponse.from(questRepository.save(quest), completedCount, isSlotAvailable(quest, completedCount));
    }

    @Transactional
    public StartQuestResponse startQuest(UUID questId, UUID userId, UUID gameSessionId) {
        Quest quest = getQuestForSession(questId, gameSessionId);
        ensureQuestAvailable(quest);
        ensureAssignedToUser(quest, userId);
        ensureParticipant(userId, gameSessionId);
        ensureSlotsAvailable(quest);

        return playerQuestRepository.findByQuestIdAndUserId(questId, userId)
                .map(playerQuest -> {
                    if (playerQuest.getStatus() == PlayerQuestStatus.COMPLETED) {
                        throw new QuestAlreadyCompletedException();
                    }
                    if (playerQuest.getStatus() == PlayerQuestStatus.FAILED) {
                        throw new BusinessException("Quest is no longer available for you");
                    }
                    return StartQuestResponse.from(PlayerQuestResponse.from(playerQuest, countCompleted(questId)));
                })
                .orElseGet(() -> StartQuestResponse.from(
                        PlayerQuestResponse.from(createPlayerQuest(quest, userId, gameSessionId), countCompleted(questId))
                ));
    }

    @Transactional
    public PlayerQuestResponse completeQuest(
            UUID questId,
            UUID userId,
            UUID gameSessionId,
            CompleteQuestRequest request
    ) {
        Quest quest = getQuestForSession(questId, gameSessionId);
        ensureQuestAvailable(quest);
        ensureAssignedToUser(quest, userId);
        ensureParticipant(userId, gameSessionId);

        PlayerQuest playerQuest = playerQuestRepository.findByQuestIdAndUserId(questId, userId)
                .orElseThrow(QuestNotStartedException::new);

        if (playerQuest.getStatus() == PlayerQuestStatus.COMPLETED) {
            throw new QuestAlreadyCompletedException();
        }
        if (playerQuest.getStatus() != PlayerQuestStatus.ACTIVE) {
            throw new BusinessException("Quest cannot be completed in current status");
        }

        ensureSlotsAvailable(quest);

        if (request != null && request.note() != null && !request.note().isBlank()) {
            playerQuest.setCompletionNote(request.note().trim());
        }

        if (quest.getType() == QuestType.SOCIAL) {
            Map<String, Object> progress = new HashMap<>();
            progress.put("current", 1);
            progress.put("target", 1);
            playerQuest.setProgress(progress);
        }

        completeQuest(playerQuest, quest, userId);

        if (quest.getCompletionPolicy() == QuestCompletionPolicy.LIMITED
                && countCompleted(quest.getId()) >= quest.getCompletionLimit()) {
            failOtherActiveQuests(quest, playerQuest.getId());
        }

        return PlayerQuestResponse.from(playerQuest, countCompleted(quest.getId()));
    }

    @Transactional
    public int closeIncompleteQuests(UUID gameSessionId, UUID performedByUserId) {
        List<Quest> activeQuests = questRepository
                .findByGameSessionIdAndStatusOrderByCreatedAtAsc(gameSessionId, QuestDefinitionStatus.ACTIVE);

        for (Quest quest : activeQuests) {
            quest.setStatus(QuestDefinitionStatus.DISABLED);
            questRepository.save(quest);
        }

        List<PlayerQuest> activePlayerQuests = playerQuestRepository
                .findByGameSessionIdAndStatus(gameSessionId, PlayerQuestStatus.ACTIVE);

        Instant now = Instant.now();
        for (PlayerQuest playerQuest : activePlayerQuests) {
            playerQuest.setStatus(PlayerQuestStatus.FAILED);
            playerQuest.setCompletedAt(now);
            playerQuestRepository.save(playerQuest);
        }

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.QUEST_CLOSE,
                "Quest",
                gameSessionId.toString(),
                "Incomplete quests closed before auction",
                Map.of(
                        "disabledQuests", activeQuests.size(),
                        "failedPlayerQuests", activePlayerQuests.size()
                )
        );

        if (!activePlayerQuests.isEmpty()) {
            appNotificationPublisher.publish(
                    AppNotificationType.QUEST_CLOSED,
                    gameSessionId,
                    null,
                    "Квесты закрыты",
                    "Незавершённые квесты завершены перед аукционом"
            );
        }

        return activePlayerQuests.size();
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> getAvailableQuests(UUID userId, UUID gameSessionId) {
        return questRepository.findByGameSessionIdAndStatusOrderByCreatedAtAsc(gameSessionId, QuestDefinitionStatus.ACTIVE)
                .stream()
                .map(quest -> {
                    long completedCount = countCompleted(quest.getId());
                    boolean available = isAvailableForUser(quest, userId, completedCount);
                    return QuestResponse.from(quest, completedCount, available);
                })
                .filter(QuestResponse::available)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> listQuestsForAdmin(UUID gameSessionId) {
        return questRepository.findAll().stream()
                .filter(quest -> quest.getGameSessionId().equals(gameSessionId))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(quest -> {
                    long completedCount = countCompleted(quest.getId());
                    return QuestResponse.from(quest, completedCount, isSlotAvailable(quest, completedCount));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerQuestResponse> getMyQuests(UUID userId, UUID gameSessionId) {
        return playerQuestRepository.findByUserIdAndGameSessionIdOrderByCreatedAtDesc(userId, gameSessionId).stream()
                .map(pq -> PlayerQuestResponse.from(pq, countCompleted(pq.getQuest().getId())))
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

    private void updateProgress(UUID userId, UUID gameSessionId, QuestType questType) {
        List<PlayerQuest> activeQuests = playerQuestRepository.findByUserIdAndGameSessionIdAndStatus(
                userId, gameSessionId, PlayerQuestStatus.ACTIVE);

        for (PlayerQuest playerQuest : activeQuests) {
            Quest quest = playerQuest.getQuest();
            if (quest.getStatus() != QuestDefinitionStatus.ACTIVE || quest.getType() != questType) {
                continue;
            }
            if (quest.getType() == QuestType.SOCIAL || quest.getType() == QuestType.CUSTOM) {
                continue;
            }

            ProgressSnapshot snapshot = calculateProgress(quest, playerQuest, userId, gameSessionId);
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

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("questId", quest.getId().toString());
        metadata.put("playerQuestId", playerQuest.getId().toString());
        metadata.put("type", quest.getType().name());
        if (playerQuest.getCompletionNote() != null) {
            metadata.put("note", playerQuest.getCompletionNote());
        }

        auditService.log(
                userId,
                playerQuest.getGameSessionId(),
                AuditAction.QUEST_COMPLETE,
                "PlayerQuest",
                playerQuest.getId().toString(),
                "Quest completed: " + quest.getTitle(),
                metadata
        );

        grantRewards(quest, userId, playerQuest);
    }

    private void failOtherActiveQuests(Quest quest, UUID winnerPlayerQuestId) {
        Instant now = Instant.now();
        for (PlayerQuest other : playerQuestRepository.findByQuestIdAndStatus(quest.getId(), PlayerQuestStatus.ACTIVE)) {
            if (other.getId().equals(winnerPlayerQuestId)) {
                continue;
            }
            other.setStatus(PlayerQuestStatus.FAILED);
            other.setCompletedAt(now);
            playerQuestRepository.save(other);

            appNotificationPublisher.publish(
                    AppNotificationType.QUEST_SLOT_TAKEN,
                    other.getGameSessionId(),
                    other.getUserId(),
                    "Квест закрыт",
                    "«" + quest.getTitle() + "» выполнен другим игроком"
            );
        }
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
            case "NONE", "NULL" -> {
                // no reward
            }
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
        if (quest.getType() == QuestType.SOCIAL) {
            Map<String, Object> progress = new HashMap<>();
            progress.put("current", 0);
            progress.put("target", 1);
            return progress;
        }

        long target = resolveTarget(quest);
        long current = 0L;

        Map<String, Object> progress = new HashMap<>();
        progress.put("current", current);
        progress.put("target", target);
        return progress;
    }

    private ProgressSnapshot calculateProgress(
            Quest quest,
            PlayerQuest playerQuest,
            UUID userId,
            UUID gameSessionId
    ) {
        Map<String, Object> progress = playerQuest.getProgress() != null
                ? new HashMap<>(playerQuest.getProgress())
                : buildInitialProgress(quest, userId, gameSessionId);

        long target = getLongValue(progress, "target", resolveTarget(quest));
        long previousCurrent = getLongValue(progress, "current", 0L);
        long current = previousCurrent;

        switch (quest.getType()) {
            case COLLECT_ITEMS, FIND_LOCATIONS, COLLECT_NUMBERS -> current = previousCurrent + 1;
            case CUSTOM, SOCIAL -> {
                return new ProgressSnapshot(progress, previousCurrent, target, false, false);
            }
        }

        progress.put("current", current);
        progress.put("target", target);

        boolean changed = current != previousCurrent;
        boolean satisfied = target > 0 && current >= target;
        return new ProgressSnapshot(progress, current, target, changed, satisfied);
    }

    private long resolveTarget(Quest quest) {
        if (quest.getType() == QuestType.SOCIAL) {
            return 1L;
        }
        Map<String, Object> targetConfig = quest.getTargetConfig();
        return switch (quest.getType()) {
            case COLLECT_ITEMS, FIND_LOCATIONS, COLLECT_NUMBERS -> getLongValue(targetConfig, "count");
            case CUSTOM, SOCIAL -> getLongValue(targetConfig, "target", 1L);
        };
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

    private void ensureSlotsAvailable(Quest quest) {
        long completedCount = countCompleted(quest.getId());
        if (!isSlotAvailable(quest, completedCount)) {
            throw new QuestLimitReachedException();
        }
    }

    private boolean isSlotAvailable(Quest quest, long completedCount) {
        if (quest.getCompletionPolicy() != QuestCompletionPolicy.LIMITED) {
            return true;
        }
        return completedCount < quest.getCompletionLimit();
    }

    private boolean isAvailableForUser(Quest quest, UUID userId, long completedCount) {
        if (quest.getStatus() != QuestDefinitionStatus.ACTIVE) {
            return false;
        }
        if (quest.getAssigneeUserId() != null && !quest.getAssigneeUserId().equals(userId)) {
            return false;
        }
        if (!isSlotAvailable(quest, completedCount)) {
            return false;
        }
        return playerQuestRepository.findByQuestIdAndUserId(quest.getId(), userId)
                .map(pq -> false)
                .orElse(true);
    }

    private void ensureAssignedToUser(Quest quest, UUID userId) {
        if (quest.getAssigneeUserId() != null && !quest.getAssigneeUserId().equals(userId)) {
            throw new QuestNotAssignedException();
        }
    }

    private UUID resolveAssignee(UUID gameSessionId, UUID assigneeUserId) {
        if (assigneeUserId == null) {
            return null;
        }
        sessionParticipantRepository.findByUserIdAndGameSessionId(assigneeUserId, gameSessionId)
                .orElseThrow(() -> new BusinessException("Assignee is not a participant of this game session"));
        return assigneeUserId;
    }

    private long countCompleted(UUID questId) {
        return playerQuestRepository.countByQuestIdAndStatus(questId, PlayerQuestStatus.COMPLETED);
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }

    private void validateCompletionPolicy(QuestCompletionPolicy policy, Integer completionLimit) {
        if (policy == QuestCompletionPolicy.LIMITED && (completionLimit == null || completionLimit <= 0)) {
            throw new BusinessException("Completion limit is required for LIMITED policy");
        }
    }

    private void validateRewardConfig(Map<String, Object> rewardConfig) {
        if (rewardConfig == null || rewardConfig.isEmpty()) {
            return;
        }
        Object typeValue = rewardConfig.get("type");
        if (typeValue == null) {
            throw new BusinessException("Reward type is required");
        }
        String rewardType = typeValue.toString().toUpperCase();
        switch (rewardType) {
            case "COIN" -> {
                if (getLongValue(rewardConfig, "amount") <= 0) {
                    throw new BusinessException("Coin reward amount must be positive");
                }
            }
            case "ITEM" -> {
                if (rewardConfig.get("itemTemplateId") == null) {
                    throw new BusinessException("Item reward requires itemTemplateId");
                }
                UUID.fromString(rewardConfig.get("itemTemplateId").toString());
            }
            case "NONE" -> {
                // no reward
            }
            default -> throw new BusinessException("Unsupported quest reward type: " + rewardType);
        }
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
