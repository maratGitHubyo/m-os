package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.secret.entity.PlayerSecret;
import com.mos.secret.enums.SecretRewardType;
import com.mos.secret.repository.PlayerSecretRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class SecretIntegrationTest {

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
    private PlayerSecretRepository playerSecretRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private PlayerItemRepository playerItemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String playerToken;
    private UUID playerUserId;

    @BeforeEach
    void setUp() throws Exception {
        String playerUsername = "player_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Player")
                .build());
        playerUserId = player.getId();

        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(player)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(player.getNickname())
                .build());

        adminToken = login("admin", "admin123");
        playerToken = login(playerUsername, "player123");
    }

    @Test
    void adminCreatesPlayerSecret() throws Exception {
        createSecret(ADMIN_USER_ID, "STAR-42", "COIN", "{\"amount\":100}");

        assertThat(playerSecretRepository.findAll()).hasSize(1);
    }

    @Test
    void redeemCoinSecretGrantsWalletAndAudit() throws Exception {
        createSecret(ADMIN_USER_ID, "COIN-SECRET", "COIN", "{\"amount\":250}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"COIN-SECRET"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coinAmount").value(250))
                .andExpect(jsonPath("$.title").exists());

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(250L);
        assertThat(coinTransactionRepository.findAll()).hasSize(1);
        assertThat(playerSecretRepository.findAll().getFirst().getUsed()).isTrue();
        assertThat(playerSecretRepository.findAll().getFirst().getUsedAt()).isNotNull();
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.SECRET_REDEEM);
    }

    @Test
    void redeemItemSecretGrantsItem() throws Exception {
        UUID templateId = createItemTemplate();
        createSecret(ADMIN_USER_ID, "ITEM-SECRET", "ITEM", "{\"itemTemplateId\":\"" + templateId + "\"}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ITEM-SECRET"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantedItem.template.id").value(templateId.toString()));

        assertThat(playerItemRepository.findAll()).hasSize(1);
    }

    @Test
    void redeemNoneSecretMarksUsedWithoutReward() throws Exception {
        createSecret(ADMIN_USER_ID, "NONE-SECRET", "NONE", "{}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"NONE-SECRET"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("NONE"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L)).isZero();
        assertThat(playerItemRepository.findAll()).isEmpty();
        assertThat(playerSecretRepository.findAll().getFirst().getUsed()).isTrue();
    }

    @Test
    void cannotRedeemSecretAssignedToAnotherUser() throws Exception {
        createSecret(playerUserId, "PLAYER-ONLY", "COIN", "{\"amount\":50}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PLAYER-ONLY"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This secret is not assigned to you"));

        assertThat(playerSecretRepository.findAll().getFirst().getUsed()).isFalse();
    }

    @Test
    void cannotRedeemAlreadyUsedSecret() throws Exception {
        createSecret(ADMIN_USER_ID, "ONCE-SECRET", "COIN", "{\"amount\":10}");
        redeemAs(adminToken, "ONCE-SECRET");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ONCE-SECRET"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Secret code has already been used"));
    }

    @Test
    void secretFromDifferentSessionIsNotFound() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        playerSecretRepository.save(PlayerSecret.builder()
                .userId(ADMIN_USER_ID)
                .gameSessionId(otherSession.getId())
                .code("FOREIGN-SECRET")
                .title("Foreign")
                .description("Other session")
                .rewardType(SecretRewardType.COIN)
                .rewardPayload(Map.of("amount", 100))
                .build());

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"FOREIGN-SECRET"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Secret code not found"));
    }

    @Test
    void numberRewardIsNotYetImplemented() throws Exception {
        createSecret(ADMIN_USER_ID, "NUMBER-SECRET", "NUMBER", "{\"number\":7}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"NUMBER-SECRET"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message").value("Reward type not yet implemented: NUMBER"));

        assertThat(playerSecretRepository.findAll().getFirst().getUsed()).isFalse();
    }

    @Test
    void questRewardIsNotYetImplemented() throws Exception {
        UUID questId = UUID.randomUUID();
        createSecret(ADMIN_USER_ID, "QUEST-SECRET", "QUEST", "{\"questId\":\"" + questId + "\"}");

        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"QUEST-SECRET"}
                                """))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message").value("Reward type not yet implemented: QUEST"));

        assertThat(playerSecretRepository.findAll().getFirst().getUsed()).isFalse();
    }

    private void createSecret(UUID userId, String code, String rewardType, String payloadJson) throws Exception {
        mockMvc.perform(post("/api/admin/secrets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"%s",
                                  "code":"%s",
                                  "title":"Test Secret",
                                  "description":"Secret description",
                                  "rewardType":"%s",
                                  "rewardPayload":%s
                                }
                                """.formatted(userId, code, rewardType, payloadJson)))
                .andExpect(status().isOk());
    }

    private UUID createItemTemplate() {
        return itemTemplateRepository.save(com.mos.item.entity.ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("Secret Item")
                .description("From secret")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build()).getId();
    }

    private void redeemAs(String token, String code) throws Exception {
        mockMvc.perform(post("/api/secrets/redeem")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
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
