package com.mos.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminWalletOperationRequest(
        @NotNull @Min(1) Long amount,
        @NotBlank String description
) {
}
