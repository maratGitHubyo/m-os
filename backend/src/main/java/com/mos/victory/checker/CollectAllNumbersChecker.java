package com.mos.victory.checker;

import com.mos.session.repository.GameConfigRepository;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Placeholder for Stage 11 (Numbers). Never reports satisfied until numbers module exists.
 */
@Component
@RequiredArgsConstructor
public class CollectAllNumbersChecker implements VictoryTypeChecker {

    private final GameConfigRepository gameConfigRepository;

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.COLLECT_ALL_NUMBERS;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        int numbersTotal = gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> config.getNumbersTotal() != null ? config.getNumbersTotal() : 0)
                .orElse(0);

        return VictoryProgress.stub(0, numbersTotal, "Numbers collected (not yet available)");
    }
}
