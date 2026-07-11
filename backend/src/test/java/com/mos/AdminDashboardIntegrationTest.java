package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.location.repository.LocationPointRepository;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.repository.QuestRepository;
import com.mos.seed.DemoDataSeeder;
import com.mos.seed.DemoSeedConstants;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameSessionRepository;
import com.mos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationPointRepository locationPointRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private CollectibleNumberRepository collectibleNumberRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        resetSessionStatus(GameSessionStatus.STARTING);
    }

    @Test
    void demoSeedCreatesData() {
        demoDataSeeder.seedDemoData();

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(gameSessionRepository.findById(DemoSeedConstants.SESSION_ID).orElseThrow().getName())
                .isEqualTo(DemoSeedConstants.DEMO_SESSION_NAME);
        assertThat(locationPointRepository.countByGameSessionId(DemoSeedConstants.SESSION_ID)).isEqualTo(5);
        assertThat(itemTemplateRepository.countByGameSessionId(DemoSeedConstants.SESSION_ID)).isEqualTo(3);
        assertThat(qrCodeRepository.countByGameSessionId(DemoSeedConstants.SESSION_ID)).isEqualTo(3);
        assertThat(questRepository.countByGameSessionIdAndStatus(
                DemoSeedConstants.SESSION_ID, QuestDefinitionStatus.ACTIVE)).isEqualTo(2);
        for (int value = 1; value <= 5; value++) {
            assertThat(collectibleNumberRepository.existsByGameSessionIdAndNumberValue(
                    DemoSeedConstants.SESSION_ID, value)).isTrue();
        }
    }

    @Test
    void adminDashboardReturnsStatistics() {
        demoDataSeeder.seedDemoData();

        try {
            mockMvc.perform(get("/api/admin/dashboard")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentSession.name").value(DemoSeedConstants.DEMO_SESSION_NAME))
                    .andExpect(jsonPath("$.playersCount").value(3))
                    .andExpect(jsonPath("$.activeQuests").value(2))
                    .andExpect(jsonPath("$.locationsCount").value(5))
                    .andExpect(jsonPath("$.itemsCount").value(3))
                    .andExpect(jsonPath("$.qrCount").value(3))
                    .andExpect(jsonPath("$.leaderboard").isArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void playerCannotAccessDashboard() throws Exception {
        demoDataSeeder.seedDemoData();
        String aliceToken = login("alice", DemoSeedConstants.DEMO_PLAYER_PASSWORD);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionLifecycle() throws Exception {
        mockMvc.perform(post("/api/admin/session/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/admin/session/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post("/api/admin/session/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/admin/session/finish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void auditCreatedOnSessionChange() throws Exception {
        mockMvc.perform(post("/api/admin/session/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/session/pause")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/session/finish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SESSION_START);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SESSION_PAUSE);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SESSION_FINISH);
    }

    private void resetSessionStatus(GameSessionStatus status) {
        GameSession session = gameSessionRepository.findById(DemoSeedConstants.SESSION_ID).orElseThrow();
        session.setStatus(status);
        gameSessionRepository.save(session);
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
