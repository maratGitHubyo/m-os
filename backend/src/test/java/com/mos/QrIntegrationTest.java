package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.repository.PlayerLocationDiscoveryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class QrIntegrationTest {

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
    private LocationPointRepository locationPointRepository;

    @Autowired
    private PlayerLocationDiscoveryRepository playerLocationDiscoveryRepository;

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
        String playerUsername = "player_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Player Two")
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
    void adminCreatesQrCode() throws Exception {
        mockMvc.perform(post("/api/admin/qr")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"GARDEN-01",
                                  "locationPointId":null,
                                  "rewardType":"COIN",
                                  "rewardPayload":{"amount":100},
                                  "scanPolicy":"EVERY_PLAYER",
                                  "scanLimit":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GARDEN-01"))
                .andExpect(jsonPath("$.scanPolicy").value("EVERY_PLAYER"));

        assertThat(qrCodeRepository.findAll()).hasSize(1);
    }

    @Test
    void scanGrantsCoinsAndWritesAuditLog() throws Exception {
        createQrCode("COIN-100", "COIN", "{\"amount\":100}", "EVERY_PLAYER", null);

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"COIN-100"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coinAmount").value(100))
                .andExpect(jsonPath("$.rewardType").value("COIN"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID))
                .isPresent()
                .get()
                .extracting("balance")
                .isEqualTo(100L);

        assertThat(coinTransactionRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.QR_SCAN);
    }

    @Test
    void everyPlayerPolicyAllowsEachPlayerOnce() throws Exception {
        createQrCode("SHARED-COIN", "COIN", "{\"amount\":50}", "EVERY_PLAYER", null);

        scanAs(adminToken, "SHARED-COIN");
        scanAs(playerToken, "SHARED-COIN");

        assertThat(qrScanRepository.findAll()).hasSize(2);
        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(50L);
        assertThat(walletRepository.findByUserIdAndGameSessionId(playerUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(50L);
    }

    @Test
    void firstPlayerPolicyBlocksSecondScanner() throws Exception {
        createQrCode("FIRST-ONLY", "COIN", "{\"amount\":200}", "FIRST_PLAYER", null);

        scanAs(adminToken, "FIRST-ONLY");

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"FIRST-ONLY"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("QR code scan limit reached"));

        assertThat(qrScanRepository.findAll()).hasSize(1);
    }

    @Test
    void limitedPolicyRespectsScanLimit() throws Exception {
        createQrCode("LIMIT-2", "COIN", "{\"amount\":10}", "LIMITED", 2);

        scanAs(adminToken, "LIMIT-2");
        scanAs(playerToken, "LIMIT-2");

        String thirdUsername = "player3_" + UUID.randomUUID().toString().substring(0, 8);
        User thirdPlayer = userRepository.save(User.builder()
                .username(thirdUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Player Three")
                .build());
        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(thirdPlayer)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(thirdPlayer.getNickname())
                .build());
        String thirdToken = login(thirdUsername, "player123");

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + thirdToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"LIMIT-2"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("QR code scan limit reached"));

        assertThat(qrScanRepository.findAll()).hasSize(2);
    }

    @Test
    void scanGrantsItem() throws Exception {
        UUID templateId = createItemTemplate();
        createQrCode("ITEM-QR", "ITEM", "{\"itemTemplateId\":\"" + templateId + "\"}", "EVERY_PLAYER", null);

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ITEM-QR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardType").value("ITEM"))
                .andExpect(jsonPath("$.grantedItem.template.id").value(templateId.toString()));

        assertThat(playerItemRepository.findAll()).hasSize(1);
    }

    @Test
    void scanDiscoversLinkedLocation() throws Exception {
        UUID locationId = createHiddenLocation();
        createQrCodeWithLocation("DISCOVER-QR", locationId);

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DISCOVER-QR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationDiscovered").value(true));

        assertThat(playerLocationDiscoveryRepository.findAll()).hasSize(1);
    }

    @Test
    void duplicateScanRejected() throws Exception {
        createQrCode("ONCE-QR", "COIN", "{\"amount\":25}", "EVERY_PLAYER", null);
        scanAs(adminToken, "ONCE-QR");

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ONCE-QR"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already scanned this QR code"));
    }

    @Test
    void qrCodeFromDifferentSessionIsNotFound() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        qrCodeRepository.save(com.mos.qrcode.entity.QrCode.builder()
                .gameSessionId(otherSession.getId())
                .code("FOREIGN-QR")
                .rewardType(com.mos.qrcode.enums.QrRewardType.COIN)
                .rewardPayload(java.util.Map.of("amount", 100))
                .scanPolicy(com.mos.qrcode.enums.QrScanPolicy.EVERY_PLAYER)
                .build());

        mockMvc.perform(post("/api/qr/scan")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"FOREIGN-QR"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("QR code not found"));

        assertThat(qrScanRepository.findAll()).isEmpty();
    }

    private void createQrCode(String code, String rewardType, String payloadJson, String policy, Integer scanLimit) throws Exception {
        String scanLimitJson = scanLimit == null ? "null" : scanLimit.toString();
        mockMvc.perform(post("/api/admin/qr")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "locationPointId":null,
                                  "rewardType":"%s",
                                  "rewardPayload":%s,
                                  "scanPolicy":"%s",
                                  "scanLimit":%s
                                }
                                """.formatted(code, rewardType, payloadJson, policy, scanLimitJson)))
                .andExpect(status().isOk());
    }

    private void createQrCodeWithLocation(String code, UUID locationId) throws Exception {
        mockMvc.perform(post("/api/admin/qr")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "locationPointId":"%s",
                                  "rewardType":"NONE",
                                  "rewardPayload":{},
                                  "scanPolicy":"EVERY_PLAYER",
                                  "scanLimit":null
                                }
                                """.formatted(code, locationId)))
                .andExpect(status().isOk());
    }

    private UUID createItemTemplate() {
        return itemTemplateRepository.save(com.mos.item.entity.ItemTemplate.builder()
                .gameSessionId(GAME_SESSION_ID)
                .name("QR Reward Item")
                .description("From QR")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build()).getId();
    }

    private UUID createHiddenLocation() {
        return locationService.createLocation(
                GAME_SESSION_ID,
                new com.mos.location.dto.CreateLocationRequest(
                        "Hidden Hut", "Secret place", 60.0, 40.0, "Forest", true
                )
        ).id();
    }

    private void scanAs(String token, String code) throws Exception {
        mockMvc.perform(post("/api/qr/scan")
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
