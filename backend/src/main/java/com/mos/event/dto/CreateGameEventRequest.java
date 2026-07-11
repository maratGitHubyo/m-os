package com.mos.event.dto;

import com.mos.event.enums.GameEventStatus;
import com.mos.event.enums.GameEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record CreateGameEventRequest(
        @NotNull GameEventType type,
        @NotBlank String title,
        @NotBlank String description,
        Instant startAt,
        Instant endAt,
        Map<String, Object> config
) {
}
