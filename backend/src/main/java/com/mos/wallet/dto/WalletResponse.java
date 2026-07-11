package com.mos.wallet.dto;

import com.mos.wallet.entity.Wallet;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        Long balance,
        Long version,
        Instant updatedAt
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getGameSessionId(),
                wallet.getBalance(),
                wallet.getVersion(),
                wallet.getUpdatedAt()
        );
    }
}
