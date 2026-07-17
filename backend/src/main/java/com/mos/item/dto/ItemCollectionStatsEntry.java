package com.mos.item.dto;

import java.util.UUID;

public record ItemCollectionStatsEntry(
        int rank,
        UUID userId,
        String nickname,
        long common,
        long rare,
        long epic,
        long legendary,
        long total
) {
}
