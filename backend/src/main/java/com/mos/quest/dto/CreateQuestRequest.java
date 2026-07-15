package com.mos.quest.dto;

import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;
import java.util.UUID;

public record CreateQuestRequest(
        @NotBlank String title,
        String description,
        @NotNull QuestType type,
        Map<String, Object> targetConfig,
        Map<String, Object> rewardConfig,
        QuestDefinitionStatus status,
        QuestCompletionPolicy completionPolicy,
        @Positive Integer completionLimit,
        UUID assigneeUserId
) {
}
