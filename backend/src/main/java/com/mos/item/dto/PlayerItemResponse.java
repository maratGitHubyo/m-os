package com.mos.item.dto;

import com.mos.item.entity.PlayerItem;
import com.mos.item.enums.ItemAcquisitionSource;

import java.time.Instant;
import java.util.UUID;

public record PlayerItemResponse(
        UUID id,
        UUID ownerId,
        UUID gameSessionId,
        ItemAcquisitionSource acquiredFrom,
        Instant acquiredAt,
        ItemTemplateResponse template
) {

    public static PlayerItemResponse from(PlayerItem item) {
        return new PlayerItemResponse(
                item.getId(),
                item.getOwnerId(),
                item.getGameSessionId(),
                item.getAcquiredFrom(),
                item.getAcquiredAt(),
                ItemTemplateResponse.from(item.getItemTemplate())
        );
    }
}
