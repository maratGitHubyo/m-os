package com.mos.admin.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.session.dto.GameSessionResponse;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AuditService auditService;

    @Transactional
    public GameSessionResponse startSession(UUID gameSessionId, UUID performedByUserId) {
        GameSession session = getSession(gameSessionId);
        GameSessionStatus previousStatus = session.getStatus();

        if (previousStatus != GameSessionStatus.STARTING && previousStatus != GameSessionStatus.PAUSED) {
            throw new BusinessException("Session can only be started from STARTING or PAUSED status");
        }

        session.setStatus(GameSessionStatus.ACTIVE);
        gameSessionRepository.save(session);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.SESSION_START,
                "GameSession",
                session.getId().toString(),
                "Game session started",
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", GameSessionStatus.ACTIVE.name()
                )
        );

        return toResponse(session);
    }

    @Transactional
    public GameSessionResponse pauseSession(UUID gameSessionId, UUID performedByUserId) {
        GameSession session = getSession(gameSessionId);
        GameSessionStatus previousStatus = session.getStatus();

        if (previousStatus != GameSessionStatus.ACTIVE) {
            throw new BusinessException("Session can only be paused from ACTIVE status");
        }

        session.setStatus(GameSessionStatus.PAUSED);
        gameSessionRepository.save(session);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.SESSION_PAUSE,
                "GameSession",
                session.getId().toString(),
                "Game session paused",
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", GameSessionStatus.PAUSED.name()
                )
        );

        return toResponse(session);
    }

    @Transactional
    public GameSessionResponse finishSession(UUID gameSessionId, UUID performedByUserId) {
        GameSession session = getSession(gameSessionId);
        GameSessionStatus previousStatus = session.getStatus();

        if (previousStatus != GameSessionStatus.ACTIVE && previousStatus != GameSessionStatus.PAUSED) {
            throw new BusinessException("Session can only be finished from ACTIVE or PAUSED status");
        }

        session.setStatus(GameSessionStatus.FINISHED);
        gameSessionRepository.save(session);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.SESSION_FINISH,
                "GameSession",
                session.getId().toString(),
                "Game session finished",
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "newStatus", GameSessionStatus.FINISHED.name()
                )
        );

        return toResponse(session);
    }

    private GameSession getSession(UUID gameSessionId) {
        return gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));
    }

    private GameSessionResponse toResponse(GameSession session) {
        if (session.getConfig() == null) {
            gameConfigRepository.findByGameSessionId(session.getId()).ifPresent(session::setConfig);
        }
        return GameSessionResponse.from(session);
    }
}
