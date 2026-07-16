package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.quest.enums.PlayerQuestStatus;
import com.mos.quest.repository.PlayerQuestRepository;
import com.mos.quest.repository.QuestRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class SocialQuestIntegrationTest {

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
    private WalletRepository walletRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionParticipantRepository sessionParticipantRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String playerToken;
    private UUID playerUserId;

    @BeforeEach
    void setUp() throws Exception {
        playerQuestRepository.deleteAll();
        questRepository.deleteAll();

        String playerUsername = "social_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Social Player")
                .build());
        playerUserId = player.getId();

        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(player)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(player.getNickname())
                .build());

        adminToken = login("marat", "Kv7nR2xP");
        playerToken = login(playerUsername, "player123");
    }

    @Test
    void playerCompletesSocialQuestWithNoteAndReceivesCoins() throws Exception {
        UUID questId = createSocialQuest("EVERY_PLAYER", null, 50);

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quests/" + questId + "/complete")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Told a funny story"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completionNote").value("Told a funny story"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(playerUserId, GAME_SESSION_ID))
                .isPresent()
                .get()
                .extracting("balance")
                .isEqualTo(50L);

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_COMPLETE);
    }

    @Test
    void limitedQuestDisappearsAfterFirstCompletion() throws Exception {
        UUID questId = createSocialQuest("LIMITED", 1, 80);

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quests/" + questId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(playerQuestRepository.findByQuestIdAndUserId(questId, playerUserId))
                .isPresent()
                .get()
                .extracting(pq -> pq.getStatus())
                .isEqualTo(PlayerQuestStatus.FAILED);

        mockMvc.perform(get("/api/quests")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(questId)).isEmpty());

        mockMvc.perform(post("/api/quests/" + questId + "/complete")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void everyPlayerCanCompleteSameQuest() throws Exception {
        UUID questId = createSocialQuest("EVERY_PLAYER", null, 20);

        startAndComplete(adminToken, questId);
        startAndComplete(playerToken, questId);

        assertThat(playerQuestRepository.countByQuestIdAndStatus(questId, PlayerQuestStatus.COMPLETED)).isEqualTo(2);
    }

    @Test
    void adminClosesIncompleteQuests() throws Exception {
        UUID questId = createSocialQuest("EVERY_PLAYER", null, 10);

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/quests/close-incomplete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(playerQuestRepository.findByQuestIdAndUserId(questId, playerUserId))
                .isPresent()
                .get()
                .extracting(pq -> pq.getStatus())
                .isEqualTo(PlayerQuestStatus.FAILED);

        mockMvc.perform(get("/api/quests")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QUEST_CLOSE);
    }

    @Test
    void socialQuestCanRewardItem() throws Exception {
        UUID templateId = itemTemplateRepository.save(com.mos.item.entity.ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Toast Badge")
                .description("For a toast")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build()).getId();

        var result = mockMvc.perform(post("/api/admin/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Gift item quest",
                                  "description":"Do something nice",
                                  "type":"SOCIAL",
                                  "targetConfig":{},
                                  "rewardConfig":{"type":"ITEM","itemTemplateId":"%s"},
                                  "completionPolicy":"EVERY_PLAYER"
                                }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andReturn();

        UUID questId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        startAndComplete(playerToken, questId);

        assertThat(itemTemplateRepository.findById(templateId)).isPresent();
    }

    @Test
    void personalQuestVisibleOnlyToAssignee() throws Exception {
        var createResult = mockMvc.perform(post("/api/admin/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Personal quest",
                                  "description":"Only for one player",
                                  "type":"SOCIAL",
                                  "targetConfig":{},
                                  "rewardConfig":{"type":"COIN","amount":15},
                                  "completionPolicy":"EVERY_PLAYER",
                                  "assigneeUserId":"%s"
                                }
                                """.formatted(playerUserId)))
                .andExpect(status().isOk())
                .andReturn();

        UUID questId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(get("/api/quests")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(questId)).isNotEmpty());

        mockMvc.perform(get("/api/quests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(questId)).isEmpty());

        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        startAndComplete(playerToken, questId);
    }

    private UUID createSocialQuest(String policy, Integer limit, long coins) throws Exception {
        String limitJson = limit == null ? "null" : limit.toString();
        var result = mockMvc.perform(post("/api/admin/quests")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Social quest",
                                  "description":"Do a nice thing",
                                  "type":"SOCIAL",
                                  "targetConfig":{},
                                  "rewardConfig":{"type":"COIN","amount":%d},
                                  "completionPolicy":"%s",
                                  "completionLimit":%s
                                }
                                """.formatted(coins, policy, limitJson)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void startAndComplete(String token, UUID questId) throws Exception {
        mockMvc.perform(post("/api/quests/" + questId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/quests/" + questId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
