package com.mos.common.exception;

public class LeaderboardDisabledException extends BusinessException {

    public LeaderboardDisabledException() {
        super("Leaderboard is disabled for this game session");
    }
}
