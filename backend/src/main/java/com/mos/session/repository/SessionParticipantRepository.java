package com.mos.session.repository;

import com.mos.session.entity.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, UUID> {

    Optional<SessionParticipant> findByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);

    List<SessionParticipant> findByGameSessionId(UUID gameSessionId);

    List<SessionParticipant> findByUserId(UUID userId);

    long countByGameSessionId(UUID gameSessionId);
}
