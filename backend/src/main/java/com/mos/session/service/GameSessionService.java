package com.mos.session.service;

import com.mos.common.exception.BusinessException;
import com.mos.security.SecurityUtils;
import com.mos.session.dto.GameSessionResponse;
import com.mos.session.entity.GameSession;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;

    public GameSessionResponse getCurrentSession() {
        UUID sessionId = SecurityUtils.getCurrentUser().gameSessionId();

        GameSession session = gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

        if (session.getConfig() == null) {
            gameConfigRepository.findByGameSessionId(sessionId).ifPresent(session::setConfig);
        }

        return GameSessionResponse.from(session);
    }

    public Optional<GameSession> findById(UUID id) {
        return gameSessionRepository.findById(id);
    }
}
