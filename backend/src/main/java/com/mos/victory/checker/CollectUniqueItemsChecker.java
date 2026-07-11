package com.mos.victory.checker;

import com.mos.common.exception.BusinessException;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.victory.dto.VictoryProgress;
import com.mos.victory.enums.VictoryConditionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollectUniqueItemsChecker implements VictoryTypeChecker {

    private final PlayerItemRepository playerItemRepository;

    @Override
    public VictoryConditionType supportedType() {
        return VictoryConditionType.COLLECT_UNIQUE_ITEMS;
    }

    @Override
    public VictoryProgress evaluate(UUID userId, UUID gameSessionId, Map<String, Object> targetValue) {
        long target = parseCount(targetValue);
        long current = playerItemRepository.countDistinctUniqueItemTemplatesByOwner(userId, gameSessionId);
        return VictoryProgress.of(current, target, "Unique items");
    }

    private long parseCount(Map<String, Object> targetValue) {
        Object raw = targetValue.get("count");
        if (raw == null) {
            throw new BusinessException("COLLECT_UNIQUE_ITEMS victory condition requires count in targetValue");
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid count in victory targetValue: " + raw);
        }
    }
}
