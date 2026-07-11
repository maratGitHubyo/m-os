package com.mos.trade.dto;

import com.mos.trade.entity.TradeCoin;

import java.util.UUID;

public record TradeCoinResponse(
        UUID id,
        UUID userId,
        long amount
) {

    public static TradeCoinResponse from(TradeCoin tradeCoin) {
        return new TradeCoinResponse(
                tradeCoin.getId(),
                tradeCoin.getUserId(),
                tradeCoin.getAmount().longValue()
        );
    }
}
