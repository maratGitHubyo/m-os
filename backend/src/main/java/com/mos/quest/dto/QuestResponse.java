package com.mos.quest.dto;

import com.mos.quest.entity.Quest;
import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record QuestResponse(
        UUID id,
        UUID gameSessionId,
        String title,
        String description,
        QuestType type,
        QuestDefinitionStatus status,
        QuestCompletionPolicy completionPolicy,
        Integer completionLimit,
        UUID assigneeUserId,
        long completedCount,
        boolean available,
        Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        Instant createdAt
) {

    public static QuestResponse from(Quest quest) {
        return from(quest, 0L, true);
    }

    public static QuestResponse from(Quest quest, long completedCount, boolean available) {
        return new QuestResponse(
                quest.getId(),
                quest.getGameSessionId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getType(),
                quest.getStatus(),
                quest.getCompletionPolicy(),
                quest.getCompletionLimit(),
                quest.getAssigneeUserId(),
                completedCount,
                available,
                quest.getTargetConfig(),
                quest.getRewardConfig(),
                quest.getCreatedAt()
        );
    }
}
