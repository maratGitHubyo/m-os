package com.mos.location.dto;

import com.mos.location.entity.LocationPoint;

import java.util.UUID;

public record LocationPointResponse(
        UUID id,
        String zone,
        Double x,
        Double y,
        Boolean hidden,
        Boolean discovered,
        String name,
        String description
) {

    public static LocationPointResponse forPlayer(LocationPoint location, boolean discovered) {
        boolean revealed = discovered || !Boolean.TRUE.equals(location.getHidden());

        return new LocationPointResponse(
                location.getId(),
                location.getZone(),
                location.getX(),
                location.getY(),
                location.getHidden(),
                revealed,
                revealed ? location.getName() : null,
                revealed ? location.getDescription() : null
        );
    }
}
