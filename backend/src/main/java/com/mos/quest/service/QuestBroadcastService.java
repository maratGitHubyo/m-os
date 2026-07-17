package com.mos.quest.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.SessionConfigurationException;
import com.mos.notification.enums.AppNotificationType;
import com.mos.notification.websocket.AppNotificationPublisher;
import com.mos.quest.dto.BroadcastQuestsResponse;
import com.mos.quest.dto.QuestAutoDistributeStatusResponse;
import com.mos.quest.entity.Quest;
import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import com.mos.quest.repository.QuestRepository;
import com.mos.seed.SocialQuestCatalog;
import com.mos.session.entity.GameConfig;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class QuestBroadcastService {

    public static final int AUTO_COUNT = 2;
    public static final int INTERVAL_MINUTES = 30;
    public static final String SOURCE_POOL_BROADCAST = "POOL_BROADCAST";

    private final QuestRepository questRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AuditService auditService;
    private final AppNotificationPublisher appNotificationPublisher;

    @Transactional
    public BroadcastQuestsResponse broadcast(UUID gameSessionId, int count, UUID performedByUserId) {
        if (count < 1 || count > 20) {
            throw new IllegalArgumentException("count must be between 1 and 20");
        }

        List<SessionParticipant> players = sessionParticipantRepository
                .findByGameSessionIdAndRole(gameSessionId, ParticipantRole.PLAYER);

        int created = 0;
        for (SessionParticipant participant : players) {
            UUID playerId = participant.getUser().getId();
            for (int i = 0; i < count; i++) {
                SocialQuestCatalog.SocialQuest template = pickRandom();
                questRepository.save(Quest.builder()
                        .gameSessionId(gameSessionId)
                        .title(template.title())
                        .description(template.description())
                        .type(QuestType.SOCIAL)
                        .status(QuestDefinitionStatus.ACTIVE)
                        .completionPolicy(QuestCompletionPolicy.EVERY_PLAYER)
                        .completionLimit(null)
                        .assigneeUserId(playerId)
                        .targetConfig(broadcastTargetConfig())
                        .rewardConfig(coinReward(template.rewardCoins()))
                        .build());
                created++;
            }
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("playerCount", players.size());
        meta.put("countPerPlayer", count);
        meta.put("questsCreated", created);

        UUID actorId = resolveAuditUserId(gameSessionId, performedByUserId);
        if (actorId != null) {
            auditService.log(
                    actorId,
                    gameSessionId,
                    AuditAction.QUEST_BROADCAST,
                    "Quest",
                    gameSessionId.toString(),
                    "Broadcast " + count + " quest(s) to " + players.size() + " player(s)",
                    meta
            );
        }

        if (!players.isEmpty() && count > 0) {
            String questWord = count == 1 ? "задание" : (count < 5 ? "задания" : "заданий");
            appNotificationPublisher.publish(
                    AppNotificationType.QUEST_BROADCAST,
                    gameSessionId,
                    null,
                    "Новые задания",
                    "Вам выдано " + count + " " + questWord
            );
        }

        return new BroadcastQuestsResponse(players.size(), created, count);
    }

    @Transactional
    public QuestAutoDistributeStatusResponse startAuto(UUID gameSessionId, UUID performedByUserId) {
        GameConfig config = requireConfig(gameSessionId);
        Instant now = Instant.now();
        config.setQuestAutoDistributeEnabled(true);
        config.setQuestAutoLastDistributedAt(now);
        gameConfigRepository.save(config);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.QUEST_AUTO_DISTRIBUTE_CHANGE,
                "GameConfig",
                config.getId().toString(),
                "Quest auto-distribute enabled",
                Map.of("enabled", true, "intervalMinutes", INTERVAL_MINUTES, "autoCount", AUTO_COUNT)
        );

        return statusFrom(config);
    }

    @Transactional
    public QuestAutoDistributeStatusResponse stopAuto(UUID gameSessionId, UUID performedByUserId) {
        GameConfig config = requireConfig(gameSessionId);
        config.setQuestAutoDistributeEnabled(false);
        gameConfigRepository.save(config);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.QUEST_AUTO_DISTRIBUTE_CHANGE,
                "GameConfig",
                config.getId().toString(),
                "Quest auto-distribute disabled",
                Map.of("enabled", false)
        );

        return statusFrom(config);
    }

    @Transactional(readOnly = true)
    public QuestAutoDistributeStatusResponse status(UUID gameSessionId) {
        return statusFrom(requireConfig(gameSessionId));
    }

    @Transactional
    public void runDueAutoDistributions() {
        Instant now = Instant.now();
        Duration interval = Duration.ofMinutes(INTERVAL_MINUTES);

        for (GameConfig config : gameConfigRepository.findByQuestAutoDistributeEnabledTrue()) {
            Instant last = config.getQuestAutoLastDistributedAt();
            if (last != null && last.plus(interval).isAfter(now)) {
                continue;
            }

            UUID sessionId = config.getGameSession().getId();
            broadcast(sessionId, AUTO_COUNT, null);
            config.setQuestAutoLastDistributedAt(now);
            gameConfigRepository.save(config);
        }
    }

    private GameConfig requireConfig(UUID gameSessionId) {
        return gameConfigRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new SessionConfigurationException("Game config not found for session"));
    }

    private QuestAutoDistributeStatusResponse statusFrom(GameConfig config) {
        Instant last = config.getQuestAutoLastDistributedAt();
        Instant next = null;
        if (Boolean.TRUE.equals(config.getQuestAutoDistributeEnabled()) && last != null) {
            next = last.plus(Duration.ofMinutes(INTERVAL_MINUTES));
        }
        return new QuestAutoDistributeStatusResponse(
                Boolean.TRUE.equals(config.getQuestAutoDistributeEnabled()),
                last,
                next,
                AUTO_COUNT,
                INTERVAL_MINUTES
        );
    }

    private static SocialQuestCatalog.SocialQuest pickRandom() {
        List<SocialQuestCatalog.SocialQuest> pool = SocialQuestCatalog.QUESTS;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static Map<String, Object> broadcastTargetConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("source", SOURCE_POOL_BROADCAST);
        return config;
    }

    private static Map<String, Object> coinReward(int amount) {
        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "COIN");
        reward.put("amount", amount);
        return reward;
    }

    private UUID resolveAuditUserId(UUID gameSessionId, UUID performedByUserId) {
        if (performedByUserId != null) {
            return performedByUserId;
        }
        return sessionParticipantRepository
                .findByGameSessionIdAndRole(gameSessionId, ParticipantRole.ADMIN)
                .stream()
                .findFirst()
                .map(participant -> participant.getUser().getId())
                .orElse(null);
    }
}
