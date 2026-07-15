package com.mos.auction.dto;

import com.mos.auction.entity.AuctionLot;
import com.mos.auction.enums.AuctionLotStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuctionLotResponse(
        UUID id,
        String title,
        Long startingPrice,
        Long minBidIncrement,
        AuctionLotStatus status,
        Long currentPrice,
        UUID currentLeaderId,
        String currentLeaderNickname,
        UUID winnerUserId,
        String winnerNickname,
        Long finalPrice,
        Long nextMinBid,
        Instant createdAt,
        Instant openedAt,
        Instant closedAt,
        List<AuctionBidResponse> recentBids
) {

    public static AuctionLotResponse from(
            AuctionLot lot,
            String currentLeaderNickname,
            String winnerNickname,
            List<AuctionBidResponse> recentBids
    ) {
        return new AuctionLotResponse(
                lot.getId(),
                lot.getTitle(),
                lot.getStartingPrice(),
                lot.getMinBidIncrement(),
                lot.getStatus(),
                lot.getCurrentPrice(),
                lot.getCurrentLeaderId(),
                currentLeaderNickname,
                lot.getWinnerUserId(),
                winnerNickname,
                lot.getFinalPrice(),
                computeNextMinBid(lot),
                lot.getCreatedAt(),
                lot.getOpenedAt(),
                lot.getClosedAt(),
                recentBids
        );
    }

    private static Long computeNextMinBid(AuctionLot lot) {
        if (lot.getStatus() != AuctionLotStatus.OPEN) {
            return null;
        }
        if (lot.getCurrentPrice() == null) {
            return Math.max(lot.getStartingPrice(), 1L);
        }
        return lot.getCurrentPrice() + lot.getMinBidIncrement();
    }
}
