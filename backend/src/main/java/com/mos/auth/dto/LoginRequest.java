package com.mos.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Username", example = "admin")
        @NotBlank @Size(max = 64) String username,
        @Schema(description = "Password", example = "admin123")
        @NotBlank @Size(max = 128) String password
) {
}
