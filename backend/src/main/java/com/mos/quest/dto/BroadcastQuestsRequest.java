package com.mos.quest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BroadcastQuestsRequest(
        @NotNull @Min(1) @Max(20) Integer count
) {
}
