package com.mos.trade.dto;

import com.mos.trade.entity.Trade;
import com.mos.trade.entity.TradeCoin;
import com.mos.trade.entity.TradeItem;
import com.mos.trade.enums.TradeStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TradeResponse(
        UUID id,
        UUID gameSessionId,
        UUID initiatorId,
        UUID receiverId,
        TradeStatus status,
        List<TradeItemResponse> items,
        List<TradeCoinResponse> coins,
        Instant createdAt,
        Instant updatedAt
) {

    public static TradeResponse from(Trade trade, List<TradeItem> items, List<TradeCoin> coins) {
        return new TradeResponse(
                trade.getId(),
                trade.getGameSessionId(),
                trade.getInitiatorId(),
                trade.getReceiverId(),
                trade.getStatus(),
                items.stream().map(TradeItemResponse::from).toList(),
                coins.stream().map(TradeCoinResponse::from).toList(),
                trade.getCreatedAt(),
                trade.getUpdatedAt()
        );
    }
}
