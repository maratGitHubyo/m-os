package com.mos.event.dto;

import com.mos.event.enums.GameEventStatus;

import java.time.Instant;
import java.util.UUID;

public record GameEventBroadcastMessage(
        UUID eventId,
        UUID gameSessionId,
        GameEventStatus status,
        String title,
        Instant changedAt
) {
}
