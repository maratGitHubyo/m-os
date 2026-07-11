package com.mos.victory.dto;

import com.mos.victory.enums.VictoryConditionType;

import java.time.Instant;
import java.util.UUID;

public record VictoryBroadcastMessage(
        UUID gameSessionId,
        UUID conditionId,
        VictoryConditionType type,
        String description,
        UUID achievedByUserId,
        Instant achievedAt,
        boolean sessionFinished
) {
}
