package com.mos.secret.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemSecretRequest(
        @NotBlank String code
) {
}
