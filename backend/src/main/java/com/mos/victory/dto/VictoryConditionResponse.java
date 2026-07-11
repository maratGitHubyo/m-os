package com.mos.victory.dto;

import com.mos.victory.entity.VictoryCondition;
import com.mos.victory.enums.VictoryConditionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record VictoryConditionResponse(
        UUID id,
        UUID gameSessionId,
        VictoryConditionType type,
        Map<String, Object> targetValue,
        String description,
        boolean active,
        boolean achieved,
        Instant achievedAt,
        UUID achievedByUserId,
        boolean achievedByMe,
        Long progressCurrent,
        Long progressTarget,
        String progressLabel
) {

    public static VictoryConditionResponse forPlayer(
            VictoryCondition condition,
            UUID currentUserId,
            VictoryProgress progress
    ) {
        boolean achieved = condition.getAchievedAt() != null;
        return new VictoryConditionResponse(
                condition.getId(),
                condition.getGameSessionId(),
                condition.getType(),
                condition.getTargetValue(),
                condition.getDescription(),
                Boolean.TRUE.equals(condition.getActive()),
                achieved,
                condition.getAchievedAt(),
                condition.getAchievedByUserId(),
                achieved && currentUserId.equals(condition.getAchievedByUserId()),
                progress.current(),
                progress.target(),
                progress.label()
        );
    }

    public static VictoryConditionResponse forAdmin(VictoryCondition condition) {
        boolean achieved = condition.getAchievedAt() != null;
        return new VictoryConditionResponse(
                condition.getId(),
                condition.getGameSessionId(),
                condition.getType(),
                condition.getTargetValue(),
                condition.getDescription(),
                Boolean.TRUE.equals(condition.getActive()),
                achieved,
                condition.getAchievedAt(),
                condition.getAchievedByUserId(),
                false,
                null,
                null,
                null
        );
    }
}
