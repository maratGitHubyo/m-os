package com.mos.secret.dto;

import com.mos.secret.entity.PlayerSecret;
import com.mos.secret.enums.SecretRewardType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlayerSecretResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        String code,
        String title,
        String description,
        SecretRewardType rewardType,
        Map<String, Object> rewardPayload,
        Boolean used,
        Instant usedAt,
        Boolean isShared
) {

    public static PlayerSecretResponse from(PlayerSecret secret) {
        return new PlayerSecretResponse(
                secret.getId(),
                secret.getUserId(),
                secret.getGameSessionId(),
                secret.getCode(),
                secret.getTitle(),
                secret.getDescription(),
                secret.getRewardType(),
                secret.getRewardPayload(),
                secret.getUsed(),
                secret.getUsedAt(),
                Boolean.TRUE.equals(secret.getIsShared())
        );
    }
}
