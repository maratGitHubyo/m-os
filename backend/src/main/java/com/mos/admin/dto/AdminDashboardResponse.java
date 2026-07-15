package com.mos.admin.dto;

import com.mos.session.dto.GameSessionResponse;
import com.mos.wallet.dto.LeaderboardEntryResponse;

import java.util.List;

public record AdminDashboardResponse(
        GameSessionResponse currentSession,
        long playersCount,
        long activeQuests,
        long locationsCount,
        long itemsCount,
        long qrCount,
        List<LeaderboardEntryResponse> leaderboard
) {
}
