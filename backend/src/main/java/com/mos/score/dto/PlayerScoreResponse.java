package com.mos.score.dto;

import com.mos.score.entity.PlayerScore;
import com.mos.score.enums.ScoreCategory;

import java.util.UUID;

public record PlayerScoreResponse(
        UUID userId,
        UUID gameSessionId,
        ScoreCategory category,
        Long points
) {

    public static PlayerScoreResponse from(PlayerScore score) {
        return new PlayerScoreResponse(
                score.getUserId(),
                score.getGameSessionId(),
                score.getCategory(),
                score.getPoints()
        );
    }

    public static PlayerScoreResponse empty(UUID userId, UUID gameSessionId, ScoreCategory category) {
        return new PlayerScoreResponse(userId, gameSessionId, category, 0L);
    }
}
