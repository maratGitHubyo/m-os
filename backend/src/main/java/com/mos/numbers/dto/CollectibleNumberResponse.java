package com.mos.numbers.dto;

import com.mos.numbers.entity.CollectibleNumber;

import java.time.Instant;
import java.util.UUID;

public record CollectibleNumberResponse(
        UUID id,
        UUID gameSessionId,
        Integer numberValue,
        Instant createdAt
) {

    public static CollectibleNumberResponse from(CollectibleNumber number) {
        return new CollectibleNumberResponse(
                number.getId(),
                number.getGameSessionId(),
                number.getNumberValue(),
                number.getCreatedAt()
        );
    }
}
