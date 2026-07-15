package com.mos.auction.dto;

import java.util.List;

public record AuctionStateResponse(
        boolean auctionModeEnabled,
        AuctionLotResponse openLot,
        List<AuctionLotResponse> lots
) {
}
