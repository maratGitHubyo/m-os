package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.numbers.repository.PlayerNumberRepository;
import com.mos.quest.entity.Quest;
import com.mos.quest.enums.PlayerQuestStatus;
import com.mos.quest.repository.PlayerQuestRepository;
import com.mos.quest.repository.QuestRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class QuestIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private PlayerQuestRepository playerQuestRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private PlayerItemRepository playerItemRepository;

    @Autowired
    private CollectibleNumberRepository collectibleNumberRepository;

    @Autowired
    private PlayerNumberRepository playerNumberRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionParticipantRepository sessionParticipantRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        playerQuestRepository.deleteAll();
        questRepository.deleteAll();
        adminToken = login("admin", "admin123");
    }

    @Test
    void adminCreatesQuest() throws Exception {
        mockMvc.perform(post("/api/admin/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Collect gems",
                                  "description":"Gather two items",
                                  "type":"COLLECT_ITEMS",
                                  "targetConfig":{"count":2},
                                  "rewardConfig":{"type":"COIN","amount":100}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Collect gems"))
                .andExpect(jsonPath("$.type").value("COLLECT_ITEMS"));

        assertThat(questRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_CREATE);
    }

    @Test
    void playerStartsQuest() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerQuest.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playerQuest.progress.current").value(0))
                .andExpect(jsonPath("$.playerQuest.progress.target").value(1));

        assertThat(playerQuestRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_START);
    }

    @Test
    void duplicateStartIsIdempotent() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().stream().filter(log -> log.getAction() == AuditAction.QUEST_START))
                .hasSize(1);
    }

    @Test
    void playerCannotAccessDifferentSessionQuest() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.ACTIVE)
                .build());

        Quest otherQuest = questRepository.save(Quest.builder()
                .gameSessionId(otherSession.getId())
                .title("Other quest")
                .description("Other")
                .type(com.mos.quest.enums.QuestType.COLLECT_ITEMS)
                .status(com.mos.quest.enums.QuestDefinitionStatus.ACTIVE)
                .targetConfig(java.util.Map.of("count", 1))
                .build());

        mockMvc.perform(post("/api/quests/" + otherQuest.getId() + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void collectItemsQuestCompletes() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);
        startQuest(questId);

        UUID templateId = createItemTemplate("Quest Gem");

        mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(ADMIN_USER_ID, templateId)))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findAll().getFirst().getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_COMPLETE);
    }

    @Test
    void collectNumbersQuestCompletes() throws Exception {
        UUID questId = createQuest("""
                {
                  "title":"Collect numbers",
                  "description":"Get one number",
                  "type":"COLLECT_NUMBERS",
                  "targetConfig":{"count":1}
                }
                """);
        startQuest(questId);

        var numberResponse = mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":21}"))
                .andExpect(status().isOk())
                .andReturn();
        UUID numberId = UUID.fromString(
                objectMapper.readTree(numberResponse.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findAll().getFirst().getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
    }

    @Test
    void scoreQuestCompletes() throws Exception {
        UUID questId = createQuest("""
                {
                  "title":"Reach score",
                  "description":"Get 50 points",
                  "type":"REACH_SCORE",
                  "targetConfig":{"category":"TOTAL","threshold":50}
                }
                """);
        startQuest(questId);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":50,"reason":"Quest test"}
                                """))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findAll().getFirst().getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
    }

    @Test
    void rewardGrantedOnComplete() throws Exception {
        UUID templateId = createItemTemplate("Reward Sword");
        UUID questId = createQuest("""
                {
                  "title":"Rewarded quest",
                  "description":"Coin, item and score",
                  "type":"COLLECT_ITEMS",
                  "targetConfig":{"count":1},
                  "rewardConfig":{"type":"COIN","amount":250}
                }
                """);
        startQuest(questId);

        long balanceBefore = walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L);

        grantItem(templateId);

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID)
                .orElseThrow()
                .getBalance()).isEqualTo(balanceBefore + 250);

        UUID itemRewardQuestId = createQuest("""
                {
                  "title":"Item reward quest",
                  "description":"Grant item reward",
                  "type":"COLLECT_ITEMS",
                  "targetConfig":{"count":1},
                  "rewardConfig":{"type":"ITEM","itemTemplateId":"%s"}
                }
                """.formatted(templateId));
        startQuest(itemRewardQuestId);
        grantItem(templateId);

        assertThat(playerItemRepository.findByOwnerIdAndGameSessionIdOrderByAcquiredAtDesc(ADMIN_USER_ID, GAME_SESSION_ID))
                .isNotEmpty();

        UUID scoreRewardQuestId = createQuest("""
                {
                  "title":"Score reward quest",
                  "description":"Grant score reward",
                  "type":"REACH_SCORE",
                  "targetConfig":{"category":"TOTAL","threshold":10},
                  "rewardConfig":{"type":"SCORE","category":"TOTAL","points":15}
                }
                """);
        startQuest(scoreRewardQuestId);

        mockMvc.perform(post("/api/admin/score/" + ADMIN_USER_ID + "/add")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"TOTAL","points":10,"reason":"Score reward quest"}
                                """))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findByQuestIdAndUserId(scoreRewardQuestId, ADMIN_USER_ID)
                .orElseThrow()
                .getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
    }

    @Test
    void completedQuestCannotCompleteTwice() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);
        startQuest(questId);
        UUID templateId = createItemTemplate("Single Gem");
        grantItem(templateId);

        assertThat(playerQuestRepository.findAll().getFirst().getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
        long completeAudits = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() == AuditAction.QUEST_COMPLETE)
                .count();

        grantItem(templateId);

        assertThat(playerQuestRepository.findAll().getFirst().getStatus()).isEqualTo(PlayerQuestStatus.COMPLETED);
        assertThat(auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() == AuditAction.QUEST_COMPLETE)
                .count()).isEqualTo(completeAudits);
    }

    @Test
    void questProgressAuditCreated() throws Exception {
        UUID questId = createCollectItemsQuest(2, null);
        startQuest(questId);
        UUID templateId = createItemTemplate("Progress Gem");
        grantItem(templateId);

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_PROGRESS);
    }

    @Test
    void playerGetsAvailableAndMyQuests() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);
        startQuest(questId);

        mockMvc.perform(get("/api/quests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(questId.toString()));

        mockMvc.perform(get("/api/quests/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questId").value(questId.toString()));
    }

    @Test
    void adminUpdatesQuest() throws Exception {
        UUID questId = createCollectItemsQuest(1, null);

        mockMvc.perform(patch("/api/admin/quests/" + questId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated quest","status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated quest"))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    private UUID createCollectItemsQuest(int count, String rewardJson) throws Exception {
        String reward = rewardJson != null ? ",\"rewardConfig\":" + rewardJson : "";
        return createQuest("""
                {
                  "title":"Collect items",
                  "description":"Collect %d items",
                  "type":"COLLECT_ITEMS",
                  "targetConfig":{"count":%d}%s
                }
                """.formatted(count, count, reward));
    }

    private UUID createQuest(String body) throws Exception {
        var result = mockMvc.perform(post("/api/admin/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );
    }

    private void startQuest(UUID questId) throws Exception {
        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private UUID createItemTemplate(String name) {
        return itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name(name)
                .description("Quest test item")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build()).getId();
    }

    private void grantItem(UUID templateId) throws Exception {
        mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(ADMIN_USER_ID, templateId)))
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
