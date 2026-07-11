package com.mos.event.dto;

import com.mos.event.entity.GameEvent;
import com.mos.event.enums.GameEventStatus;
import com.mos.event.enums.GameEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GameEventResponse(
        UUID id,
        UUID gameSessionId,
        GameEventType type,
        String title,
        String description,
        GameEventStatus status,
        Instant startAt,
        Instant endAt,
        Map<String, Object> config,
        UUID createdBy,
        Instant createdAt
) {

    public static GameEventResponse from(GameEvent event) {
        return new GameEventResponse(
                event.getId(),
                event.getGameSessionId(),
                event.getType(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus(),
                event.getStartAt(),
                event.getEndAt(),
                event.getConfig(),
                event.getCreatedBy(),
                event.getCreatedAt()
        );
    }
}
