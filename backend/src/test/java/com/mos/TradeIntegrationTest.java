package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.item.service.ItemService;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.trade.enums.TradeStatus;
import com.mos.trade.repository.TradeCoinRepository;
import com.mos.trade.repository.TradeItemRepository;
import com.mos.trade.repository.TradeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TradeIntegrationTest {

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
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private PlayerItemRepository playerItemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TradeItemRepository tradeItemRepository;

    @Autowired
    private TradeCoinRepository tradeCoinRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID playerUserId;
    private String playerUsername;
    private String adminToken;
    private String playerToken;

    @BeforeEach
    void setUp() throws Exception {
        tradeItemRepository.deleteAll();
        tradeCoinRepository.deleteAll();
        tradeRepository.deleteAll();
        playerItemRepository.deleteAll();
        itemTemplateRepository.deleteAll();
        auditLogRepository.deleteAll();

        playerUsername = "trader_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Trader")
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
    void createTrade() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Admin Gem");
        creditWallet(ADMIN_USER_ID, 200);

        mockMvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId":"%s",
                                  "initiatorItemIds":["%s"],
                                  "receiverItemIds":[],
                                  "initiatorCoins":100,
                                  "receiverCoins":0
                                }
                                """.formatted(playerUserId, adminItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.initiatorId").value(ADMIN_USER_ID.toString()))
                .andExpect(jsonPath("$.receiverId").value(playerUserId.toString()));

        assertThat(tradeRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.TRADE_CREATE);
    }

    @Test
    void receiverAcceptsTrade() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Trade Gem");
        UUID playerItemId = grantItem(playerUserId, "Player Gem");
        creditWallet(ADMIN_USER_ID, 150);
        creditWallet(playerUserId, 50);

        UUID tradeId = createTrade(adminToken, """
                {
                  "receiverId":"%s",
                  "initiatorItemIds":["%s"],
                  "receiverItemIds":["%s"],
                  "initiatorCoins":100,
                  "receiverCoins":25
                }
                """.formatted(playerUserId, adminItemId, playerItemId));

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(playerItemRepository.findById(adminItemId).orElseThrow().getOwnerId()).isEqualTo(playerUserId);
        assertThat(playerItemRepository.findById(playerItemId).orElseThrow().getOwnerId()).isEqualTo(ADMIN_USER_ID);
        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(75L);
        assertThat(walletRepository.findByUserIdAndGameSessionId(playerUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(125L);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.TRADE_ACCEPT);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.ITEM_TRANSFER);
    }

    @Test
    void receiverDeclinesTrade() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Decline Gem");
        UUID tradeId = createTrade(adminToken, """
                {
                  "receiverId":"%s",
                  "initiatorItemIds":["%s"],
                  "receiverItemIds":[],
                  "initiatorCoins":0,
                  "receiverCoins":0
                }
                """.formatted(playerUserId, adminItemId));

        mockMvc.perform(post("/api/trades/" + tradeId + "/decline")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));

        assertThat(tradeRepository.findById(tradeId).orElseThrow().getStatus()).isEqualTo(TradeStatus.DECLINED);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.TRADE_DECLINE);
    }

    @Test
    void initiatorCancelsTrade() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Cancel Gem");
        UUID tradeId = createTrade(adminToken, """
                {
                  "receiverId":"%s",
                  "initiatorItemIds":["%s"],
                  "receiverItemIds":[],
                  "initiatorCoins":0,
                  "receiverCoins":0
                }
                """.formatted(playerUserId, adminItemId));

        mockMvc.perform(post("/api/trades/" + tradeId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.TRADE_CANCEL);
    }

    @Test
    void cannotTradeItemFromAnotherSession() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Foreign Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        ItemTemplate foreignTemplate = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(otherSession.getId())
                .name("Foreign Item")
                .description("Foreign")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build());

        UUID foreignItemId = playerItemRepository.save(com.mos.item.entity.PlayerItem.builder()
                .itemTemplate(foreignTemplate)
                .ownerId(ADMIN_USER_ID)
                .gameSessionId(otherSession.getId())
                .acquiredFrom(com.mos.item.enums.ItemAcquisitionSource.ADMIN)
                .build()).getId();

        mockMvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId":"%s",
                                  "initiatorItemIds":["%s"],
                                  "receiverItemIds":[],
                                  "initiatorCoins":0,
                                  "receiverCoins":0
                                }
                                """.formatted(playerUserId, foreignItemId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotTradeNotOwnedItem() throws Exception {
        UUID playerItemId = grantItem(playerUserId, "Not Mine");

        mockMvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId":"%s",
                                  "initiatorItemIds":["%s"],
                                  "receiverItemIds":[],
                                  "initiatorCoins":0,
                                  "receiverCoins":0
                                }
                                """.formatted(playerUserId, playerItemId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotAcceptTwice() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Twice Gem");
        UUID tradeId = createTrade(adminToken, """
                {
                  "receiverId":"%s",
                  "initiatorItemIds":["%s"],
                  "receiverItemIds":[],
                  "initiatorCoins":0,
                  "receiverCoins":0
                }
                """.formatted(playerUserId, adminItemId));

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trades/" + tradeId + "/accept")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void tradeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/trades"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tradeWithInsufficientCoins() throws Exception {
        UUID adminItemId = grantItem(ADMIN_USER_ID, "Poor Gem");

        mockMvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverId":"%s",
                                  "initiatorItemIds":["%s"],
                                  "receiverItemIds":[],
                                  "initiatorCoins":9999,
                                  "receiverCoins":0
                                }
                                """.formatted(playerUserId, adminItemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient coins for this trade"));
    }

    private UUID createTrade(String token, String body) throws Exception {
        var result = mockMvc.perform(post("/api/trades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );
    }

    private UUID grantItem(UUID userId, String name) {
        UUID templateId = itemService.createTemplate(
                GAME_SESSION_ID,
                new com.mos.item.dto.CreateItemTemplateRequest(name, "Trade test", null, ItemRarity.COMMON, false)
        ).id();
        return itemService.grantItem(userId, templateId, GAME_SESSION_ID, ADMIN_USER_ID).id();
    }

    private void creditWallet(UUID userId, long amount) throws Exception {
        mockMvc.perform(post("/api/admin/wallet/" + userId + "/credit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d,"description":"Trade test funds"}
                                """.formatted(amount)))
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
