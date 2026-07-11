package com.mos.quest.dto;

import com.mos.quest.entity.PlayerQuest;
import com.mos.quest.entity.Quest;
import com.mos.quest.enums.PlayerQuestStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlayerQuestResponse(
        UUID id,
        UUID questId,
        UUID userId,
        UUID gameSessionId,
        PlayerQuestStatus status,
        Map<String, Object> progress,
        Instant completedAt,
        Instant createdAt,
        QuestResponse quest
) {

    public static PlayerQuestResponse from(PlayerQuest playerQuest) {
        Quest quest = playerQuest.getQuest();
        return new PlayerQuestResponse(
                playerQuest.getId(),
                quest.getId(),
                playerQuest.getUserId(),
                playerQuest.getGameSessionId(),
                playerQuest.getStatus(),
                playerQuest.getProgress(),
                playerQuest.getCompletedAt(),
                playerQuest.getCreatedAt(),
                QuestResponse.from(quest)
        );
    }
}
