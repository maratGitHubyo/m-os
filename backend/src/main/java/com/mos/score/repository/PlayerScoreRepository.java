package com.mos.score.repository;

import com.mos.score.entity.PlayerScore;
import com.mos.score.enums.ScoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerScoreRepository extends JpaRepository<PlayerScore, UUID> {

    Optional<PlayerScore> findByUserIdAndGameSessionIdAndCategory(
            UUID userId,
            UUID gameSessionId,
            ScoreCategory category
    );

    List<PlayerScore> findByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);

    List<PlayerScore> findByGameSessionIdAndCategory(UUID gameSessionId, ScoreCategory category);
}
