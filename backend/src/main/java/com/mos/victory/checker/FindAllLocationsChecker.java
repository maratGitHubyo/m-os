package com.mos.victory.checker;

import com.mos.location.repository.LocationPointRepository;
import com.mos.location.repository.PlayerLocationDiscoveryRepository;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindAllLocationsChecker implements VictoryTypeChecker {

    private final LocationPointRepository locationPointRepository;
    private final PlayerLocationDiscoveryRepository playerLocationDiscoveryRepository;

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.FIND_ALL_LOCATIONS;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        long target = locationPointRepository.countByGameSessionId(gameSessionId);
        long current = playerLocationDiscoveryRepository.countByUserIdAndGameSessionId(userId, gameSessionId);
        return VictoryProgress.of(current, target, "Locations discovered");
    }
}
