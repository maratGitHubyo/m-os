package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.auction.dto.AuctionBroadcastMessage;
import com.mos.auction.enums.AuctionLotStatus;
import com.mos.auction.repository.AuctionBidRepository;
import com.mos.auction.repository.AuctionLotRepository;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.support.MosIntegrationTest;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class AuctionIntegrationTest {

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
    private AuctionLotRepository auctionLotRepository;

    @Autowired
    private AuctionBidRepository auctionBidRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private UUID bidderUserId;
    private String adminToken;
    private String bidderToken;

    @BeforeEach
    void setUp() throws Exception {
        gameConfigRepository.findByGameSessionId(GAME_SESSION_ID).ifPresent(config -> {
            config.setAuctionModeEnabled(false);
            gameConfigRepository.save(config);
        });

        User bidder = createPlayer("bidder_" + UUID.randomUUID().toString().substring(0, 8), "Bidder");
        bidderUserId = bidder.getId();
        addParticipant(bidder, ParticipantRole.PLAYER);

        adminToken = login("marat", "Kv7nR2xP");
        bidderToken = login(bidder.getUsername(), "player123");
        creditWallet(adminToken, bidderUserId, 500);
    }

    @Test
    void playerCannotSeeAuctionUntilModeEnabled() throws Exception {
        mockMvc.perform(get("/api/auction")
                        .header("Authorization", "Bearer " + bidderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Auction is not open yet"));
    }

    @Test
    void transfersAreFrozenWhenAuctionModeEnabled() throws Exception {
        User receiver = createPlayer("recv_" + UUID.randomUUID().toString().substring(0, 8), "Receiver");
        addParticipant(receiver, ParticipantRole.PLAYER);

        enableAuctionMode();

        mockMvc.perform(post("/api/wallet/transfer")
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverUserId":"%s","amount":10}
                                """.formatted(receiver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Transfers and trades are disabled while the auction is active"));
    }

    @Test
    void fullAuctionFlowBurnsCoinsOnSell() throws Exception {
        enableAuctionMode();

        MvcResult createResult = mockMvc.perform(post("/api/admin/auction/lots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Mystery Box","startingPrice":50,"minBidIncrement":50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("Mystery Box"))
                .andReturn();

        UUID lotId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(post("/api/admin/auction/lots/" + lotId + "/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.nextMinBid").value(50));

        mockMvc.perform(post("/api/auction/lots/" + lotId + "/bids")
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(100))
                .andExpect(jsonPath("$.currentLeaderId").value(bidderUserId.toString()))
                .andExpect(jsonPath("$.nextMinBid").value(150));

        mockMvc.perform(post("/api/admin/auction/lots/" + lotId + "/sell")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.finalPrice").value(100))
                .andExpect(jsonPath("$.winnerUserId").value(bidderUserId.toString()));

        assertThat(walletRepository.findByUserIdAndGameSessionId(bidderUserId, GAME_SESSION_ID))
                .isPresent()
                .get()
                .extracting(wallet -> wallet.getBalance())
                .isEqualTo(400L);

        assertThat(coinTransactionRepository.findAll()).anySatisfy(tx -> {
            assertThat(tx.getUserId()).isEqualTo(bidderUserId);
            assertThat(tx.getType()).isEqualTo(CoinTransactionType.AUCTION);
            assertThat(tx.getAmount()).isEqualTo(-100L);
            assertThat(tx.getReferenceId()).isEqualTo(lotId.toString());
        });

        assertThat(auctionLotRepository.findById(lotId))
                .isPresent()
                .get()
                .extracting(lot -> lot.getStatus())
                .isEqualTo(AuctionLotStatus.SOLD);

        assertThat(auctionBidRepository.findByLotIdOrderByCreatedAtDesc(lotId)).hasSize(1);

        assertThat(auditLogRepository.findAll()).anyMatch(log ->
                log.getAction() == AuditAction.AUCTION_LOT_SELL);

        ArgumentCaptor<AuctionBroadcastMessage> captor = ArgumentCaptor.forClass(AuctionBroadcastMessage.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/session/" + GAME_SESSION_ID + "/auction"),
                captor.capture()
        );
        assertThat(captor.getAllValues()).anyMatch(msg -> "BID".equals(msg.type()));
        assertThat(captor.getAllValues()).anyMatch(msg -> "LOT_SOLD".equals(msg.type()));
    }

    @Test
    void bidBelowMinimumIsRejected() throws Exception {
        enableAuctionMode();

        MvcResult createResult = mockMvc.perform(post("/api/admin/auction/lots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Low Bid Lot","startingPrice":100,"minBidIncrement":50}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID lotId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(post("/api/admin/auction/lots/" + lotId + "/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auction/lots/" + lotId + "/bids")
                        .header("Authorization", "Bearer " + bidderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":50}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bid amount is too low"));
    }

    private void enableAuctionMode() throws Exception {
        mockMvc.perform(post("/api/admin/auction/mode")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionModeEnabled").value(true));
    }

    private void creditWallet(String token, UUID userId, long amount) throws Exception {
        mockMvc.perform(post("/api/admin/wallet/" + userId + "/credit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d,"description":"test credit"}
                                """.formatted(amount)))
                .andExpect(status().isOk());
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
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
