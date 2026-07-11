package com.mos.session.dto;

import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GameSessionResponse(
        UUID id,
        String name,
        LocalDate date,
        GameSessionStatus status,
        String mapImageUrl,
        Instant createdAt,
        Instant updatedAt,
        GameConfigResponse config
) {

    public static GameSessionResponse from(GameSession session) {
        GameConfigResponse configResponse = session.getConfig() != null
                ? GameConfigResponse.from(session.getConfig())
                : null;

        return new GameSessionResponse(
                session.getId(),
                session.getName(),
                session.getDate(),
                session.getStatus(),
                session.getMapImageUrl(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                configResponse
        );
    }
}
