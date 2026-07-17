package com.mos.lore.dto;

import jakarta.validation.constraints.NotNull;

public record SetLoreRevealRequest(
        @NotNull Boolean revealed
) {
}
