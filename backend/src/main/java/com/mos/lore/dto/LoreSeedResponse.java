package com.mos.lore.dto;

import java.util.List;

public record LoreSeedResponse(
        boolean loreRevealed,
        int templatesCreated,
        int templatesExisting,
        int secretsCreated,
        int secretsExisting,
        int locationsUpdated,
        List<String> codes
) {
}
