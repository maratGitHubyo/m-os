package com.mos.item.dto;

import com.mos.item.entity.PlayerItem;

import java.time.Instant;
import java.util.UUID;

public record PlayerItemResponse(
        UUID id,
        UUID ownerId,
        UUID gameSessionId,
        String acquiredFrom,
        Instant acquiredAt,
        ItemTemplateResponse template
) {

    public static PlayerItemResponse from(PlayerItem item) {
        return from(item, true);
    }

    public static PlayerItemResponse from(PlayerItem item, boolean revealLoreDescription) {
        return new PlayerItemResponse(
                item.getId(),
                item.getOwnerId(),
                item.getGameSessionId(),
                item.getAcquiredFrom().name(),
                item.getAcquiredAt(),
                ItemTemplateResponse.from(item.getItemTemplate(), revealLoreDescription)
        );
    }
}
