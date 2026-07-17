package com.mos.lore.dto;

import java.util.List;

public record LoreStatsResponse(
        boolean loreRevealed,
        int totalFragments,
        int foundCount,
        int unfoundCount,
        int playersWithLore,
        int playersWithoutLore,
        List<LoreFragmentStatsEntry> fragments
) {
}
