package com.mos.numbers.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCollectibleNumberRequest(
        @NotNull Integer numberValue
) {
}
