package com.mos.auction.dto;

import com.mos.auction.enums.AuctionLotStatus;

import java.time.Instant;
import java.util.UUID;

public record AuctionBroadcastMessage(
        String type,
        UUID gameSessionId,
        UUID lotId,
        String lotTitle,
        AuctionLotStatus status,
        Long currentPrice,
        UUID currentLeaderId,
        String currentLeaderNickname,
        Long nextMinBid,
        Long bidAmount,
        UUID bidderUserId,
        String bidderNickname,
        boolean auctionModeEnabled,
        Instant changedAt
) {
}
