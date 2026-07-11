package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.event.dto.GameEventBroadcastMessage;
import com.mos.event.entity.GameEvent;
import com.mos.event.enums.GameEventStatus;
import com.mos.event.enums.GameEventType;
import com.mos.event.repository.EventParticipationRepository;
import com.mos.event.repository.GameEventRepository;
import com.mos.event.service.GameEventService;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class GameEventIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private EventParticipationRepository eventParticipationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GameEventService gameEventService;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
    }

    @Test
    void adminCreatesGameEvent() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"ANNOUNCEMENT",
                                  "title":"Welcome",
                                  "description":"Game starts now",
                                  "startAt":null,
                                  "endAt":null,
                                  "config":{"priority":"high"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Welcome"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        assertThat(gameEventRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.EVENT_CREATE);
    }

    @Test
    void playerListsAndGetsEvents() throws Exception {
        UUID eventId = createEvent("Bonus Round", "BONUS_PERIOD");

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Bonus Round"));

        mockMvc.perform(get("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()));
    }

    @Test
    void adminUpdatesStatusCreatesAuditAndBroadcast() throws Exception {
        UUID eventId = createEvent("Freeze Leaderboard", "LEADERBOARD_FREEZE");

        mockMvc.perform(patch("/api/admin/events/" + eventId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.EVENT_STATUS_CHANGE);

        ArgumentCaptor<GameEventBroadcastMessage> captor = ArgumentCaptor.forClass(GameEventBroadcastMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/session/" + GAME_SESSION_ID + "/events"),
                captor.capture()
        );
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().status()).isEqualTo(GameEventStatus.RUNNING);
    }

    @Test
    void auctionTypeIsStoredWithoutAuctionLogic() throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"AUCTION",
                                  "title":"Future Auction",
                                  "description":"Placeholder only",
                                  "config":{}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("AUCTION"));
    }

    @Test
    void eventFromDifferentSessionIsNotFound() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        GameEvent foreignEvent = gameEventRepository.save(GameEvent.builder()
                .gameSessionId(otherSession.getId())
                .type(GameEventType.CUSTOM)
                .title("Foreign")
                .description("Other session")
                .status(GameEventStatus.SCHEDULED)
                .createdBy(ADMIN_USER_ID)
                .build());

        mockMvc.perform(get("/api/events/" + foreignEvent.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordParticipationIsIdempotent() {
        UUID eventId = createEventQuietly("Participation Event");

        gameEventService.recordParticipation(ADMIN_USER_ID, eventId, GAME_SESSION_ID);
        gameEventService.recordParticipation(ADMIN_USER_ID, eventId, GAME_SESSION_ID);

        assertThat(eventParticipationRepository.findAll()).hasSize(1);
    }

    private UUID createEvent(String title, String type) throws Exception {
        var result = mockMvc.perform(post("/api/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"%s",
                                  "title":"%s",
                                  "description":"Test event",
                                  "config":{}
                                }
                                """.formatted(type, title)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createEventQuietly(String title) {
        return gameEventService.createEvent(
                GAME_SESSION_ID,
                new com.mos.event.dto.CreateGameEventRequest(
                        GameEventType.ANNOUNCEMENT,
                        title,
                        "Test",
                        null,
                        null,
                        java.util.Map.of()
                ),
                ADMIN_USER_ID
        ).id();
    }

    private String login(String username, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
