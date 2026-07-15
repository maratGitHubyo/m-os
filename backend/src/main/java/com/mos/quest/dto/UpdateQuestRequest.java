package com.mos.quest.dto;

import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;

import java.util.Map;
import java.util.UUID;

public record UpdateQuestRequest(
        String title,
        String description,
        Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        QuestDefinitionStatus status,
        QuestCompletionPolicy completionPolicy,
        Integer completionLimit,
        UUID assigneeUserId,
        Boolean assignToAll
) {
}
