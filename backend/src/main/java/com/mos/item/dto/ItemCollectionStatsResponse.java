package com.mos.item.dto;

import java.util.List;

public record ItemCollectionStatsResponse(
        List<ItemCollectionStatsEntry> players,
        ItemCollectionTotals collected,
        ItemCollectionTotals catalog
) {
}
