package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.location.service.LocationService;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.qrcode.repository.QrScanRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class QrScanIntegrationTest {

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
    private QrCodeRepository qrCodeRepository;

    @Autowired
    private QrScanRepository qrScanRepository;

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
    private LocationService locationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String playerToken;
    private UUID playerUserId;

    @BeforeEach
    void setUp() throws Exception {
        String playerUsername = "qrscan_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("QR Scanner")
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
    void firstScanByPublicIdGrantsReward() throws Exception {
        UUID publicId = createSimpleCoinQr("Treasure Chest", 50L);

        mockMvc.perform(post("/api/qr/scan/" + publicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.title").value("Treasure Chest"))
                .andExpect(jsonPath("$.reward.coins").value(50))
                .andExpect(jsonPath("$.message").value("Reward received"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID))
                .isPresent()
                .get()
                .extracting("balance")
                .isEqualTo(50L);
        assertThat(qrScanRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QR_SCAN);
    }

    @Test
    void duplicateScanBySamePlayerReturnsAlreadyScanned() throws Exception {
        UUID publicId = createSimpleCoinQr("Duplicate QR", 25L);

        scanByPublicId(adminToken, publicId);

        mockMvc.perform(post("/api/qr/scan/" + publicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("QR already scanned"));

        assertThat(qrScanRepository.findAll()).hasSize(1);
        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(25L);
    }

    @Test
    void anotherPlayerCanScanSameQrWithEveryPlayerPolicy() throws Exception {
        UUID publicId = createSimpleCoinQr("Shared Treasure", 30L);

        scanByPublicId(adminToken, publicId);
        scanByPublicId(playerToken, publicId);

        assertThat(qrScanRepository.findAll()).hasSize(2);
        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(30L);
        assertThat(walletRepository.findByUserIdAndGameSessionId(playerUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(30L);
    }

    @Test
    void qrFromDifferentSessionIsUnavailable() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Foreign Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        UUID publicId = qrCodeRepository.save(com.mos.qrcode.entity.QrCode.builder()
                .gameSessionId(otherSession.getId())
                .code("FOREIGN-QR")
                .title("Foreign QR")
                .rewardType(com.mos.qrcode.enums.QrRewardType.COIN)
                .rewardPayload(java.util.Map.of("amount", 100))
                .scanPolicy(com.mos.qrcode.enums.QrScanPolicy.EVERY_PLAYER)
                .build()).getPublicId();

        mockMvc.perform(post("/api/qr/scan/" + publicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("QR unavailable"));

        assertThat(qrScanRepository.findAll()).isEmpty();
    }

    @Test
    void adminCanDownloadQrImage() throws Exception {
        UUID qrId = createSimpleCoinQrAndReturnId("Printable QR", 10L);

        mockMvc.perform(get("/api/admin/qr/" + qrId + "/image")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("image/png"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(100));
    }

    private UUID createSimpleCoinQr(String title, long amount) throws Exception {
        var result = mockMvc.perform(post("/api/admin/qr/simple")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "rewardKind":"COIN",
                                  "coinAmount":%d,
                                  "scanPolicy":"EVERY_PLAYER"
                                }
                                """.formatted(title, amount)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("publicId").asText());
    }

    private UUID createSimpleCoinQrAndReturnId(String title, long amount) throws Exception {
        var result = mockMvc.perform(post("/api/admin/qr/simple")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "rewardKind":"COIN",
                                  "coinAmount":%d,
                                  "scanPolicy":"EVERY_PLAYER"
                                }
                                """.formatted(title, amount)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void scanByPublicId(String token, UUID publicId) throws Exception {
        mockMvc.perform(post("/api/qr/scan/" + publicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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
