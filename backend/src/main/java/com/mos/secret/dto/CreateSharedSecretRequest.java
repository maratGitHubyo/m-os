package com.mos.secret.dto;

import com.mos.secret.enums.SecretRewardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateSharedSecretRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull SecretRewardType rewardType,
        Map<String, Object> rewardPayload
) {
}
