package com.mos.session.service;

import com.mos.common.exception.SessionConfigurationException;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionResolver {

    private static final List<GameSessionStatus> CURRENT_SESSION_STATUSES = List.of(
            GameSessionStatus.ACTIVE,
            GameSessionStatus.STARTING
    );

    private final GameSessionRepository gameSessionRepository;

    public GameSession resolveCurrentSession() {
        List<GameSession> sessions = gameSessionRepository.findByStatusIn(CURRENT_SESSION_STATUSES);

        if (sessions.isEmpty()) {
            throw new SessionConfigurationException("No ACTIVE or STARTING game session configured");
        }

        if (sessions.size() > 1) {
            throw new SessionConfigurationException("Multiple ACTIVE or STARTING game sessions configured");
        }

        return sessions.getFirst();
    }
}
