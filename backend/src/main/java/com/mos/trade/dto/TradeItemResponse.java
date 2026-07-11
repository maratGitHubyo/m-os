package com.mos.trade.dto;

import com.mos.item.dto.ItemTemplateResponse;
import com.mos.trade.entity.TradeItem;

import java.util.UUID;

public record TradeItemResponse(
        UUID id,
        UUID playerItemId,
        UUID ownerId,
        ItemTemplateResponse template
) {

    public static TradeItemResponse from(TradeItem tradeItem) {
        return new TradeItemResponse(
                tradeItem.getId(),
                tradeItem.getPlayerItem().getId(),
                tradeItem.getOwnerId(),
                ItemTemplateResponse.from(tradeItem.getPlayerItem().getItemTemplate())
        );
    }
}
