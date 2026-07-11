package com.mos.auth.dto;

import com.mos.session.entity.GameSession;

import java.util.UUID;

public record LoginSessionResponse(
        UUID id,
        String name
) {

    public static LoginSessionResponse from(GameSession session) {
        return new LoginSessionResponse(session.getId(), session.getName());
    }
}
