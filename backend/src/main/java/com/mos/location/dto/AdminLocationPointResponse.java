package com.mos.location.dto;

import com.mos.location.entity.LocationPoint;

import java.util.UUID;

public record AdminLocationPointResponse(
        UUID id,
        UUID gameSessionId,
        String name,
        String description,
        Double x,
        Double y,
        String zone,
        Boolean hidden
) {

    public static AdminLocationPointResponse from(LocationPoint location) {
        return new AdminLocationPointResponse(
                location.getId(),
                location.getGameSessionId(),
                location.getName(),
                location.getDescription(),
                location.getX(),
                location.getY(),
                location.getZone(),
                location.getHidden()
        );
    }
}
