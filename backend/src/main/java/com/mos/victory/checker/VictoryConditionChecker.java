package com.mos.victory.checker;

import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.entity.VictoryCondition;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VictoryConditionChecker {

    private final List<VictoryTypeChecker> typeCheckers;

    private Map<VictoryConditionType, VictoryTypeChecker> checkerByType;

    public VictoryProgress evaluateCondition(UUID userId, UUID gameSessionId, VictoryCondition condition) {
        if (condition.getAchievedAt() != null) {
            return VictoryProgress.of(1, 1, "Achieved");
        }
        return resolveChecker(condition.getType())
                .evaluate(userId, gameSessionId, condition.getTargetValue());
    }

    public boolean isSatisfied(UUID userId, UUID gameSessionId, VictoryCondition condition) {
        return evaluateCondition(userId, gameSessionId, condition).satisfied();
    }

    private VictoryTypeChecker resolveChecker(VictoryConditionType type) {
        if (checkerByType == null) {
            checkerByType = new EnumMap<>(VictoryConditionType.class);
            for (VictoryTypeChecker checker : typeCheckers) {
                checkerByType.put(checker.supportedType(), checker);
            }
        }
        VictoryTypeChecker checker = checkerByType.get(type);
        if (checker == null) {
            throw new IllegalStateException("No victory checker registered for type: " + type);
        }
        return checker;
    }
}
