package com.mos.quest.dto;

import java.time.Instant;

public record QuestAutoDistributeStatusResponse(
        boolean enabled,
        Instant lastDistributedAt,
        Instant nextDistributionAt,
        int autoCount,
        int intervalMinutes
) {
}
