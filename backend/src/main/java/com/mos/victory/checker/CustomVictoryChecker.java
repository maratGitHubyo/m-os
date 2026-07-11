package com.mos.victory.checker;

import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class CustomVictoryChecker implements VictoryTypeChecker {

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.CUSTOM;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        return VictoryProgress.notApplicable("Custom condition (manual)");
    }
}
