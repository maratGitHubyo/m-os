package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import com.mos.support.MosIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class WalletIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void getMyWalletCreatesEmptyWallet() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/wallet/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.userId").value(ADMIN_USER_ID.toString()))
                .andExpect(jsonPath("$.gameSessionId").value(GAME_SESSION_ID.toString()));

        assertThat(walletRepository.findByUserIdAndGameSessionId(ADMIN_USER_ID, GAME_SESSION_ID)).isPresent();
    }

    @Test
    void adminCreditUpdatesBalanceCreatesTransactionAndAuditLog() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/wallet/" + ADMIN_USER_ID + "/credit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":500,"description":"Birthday bonus"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.type").value("ADMIN"));

        mockMvc.perform(get("/api/wallet/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500));

        assertThat(coinTransactionRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.COIN_CREDIT
                        && log.getEntityType().equals("Wallet")
                        && log.getDescription().equals("Birthday bonus"));
    }

    @Test
    void adminDebitUpdatesBalance() throws Exception {
        String token = loginAsAdmin();
        credit(token, 1000);

        mockMvc.perform(post("/api/admin/wallet/" + ADMIN_USER_ID + "/debit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":300,"description":"Penalty"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(-300));

        mockMvc.perform(get("/api/wallet/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.COIN_DEBIT);
    }

    @Test
    void adminDebitFailsWhenInsufficientBalance() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/wallet/" + ADMIN_USER_ID + "/debit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"description":"Penalty"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient M-coin balance"));
    }

    @Test
    void getMyTransactionsReturnsHistory() throws Exception {
        String token = loginAsAdmin();
        credit(token, 250);

        mockMvc.perform(get("/api/wallet/me/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(250))
                .andExpect(jsonPath("$.content[0].type").value("ADMIN"));
    }

    @Test
    void coinLeaderboardSortedByBalance() throws Exception {
        String token = loginAsAdmin();
        credit(token, 250);

        mockMvc.perform(get("/api/leaderboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(ADMIN_USER_ID.toString()))
                .andExpect(jsonPath("$[0].balance").value(250))
                .andExpect(jsonPath("$[0].rank").value(1));
    }

    @Test
    void walletEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/wallet/me"))
                .andExpect(status().isUnauthorized());
    }

    private void credit(String token, long amount) throws Exception {
        mockMvc.perform(post("/api/admin/wallet/" + ADMIN_USER_ID + "/credit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":%d,"description":"Setup credit"}
                                """.formatted(amount)))
                .andExpect(status().isOk());
    }

    private String loginAsAdmin() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
