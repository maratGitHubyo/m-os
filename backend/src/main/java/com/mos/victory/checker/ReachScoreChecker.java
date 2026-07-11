package com.mos.victory.checker;

import com.mos.common.exception.BusinessException;
import com.mos.score.enums.ScoreCategory;
import com.mos.score.repository.PlayerScoreRepository;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReachScoreChecker implements VictoryTypeChecker {

    private final PlayerScoreRepository playerScoreRepository;

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.REACH_SCORE;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        ScoreCategory category = parseCategory(targetValue);
        long threshold = parseThreshold(targetValue);

        long current = playerScoreRepository
                .findByUserIdAndGameSessionIdAndCategory(userId, gameSessionId, category)
                .map(score -> score.getPoints())
                .orElse(0L);

        return VictoryProgress.of(current, threshold, category.name() + " score");
    }

    private ScoreCategory parseCategory(Map<String, Object> targetValue) {
        Object raw = targetValue.get("category");
        if (raw == null) {
            return ScoreCategory.TOTAL;
        }
        try {
            return ScoreCategory.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid score category in victory targetValue: " + raw);
        }
    }

    private long parseThreshold(Map<String, Object> targetValue) {
        Object raw = targetValue.get("threshold");
        if (raw == null) {
            throw new BusinessException("REACH_SCORE victory condition requires threshold in targetValue");
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid threshold in victory targetValue: " + raw);
        }
    }
}
