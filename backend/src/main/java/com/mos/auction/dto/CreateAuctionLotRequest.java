package com.mos.auction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuctionLotRequest(
        @NotBlank
        @Size(max = 255)
        String title,
        @Min(0) Long startingPrice,
        @Min(1) Long minBidIncrement
) {
}
