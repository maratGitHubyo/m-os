package com.mos.wallet.transfer.dto;

import com.mos.wallet.transfer.entity.CoinTransfer;

import java.time.Instant;
import java.util.UUID;

public record CoinTransferResponse(
        UUID id,
        UUID senderUserId,
        String senderNickname,
        UUID receiverUserId,
        String receiverNickname,
        Long amount,
        Instant createdAt
) {

    public static CoinTransferResponse from(
            CoinTransfer transfer,
            String senderNickname,
            String receiverNickname
    ) {
        return new CoinTransferResponse(
                transfer.getId(),
                transfer.getSenderUserId(),
                senderNickname,
                transfer.getReceiverUserId(),
                receiverNickname,
                transfer.getAmount(),
                transfer.getCreatedAt()
        );
    }
}
