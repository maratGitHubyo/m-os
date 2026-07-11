package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.support.MosIntegrationTest;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import com.mos.wallet.transfer.repository.CoinTransferRepository;
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
class CoinTransferIntegrationTest {

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
    private WalletRepository walletRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private CoinTransferRepository coinTransferRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID senderUserId;
    private UUID receiverUserId;
    private String senderToken;
    private String receiverToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        User sender = createPlayer("sender_" + UUID.randomUUID().toString().substring(0, 8), "Sender");
        User receiver = createPlayer("receiver_" + UUID.randomUUID().toString().substring(0, 8), "Receiver");
        senderUserId = sender.getId();
        receiverUserId = receiver.getId();

        addParticipant(sender, ParticipantRole.PLAYER);
        addParticipant(receiver, ParticipantRole.PLAYER);

        adminToken = login("admin", "admin123");
        senderToken = login(sender.getUsername(), "player123");
        receiverToken = login(receiver.getUsername(), "player123");

        creditWallet(adminToken, senderUserId, 500);
    }

    @Test
    void playerTransfersCoinsSuccessfully() throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":120}
                                """.formatted(receiverUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(120))
                .andExpect(jsonPath("$.senderUserId").value(senderUserId.toString()))
                .andExpect(jsonPath("$.receiverUserId").value(receiverUserId.toString()));

        assertThat(walletRepository.findByUserIdAndGameSessionId(senderUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(380);
        assertThat(walletRepository.findByUserIdAndGameSessionId(receiverUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(120);

        assertThat(coinTransferRepository.findAll()).hasSize(1);
        assertThat(coinTransactionRepository.findAll()).hasSize(3);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.COIN_TRANSFER
                        && log.getEntityType().equals("CoinTransfer"));
    }

    @Test
    void cannotTransferToSelf() throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":50}
                                """.formatted(senderUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transfer coins to yourself"));

        assertThat(coinTransferRepository.findAll()).isEmpty();
    }

    @Test
    void cannotTransferNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":0}
                                """.formatted(receiverUserId)))
                .andExpect(status().isBadRequest());

        assertThat(coinTransferRepository.findAll()).isEmpty();
    }

    @Test
    void cannotTransferWithoutBalance() throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":1000}
                                """.formatted(receiverUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient M-coin balance"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(senderUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(500);
        assertThat(coinTransferRepository.findAll()).isEmpty();
    }

    @Test
    void cannotTransferToPlayerFromAnotherSession() throws Exception {
        User foreignUser = createPlayer("foreign_" + UUID.randomUUID().toString().substring(0, 8), "Foreign");
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Foreign Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());
        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(foreignUser)
                .gameSession(otherSession)
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(foreignUser.getNickname())
                .build());

        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":50}
                                """.formatted(foreignUser.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User is not a participant of this game session"));

        assertThat(walletRepository.findByUserIdAndGameSessionId(senderUserId, GAME_SESSION_ID).orElseThrow().getBalance())
                .isEqualTo(500);
        assertThat(coinTransferRepository.findAll()).isEmpty();
    }

    @Test
    void transferHistoryReturnsIncomingAndOutgoing() throws Exception {
        transfer(senderToken, receiverUserId, 80);
        transfer(receiverToken, senderUserId, 30);

        mockMvc.perform(get("/api/wallet/transfers")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(30))
                .andExpect(jsonPath("$.content[1].amount").value(80));

        mockMvc.perform(get("/api/wallet/transfers")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void transactionIsAtomic() throws Exception {
        long senderBalanceBefore = walletRepository
                .findByUserIdAndGameSessionId(senderUserId, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L);
        long receiverBalanceBefore = walletRepository
                .findByUserIdAndGameSessionId(receiverUserId, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L);
        int transfersBefore = coinTransferRepository.findAll().size();
        int transactionsBefore = coinTransactionRepository.findAll().size();

        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":9999}
                                """.formatted(receiverUserId)))
                .andExpect(status().isBadRequest());

        assertThat(walletRepository.findByUserIdAndGameSessionId(senderUserId, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L))
                .isEqualTo(senderBalanceBefore);
        assertThat(walletRepository.findByUserIdAndGameSessionId(receiverUserId, GAME_SESSION_ID)
                .map(wallet -> wallet.getBalance())
                .orElse(0L))
                .isEqualTo(receiverBalanceBefore);
        assertThat(coinTransferRepository.findAll()).hasSize(transfersBefore);
        assertThat(coinTransactionRepository.findAll()).hasSize(transactionsBefore);
    }

    @Test
    void unauthenticatedCannotTransfer() throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":50}
                                """.formatted(receiverUserId)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/wallet/transfers"))
                .andExpect(status().isUnauthorized());
    }

    private User createPlayer(String username, String nickname) {
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname(nickname)
                .build());
    }

    private void addParticipant(User user, ParticipantRole role) {
        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(user)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(role)
                .nicknameSnapshot(user.getNickname())
                .build());
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

    private void creditWallet(String token, UUID userId, long amount) throws Exception {
        mockMvc.perform(post("/api/admin/wallet/" + userId + "/credit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d,"description":"Setup credit"}
                                """.formatted(amount)))
                .andExpect(status().isOk());
    }

    private void transfer(String token, UUID receiverId, long amount) throws Exception {
        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":%d}
                                """.formatted(receiverId, amount)))
                .andExpect(status().isOk());
    }
}
