package com.mos.quest.dto;

import com.mos.quest.enums.QuestDefinitionStatus;

import java.util.Map;

public record UpdateQuestRequest(
        String title,
        String description,
        Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        QuestDefinitionStatus status
) {
}
