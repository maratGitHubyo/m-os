package com.mos.victory.dto;

import com.mos.victory.enums.VictoryConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateVictoryConditionRequest(
        @NotNull VictoryConditionType type,
        Map<String, Object> targetValue,
        @NotBlank String description,
        Boolean active
) {
}
