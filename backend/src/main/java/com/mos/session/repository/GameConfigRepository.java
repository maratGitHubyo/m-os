package com.mos.session.repository;

import com.mos.session.entity.GameConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameConfigRepository extends JpaRepository<GameConfig, UUID> {

    Optional<GameConfig> findByGameSessionId(UUID gameSessionId);
}
