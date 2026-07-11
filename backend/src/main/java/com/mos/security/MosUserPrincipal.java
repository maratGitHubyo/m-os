package com.mos.security;

import com.mos.session.entity.ParticipantRole;

import java.util.UUID;

public record MosUserPrincipal(
        UUID userId,
        UUID gameSessionId,
        ParticipantRole role
) {
}
