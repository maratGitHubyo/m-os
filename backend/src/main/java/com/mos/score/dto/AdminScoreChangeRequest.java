package com.mos.score.dto;

import com.mos.score.enums.ScoreCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminScoreChangeRequest(
        @NotNull ScoreCategory category,
        @NotNull @Positive Long points,
        @NotBlank String reason
) {
}
