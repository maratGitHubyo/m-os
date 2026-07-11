package com.mos.score.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String nickname,
        Long points
) {
}
