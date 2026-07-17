package com.mos.session.repository;

import com.mos.session.entity.GameConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameConfigRepository extends JpaRepository<GameConfig, UUID> {

    Optional<GameConfig> findByGameSessionId(UUID gameSessionId);

    @Query("SELECT c FROM GameConfig c JOIN FETCH c.gameSession WHERE c.questAutoDistributeEnabled = TRUE")
    List<GameConfig> findByQuestAutoDistributeEnabledTrue();
}
