package com.mos.auth.dto;

public record LoginResponse(
        String token,
        LoginUserResponse user,
        LoginSessionResponse session
) {
}
