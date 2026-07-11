package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.score.enums.ScoreCategory;
import com.mos.score.repository.PlayerScoreRepository;
import com.mos.score.repository.ScoreTransactionRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class ScoreIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionParticipantRepository sessionParticipantRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private GameConfigRepository gameConfigRepository;

    @Autowired
    private PlayerScoreRepository playerScoreRepository;

    @Autowired
    private ScoreTransactionRepository scoreTransactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID playerUserId;
    private SessionParticipant playerParticipant;

    @BeforeEach
    void setUp() throws Exception {
        String playerUsername = "player_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Live Nickname")
                .build());
        playerUserId = player.getId();

        playerParticipant = sessionParticipantRepository.save(SessionParticipant.builder()
                .user(player)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot("Snapshot Nickname")
                .build());

        adminToken = login("admin", "admin123");
    }

    @Test
    void adminAddsScoreCreatesTransactionAndAudit() throws Exception {
        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":100,"reason":"Birthday bonus"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(100))
                .andExpect(jsonPath("$.category").value("TOTAL"));

        assertThat(playerScoreRepository.findAll()).hasSize(1);
        assertThat(scoreTransactionRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SCORE_ADD);
    }

    @Test
    void adminSubtractsScore() throws Exception {
        addScore(ADMIN_USER_ID, 200);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/subtract")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":50,"reason":"Penalty"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delta").value(-50));

        assertThat(playerScoreRepository.findAll().getFirst().getPoints()).isEqualTo(150L);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SCORE_SUBTRACT);
    }

    @Test
    void getMyScoresReturnsAllCategories() throws Exception {
        addScore(ADMIN_USER_ID, 75);

        mockMvc.perform(get("/api/score/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[?(@.category=='TOTAL')].points").value(75));
    }

    @Test
    void leaderboardSortedByPointsUsesNicknameSnapshot() throws Exception {
        addScore(playerUserId, 300);
        addScore(ADMIN_USER_ID, 100);

        mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].nickname").value("Snapshot Nickname"))
                .andExpect(jsonPath("$[0].points").value(300))
                .andExpect(jsonPath("$[1].nickname").value("Admin"));
    }

    @Test
    void leaderboardDisabledReturnsForbidden() throws Exception {
        var config = gameConfigRepository.findByGameSessionId(GAME_SESSION_ID).orElseThrow();
        config.setLeaderboardEnabled(false);
        gameConfigRepository.save(config);

        mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Leaderboard is disabled for this game session"));
    }

    @Test
    void cannotSubtractBelowZero() throws Exception {
        addScore(ADMIN_USER_ID, 30);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/subtract")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":50,"reason":"Too much"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient score points"));
    }

    @Test
    void scoresAreIsolatedByGameSession() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        playerScoreRepository.save(com.mos.score.entity.PlayerScore.builder()
                .userId(ADMIN_USER_ID)
                .gameSessionId(otherSession.getId())
                .category(ScoreCategory.TOTAL)
                .points(999L)
                .build());

        addScore(ADMIN_USER_ID, 10);

        mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId=='" + ADMIN_USER_ID + "')].points").value(10));
    }

    private void addScore(UUID userId, long points) throws Exception {
        mockMvc.perform(post("/api/admin/score/" + userId + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":%d,"reason":"Test points"}
                                """.formatted(points)))
                .andExpect(status().isOk());
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
