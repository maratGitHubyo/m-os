package com.mos.quest.dto;

import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateQuestRequest(
        @NotBlank String title,
        String description,
        @NotNull QuestType type,
        @NotNull Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        QuestDefinitionStatus status
) {
}
