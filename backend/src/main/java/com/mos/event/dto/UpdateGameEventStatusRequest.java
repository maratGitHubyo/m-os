package com.mos.event.dto;

import com.mos.event.enums.GameEventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGameEventStatusRequest(
        @NotNull GameEventStatus status
) {
}
