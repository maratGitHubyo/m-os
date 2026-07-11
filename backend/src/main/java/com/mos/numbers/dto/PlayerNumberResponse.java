package com.mos.numbers.dto;

import com.mos.numbers.entity.PlayerNumber;

import java.time.Instant;
import java.util.UUID;

public record PlayerNumberResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        Integer numberValue,
        Instant acquiredAt
) {

    public static PlayerNumberResponse from(PlayerNumber playerNumber) {
        return new PlayerNumberResponse(
                playerNumber.getId(),
                playerNumber.getUserId(),
                playerNumber.getGameSessionId(),
                playerNumber.getNumber().getNumberValue(),
                playerNumber.getAcquiredAt()
        );
    }
}
