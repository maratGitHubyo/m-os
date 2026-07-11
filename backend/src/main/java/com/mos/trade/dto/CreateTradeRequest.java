package com.mos.trade.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateTradeRequest(
        @NotNull UUID receiverId,
        List<UUID> initiatorItemIds,
        List<UUID> receiverItemIds,
        Long initiatorCoins,
        Long receiverCoins
) {
}
