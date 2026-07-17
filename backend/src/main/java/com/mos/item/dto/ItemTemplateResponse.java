package com.mos.item.dto;

import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;

import java.time.Instant;
import java.util.UUID;

public record ItemTemplateResponse(
        UUID id,
        UUID gameSessionId,
        String name,
        String description,
        String imageUrl,
        ItemRarity rarity,
        Boolean isUnique,
        Boolean isLore,
        Instant createdAt
) {

    public static ItemTemplateResponse from(ItemTemplate template) {
        return from(template, true);
    }

    /**
     * @param revealLoreDescription when false, lore item descriptions are redacted for players
     */
    public static ItemTemplateResponse from(ItemTemplate template, boolean revealLoreDescription) {
        boolean lore = Boolean.TRUE.equals(template.getIsLore());
        String description = lore && !revealLoreDescription ? null : template.getDescription();
        return new ItemTemplateResponse(
                template.getId(),
                template.getGameSessionId(),
                template.getName(),
                description,
                template.getImageUrl(),
                template.getRarity(),
                template.getIsUnique(),
                lore,
                template.getCreatedAt()
        );
    }
}
