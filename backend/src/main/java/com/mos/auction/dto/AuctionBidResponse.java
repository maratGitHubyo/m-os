package com.mos.auction.dto;

import com.mos.auction.entity.AuctionBid;

import java.time.Instant;
import java.util.UUID;

public record AuctionBidResponse(
        UUID id,
        UUID lotId,
        UUID bidderUserId,
        String bidderNickname,
        Long amount,
        Instant createdAt
) {

    public static AuctionBidResponse from(AuctionBid bid, String bidderNickname) {
        return new AuctionBidResponse(
                bid.getId(),
                bid.getLotId(),
                bid.getBidderUserId(),
                bidderNickname,
                bid.getAmount(),
                bid.getCreatedAt()
        );
    }
}
