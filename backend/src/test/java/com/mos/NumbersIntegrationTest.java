package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.numbers.repository.PlayerNumberRepository;
import org.junit.jupiter.api.BeforeEach;
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
class NumbersIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollectibleNumberRepository collectibleNumberRepository;

    @Autowired
    private PlayerNumberRepository playerNumberRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        playerNumberRepository.deleteAll();
        collectibleNumberRepository.deleteAll();
        adminToken = login("admin", "admin123");
    }

    private int uniqueNumberValue() {
        return 10_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900_000);
    }

    @Test
    void adminCreatesCollectibleNumber() throws Exception {
        int numberValue = uniqueNumberValue();
        mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + numberValue + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberValue").value(numberValue))
                .andExpect(jsonPath("$.gameSessionId").value(GAME_SESSION_ID.toString()));

        assertThat(collectibleNumberRepository.findAll()).hasSize(1);
    }

    @Test
    void duplicateNumberValueRejected() throws Exception {
        int numberValue = uniqueNumberValue();
        mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + numberValue + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + numberValue + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminGrantsNumberToPlayer() throws Exception {
        int numberValue = uniqueNumberValue();
        UUID numberId = createNumber(numberValue);

        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberValue").value(numberValue))
                .andExpect(jsonPath("$.userId").value(ADMIN_USER_ID.toString()));

        assertThat(playerNumberRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.NUMBER_GRANT);
    }

    @Test
    void grantNumberIsIdempotent() throws Exception {
        UUID numberId = createNumber(uniqueNumberValue());

        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(playerNumberRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().stream().filter(log -> log.getAction() == AuditAction.NUMBER_GRANT))
                .hasSize(1);
    }

    @Test
    void playerGetsMyNumbers() throws Exception {
        int numberValue = uniqueNumberValue();
        UUID numberId = createNumber(numberValue);
        grantNumber(numberId);

        mockMvc.perform(get("/api/numbers/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numberValue").value(numberValue))
                .andExpect(jsonPath("$[0].userId").value(ADMIN_USER_ID.toString()));
    }

    @Test
    void playerGetsCollectionProgress() throws Exception {
        UUID firstNumberId = createNumber(uniqueNumberValue());
        UUID secondNumberId = createNumber(uniqueNumberValue());
        grantNumber(firstNumberId);

        mockMvc.perform(get("/api/numbers/progress")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(1))
                .andExpect(jsonPath("$.total").value(50));

        grantNumber(secondNumberId);

        mockMvc.perform(get("/api/numbers/progress")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collected").value(2));
    }

    @Test
    void grantNumberFromAnotherSessionRejected() throws Exception {
        UUID numberId = createNumber(uniqueNumberValue());

        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    private UUID createNumber(int value) throws Exception {
        var result = mockMvc.perform(post("/api/admin/numbers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numberValue\":" + value + "}"))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText()
        );
    }

    private void grantNumber(UUID numberId) throws Exception {
        mockMvc.perform(post("/api/admin/numbers/" + numberId + "/grant/" + ADMIN_USER_ID)
                        .header("Authorization", "Bearer " + adminToken))
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
