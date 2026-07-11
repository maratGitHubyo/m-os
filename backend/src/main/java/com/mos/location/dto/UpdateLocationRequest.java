package com.mos.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdateLocationRequest(
        String name,
        String description,
        @DecimalMin("0") @DecimalMax("100") Double x,
        @DecimalMin("0") @DecimalMax("100") Double y,
        String zone,
        Boolean hidden
) {
}
