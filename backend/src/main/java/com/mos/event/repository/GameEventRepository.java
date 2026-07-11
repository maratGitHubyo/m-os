package com.mos.event.repository;

import com.mos.event.entity.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameEventRepository extends JpaRepository<GameEvent, UUID> {

    List<GameEvent> findByGameSessionIdOrderByStartAtAscCreatedAtAsc(UUID gameSessionId);

    Optional<GameEvent> findByIdAndGameSessionId(UUID id, UUID gameSessionId);
}
