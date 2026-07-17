package com.mos.wallet.dto;

public record AdminBulkCreditResponse(
        int playerCount,
        long amountPerPlayer,
        long totalCredited
) {
}
