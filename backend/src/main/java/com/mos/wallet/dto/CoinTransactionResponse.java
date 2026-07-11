package com.mos.wallet.dto;

import com.mos.wallet.entity.CoinTransaction;
import com.mos.wallet.enums.CoinTransactionType;

import java.time.Instant;
import java.util.UUID;

public record CoinTransactionResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        Long amount,
        CoinTransactionType type,
        String referenceId,
        String description,
        Instant createdAt
) {

    public static CoinTransactionResponse from(CoinTransaction transaction) {
        return new CoinTransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getGameSessionId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
