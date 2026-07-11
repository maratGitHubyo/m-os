package com.mos.item.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminGrantItemRequest(
        @NotNull UUID userId,
        @NotNull UUID itemTemplateId
) {
}
