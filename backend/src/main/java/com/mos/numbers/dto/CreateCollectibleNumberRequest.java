package com.mos.numbers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCollectibleNumberRequest(
        @NotNull @Positive Integer numberValue
) {
}
