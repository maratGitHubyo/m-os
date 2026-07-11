package com.mos.item.dto;

import com.mos.item.enums.ItemRarity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateItemTemplateRequest(
        @NotBlank String name,
        @NotBlank String description,
        String imageUrl,
        @NotNull ItemRarity rarity,
        @NotNull Boolean isUnique
) {
}
