package com.mos.victory.checker;

import com.mos.numbers.repository.PlayerNumberRepository;
import com.mos.session.repository.GameConfigRepository;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollectAllNumbersChecker implements VictoryTypeChecker {

    private final GameConfigRepository gameConfigRepository;
    private final PlayerNumberRepository playerNumberRepository;

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.COLLECT_ALL_NUMBERS;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        long numbersTotal = gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> config.getNumbersTotal() != null ? config.getNumbersTotal().longValue() : 0L)
                .orElse(0L);

        long current = playerNumberRepository.countByUserIdAndGameSessionId(userId, gameSessionId);

        return VictoryProgress.of(current, numbersTotal, "Numbers collected");
    }
}
