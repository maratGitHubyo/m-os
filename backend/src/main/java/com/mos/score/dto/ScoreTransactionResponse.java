package com.mos.score.dto;

import com.mos.score.entity.ScoreTransaction;
import com.mos.score.enums.ScoreCategory;

import java.time.Instant;
import java.util.UUID;

public record ScoreTransactionResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        ScoreCategory category,
        Long delta,
        String reason,
        Instant createdAt
) {

    public static ScoreTransactionResponse from(ScoreTransaction transaction) {
        return new ScoreTransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getGameSessionId(),
                transaction.getCategory(),
                transaction.getDelta(),
                transaction.getReason(),
                transaction.getCreatedAt()
        );
    }
}
