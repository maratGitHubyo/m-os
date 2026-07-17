package com.mos.lore.dto;

import java.util.UUID;

public record LoreFragmentStatsEntry(
        UUID templateId,
        String name,
        String code,
        boolean found,
        UUID ownerUserId,
        String ownerNickname
) {
}
