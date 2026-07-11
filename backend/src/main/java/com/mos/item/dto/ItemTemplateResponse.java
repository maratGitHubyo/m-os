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
        Instant createdAt
) {

    public static ItemTemplateResponse from(ItemTemplate template) {
        return new ItemTemplateResponse(
                template.getId(),
                template.getGameSessionId(),
                template.getName(),
                template.getDescription(),
                template.getImageUrl(),
                template.getRarity(),
                template.getIsUnique(),
                template.getCreatedAt()
        );
    }
}
