package com.mos.session.dto;

import java.util.UUID;

public record SessionPlayerResponse(
        UUID id,
        String nickname
) {
}
