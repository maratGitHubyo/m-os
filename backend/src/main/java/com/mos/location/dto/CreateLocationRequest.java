package com.mos.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double x,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double y,
        @NotBlank String zone,
        @NotNull Boolean hidden
) {
}
