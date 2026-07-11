package com.mos.victory.checker;

import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;

import java.util.Map;
import java.util.UUID;

public interface VictoryTypeChecker {

    VictoryConditionType supportedType();

    VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue);
}
