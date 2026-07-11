package com.mos.victory.dto;

import java.util.List;
import java.util.UUID;

public record VictoryConditionAdminStatusResponse(
        UUID gameSessionId,
        boolean anyAchieved,
        boolean sessionFinished,
        List<VictoryConditionResponse> conditions
) {
}
