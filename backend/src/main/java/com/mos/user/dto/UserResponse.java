package com.mos.user.dto;

import com.mos.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String nickname,
        String displayName,
        String avatarUrl,
        boolean active,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
