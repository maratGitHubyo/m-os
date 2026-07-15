package com.mos.auction.dto;

import jakarta.validation.constraints.NotNull;

public record SetAuctionModeRequest(
        @NotNull Boolean enabled
) {
}
