package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.service.ItemService;
import com.mos.location.entity.LocationPoint;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.service.LocationService;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.numbers.repository.PlayerNumberRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.victory.dto.VictoryBroadcastMessage;
import com.mos.victory.entity.VictoryCondition;
import com.mos.victory.enums.VictoryConditionType;
import com.mos.victory.repository.VictoryConditionRepository;
import com.mos.victory.websocket.VictoryWebSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class VictoryIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VictoryConditionRepository victoryConditionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private GameConfigRepository gameConfigRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private LocationPointRepository locationPointRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CollectibleNumberRepository collectibleNumberRepository;

    @Autowired
    private PlayerNumberRepository playerNumberRepository;

    @Autowired
    private VictoryWebSocketPublisher victoryWebSocketPublisher;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        victoryConditionRepository.deleteAll();
        playerNumberRepository.deleteAll();
        collectibleNumberRepository.deleteAll();
        adminToken = login("admin", "admin123");
    }

    @Test
    void adminCreatesVictoryCondition() throws Exception {
        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"REACH_SCORE",
                                  "targetValue":{"category":"TOTAL","threshold":500},
                                  "description":"Reach 500 total points",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REACH_SCORE"))
                .andExpect(jsonPath("$.description").value("Reach 500 total points"))
                .andExpect(jsonPath("$.achieved").value(false));

        assertThat(victoryConditionRepository.findAll()).hasSize(1);
    }

    @Test
    void playerSeesVictoryConditionsWithProgress() throws Exception {
        createReachScoreCondition(500);

        mockMvc.perform(get("/api/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("REACH_SCORE"))
                .andExpect(jsonPath("$[0].achieved").value(false))
                .andExpect(jsonPath("$[0].progressCurrent").value(0))
                .andExpect(jsonPath("$[0].progressTarget").value(500));
    }

    @Test
    void reachScoreConditionAchievedOnScoreChange() throws Exception {
        createReachScoreCondition(100);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":100,"reason":"Victory test"}
                                """))
                .andExpect(status().isOk());

        VictoryCondition condition = victoryConditionRepository.findAll().getFirst();
        assertThat(condition.getAchievedAt()).isNotNull();
        assertThat(condition.getAchievedByUserId()).isEqualTo(ADMIN_USER_ID);

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.VICTORY_ACHIEVED);

        ArgumentCaptor<VictoryBroadcastMessage> captor = ArgumentCaptor.forClass(VictoryBroadcastMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq(victoryWebSocketPublisher.topicForSession(GAME_SESSION_ID)),
                captor.capture()
        );
        assertThat(captor.getValue().type()).isEqualTo(VictoryConditionType.REACH_SCORE);
        assertThat(captor.getValue().achievedByUserId()).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void collectUniqueItemsConditionAchieved() throws Exception {
        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"COLLECT_UNIQUE_ITEMS",
                                  "targetValue":{"count":2},
                                  "description":"Collect 2 unique items",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());

        ItemTemplate first = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Unique A")
                .description("A")
                .rarity(ItemRarity.RARE)
                .isUnique(true)
                .build());
        ItemTemplate second = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Unique B")
                .description("B")
                .rarity(ItemRarity.RARE)
                .isUnique(true)
                .build());

        itemService.grantItem(ADMIN_USER_ID, first.getId(), GAME_SESSION_ID, ADMIN_USER_ID);
        itemService.grantItem(ADMIN_USER_ID, second.getId(), GAME_SESSION_ID, ADMIN_USER_ID);

        VictoryCondition condition = victoryConditionRepository.findAll().getFirst();
        assertThat(condition.getAchievedAt()).isNotNull();
        assertThat(condition.getAchievedByUserId()).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void findAllLocationsConditionAchieved() throws Exception {
        LocationPoint first = locationPointRepository.save(LocationPoint.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Point A")
                .description("A")
                .x(10.0)
                .y(20.0)
                .zone("north")
                .hidden(false)
                .build());
        LocationPoint second = locationPointRepository.save(LocationPoint.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Point B")
                .description("B")
                .x(30.0)
                .y(40.0)
                .zone("south")
                .hidden(false)
                .build());

        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"FIND_ALL_LOCATIONS",
                                  "targetValue":{},
                                  "description":"Discover every location",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());

        locationService.discoverLocation(ADMIN_USER_ID, first.getId(), GAME_SESSION_ID);
        assertThat(victoryConditionRepository.findAll().getFirst().getAchievedAt()).isNull();

        locationService.discoverLocation(ADMIN_USER_ID, second.getId(), GAME_SESSION_ID);
        assertThat(victoryConditionRepository.findAll().getFirst().getAchievedAt()).isNotNull();
    }

    @Test
    void collectAllNumbersConditionAchieved() throws Exception {
        var config = gameConfigRepository.findByGameSessionId(GAME_SESSION_ID).orElseThrow();
        config.setNumbersTotal(2);
        gameConfigRepository.save(config);

        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"COLLECT_ALL_NUMBERS",
                                  "targetValue":{},
                                  "description":"Collect all numbers",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());

        int firstValue = 10_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(400_000);
        int secondValue = firstValue + 1;

        var firstNumber = mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + firstValue + "}"))
                .andExpect(status().isOk())
                .andReturn();
        UUID firstNumberId = UUID.fromString(
                objectMapper.readTree(firstNumber.getResponse().getContentAsString()).get("id").asText()
        );

        var secondNumber = mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + secondValue + "}"))
                .andExpect(status().isOk())
                .andReturn();
        UUID secondNumberId = UUID.fromString(
                objectMapper.readTree(secondNumber.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(post("/api/admin/numbers/" + firstNumberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].progressCurrent").value(1))
                .andExpect(jsonPath("$[0].progressTarget").value(2))
                .andExpect(jsonPath("$[0].achieved").value(false));

        mockMvc.perform(post("/api/admin/numbers/" + secondNumberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].achieved").value(true))
                .andExpect(jsonPath("$[0].achievedByMe").value(true));

        assertThat(victoryConditionRepository.findAll().getFirst().getAchievedAt()).isNotNull();
        assertThat(victoryConditionRepository.findAll().getFirst().getAchievedByUserId()).isEqualTo(ADMIN_USER_ID);
    }

    @Test
    void orConditionsOnlyOneNeedsAchievement() throws Exception {
        createReachScoreCondition(1000);
        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"REACH_SCORE",
                                  "targetValue":{"category":"TOTAL","threshold":50},
                                  "description":"Reach 50 points",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":50,"reason":"Partial victory"}
                                """))
                .andExpect(status().isOk());

        var conditions = victoryConditionRepository.findAll();
        long achievedCount = conditions.stream().filter(c -> c.getAchievedAt() != null).count();
        assertThat(achievedCount).isEqualTo(1);
        assertThat(conditions.stream().filter(c -> c.getAchievedAt() == null)).hasSize(1);
    }

    @Test
    void finishSessionOnVictoryWhenConfigured() throws Exception {
        var config = gameConfigRepository.findByGameSessionId(GAME_SESSION_ID).orElseThrow();
        config.setCustomSettings(new HashMap<>(config.getCustomSettings()));
        config.getCustomSettings().put("finishSessionOnVictory", true);
        gameConfigRepository.save(config);

        createReachScoreCondition(10);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":10,"reason":"Finish session"}
                                """))
                .andExpect(status().isOk());

        GameSession session = gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.FINISHED);

        mockMvc.perform(get("/api/admin/victory-conditions/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anyAchieved").value(true))
                .andExpect(jsonPath("$.sessionFinished").value(true));
    }

    @Test
    void adminStatusListsAllConditions() throws Exception {
        createReachScoreCondition(200);

        mockMvc.perform(get("/api/admin/victory-conditions/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameSessionId").value(GAME_SESSION_ID.toString()))
                .andExpect(jsonPath("$.conditions.length()").value(1))
                .andExpect(jsonPath("$.anyAchieved").value(false));
    }

    private void createReachScoreCondition(long threshold) throws Exception {
        mockMvc.perform(post("/api/admin/victory-conditions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"REACH_SCORE",
                                  "targetValue":{"category":"TOTAL","threshold":%d},
                                  "description":"Reach %d total points",
                                  "active":true
                                }
                                """.formatted(threshold, threshold)))
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
