package com.mos.session.repository;

import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    List<GameSession> findByStatusIn(List<GameSessionStatus> statuses);
}
