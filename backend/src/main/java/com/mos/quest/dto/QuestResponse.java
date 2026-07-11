package com.mos.quest.dto;

import com.mos.quest.entity.Quest;
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
        Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        Instant createdAt
) {

    public static QuestResponse from(Quest quest) {
        return new QuestResponse(
                quest.getId(),
                quest.getGameSessionId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getType(),
                quest.getStatus(),
                quest.getTargetConfig(),
                quest.getRewardConfig(),
                quest.getCreatedAt()
        );
    }
}
