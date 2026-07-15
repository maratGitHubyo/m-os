package com.mos.wallet.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        int rank,
        UUID userId,
        String nickname,
        Long balance
) {
}
