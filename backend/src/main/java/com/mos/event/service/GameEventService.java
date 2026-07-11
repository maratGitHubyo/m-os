package com.mos.event.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.GameEventNotFoundException;
import com.mos.event.dto.CreateGameEventRequest;
import com.mos.event.dto.GameEventBroadcastMessage;
import com.mos.event.dto.GameEventResponse;
import com.mos.event.entity.EventParticipation;
import com.mos.event.entity.GameEvent;
import com.mos.event.enums.GameEventStatus;
import com.mos.event.repository.EventParticipationRepository;
import com.mos.event.repository.GameEventRepository;
import com.mos.event.websocket.GameEventWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameEventService {

    private final GameEventRepository gameEventRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final AuditService auditService;
    private final GameEventWebSocketPublisher gameEventWebSocketPublisher;

    @Transactional
    public GameEventResponse createEvent(UUID gameSessionId, CreateGameEventRequest request, UUID createdBy) {
        GameEvent event = gameEventRepository.save(GameEvent.builder()
                .gameSessionId(gameSessionId)
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .status(GameEventStatus.SCHEDULED)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .config(request.config() != null ? new HashMap<>(request.config()) : new HashMap<>())
                .createdBy(createdBy)
                .build());

        auditService.log(
                createdBy,
                gameSessionId,
                AuditAction.EVENT_CREATE,
                "GameEvent",
                event.getId().toString(),
                "Game event created: " + event.getTitle(),
                Map.of(
                        "eventId", event.getId().toString(),
                        "type", event.getType().name(),
                        "status", event.getStatus().name()
                )
        );

        return GameEventResponse.from(event);
    }

    @Transactional
    public GameEventResponse updateStatus(
            UUID eventId,
            UUID gameSessionId,
            GameEventStatus newStatus,
            UUID performedByUserId
    ) {
        GameEvent event = getEventForSession(eventId, gameSessionId);
        GameEventStatus previousStatus = event.getStatus();

        event.setStatus(newStatus);
        event = gameEventRepository.save(event);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.EVENT_STATUS_CHANGE,
                "GameEvent",
                event.getId().toString(),
                "Game event status changed: " + previousStatus + " -> " + newStatus,
                Map.of(
                        "eventId", event.getId().toString(),
                        "previousStatus", previousStatus.name(),
                        "newStatus", newStatus.name()
                )
        );

        gameEventWebSocketPublisher.publishStatusChange(new GameEventBroadcastMessage(
                event.getId(),
                event.getGameSessionId(),
                event.getStatus(),
                event.getTitle(),
                Instant.now()
        ));

        return GameEventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<GameEventResponse> getEvents(UUID gameSessionId) {
        return gameEventRepository.findByGameSessionIdOrderByStartAtAscCreatedAtAsc(gameSessionId).stream()
                .map(GameEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameEventResponse getEvent(UUID eventId, UUID gameSessionId) {
        return GameEventResponse.from(getEventForSession(eventId, gameSessionId));
    }

    @Transactional
    public EventParticipation recordParticipation(UUID userId, UUID eventId, UUID gameSessionId) {
        GameEvent event = getEventForSession(eventId, gameSessionId);

        return eventParticipationRepository.findByUserIdAndEventId(userId, event.getId())
                .orElseGet(() -> eventParticipationRepository.save(EventParticipation.builder()
                        .userId(userId)
                        .event(event)
                        .build()));
    }

    private GameEvent getEventForSession(UUID eventId, UUID gameSessionId) {
        return gameEventRepository.findByIdAndGameSessionId(eventId, gameSessionId)
                .orElseThrow(GameEventNotFoundException::new);
    }
}
