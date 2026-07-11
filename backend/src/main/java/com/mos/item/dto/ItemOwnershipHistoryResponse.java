package com.mos.item.dto;

import com.mos.item.entity.ItemOwnershipHistory;
import com.mos.item.enums.OwnershipTransferReason;

import java.time.Instant;
import java.util.UUID;

public record ItemOwnershipHistoryResponse(
        UUID id,
        UUID playerItemId,
        UUID gameSessionId,
        UUID fromUserId,
        UUID toUserId,
        OwnershipTransferReason reason,
        Instant createdAt
) {

    public static ItemOwnershipHistoryResponse from(ItemOwnershipHistory history) {
        return new ItemOwnershipHistoryResponse(
                history.getId(),
                history.getPlayerItem().getId(),
                history.getGameSessionId(),
                history.getFromUserId(),
                history.getToUserId(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}
