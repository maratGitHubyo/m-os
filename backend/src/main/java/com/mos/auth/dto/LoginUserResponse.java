package com.mos.auth.dto;

import com.mos.session.entity.GameSession;
import com.mos.session.entity.ParticipantRole;
import com.mos.user.entity.User;

import java.util.UUID;

public record LoginUserResponse(
        UUID id,
        String nickname,
        ParticipantRole role
) {

    public static LoginUserResponse from(User user, ParticipantRole role) {
        return new LoginUserResponse(user.getId(), user.getNickname(), role);
    }
}
