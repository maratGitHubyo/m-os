package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.common.audit.service.AuditLogEntry;
import com.mos.common.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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
class AuditLogIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAuditLogViaService() {
        var saved = auditService.log(new AuditLogEntry(
                ADMIN_USER_ID,
                GAME_SESSION_ID,
                AuditAction.ADMIN_ACTION,
                "AuditLog",
                null,
                "Infrastructure audit log test",
                Map.of("source", "test")
        ));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(ADMIN_USER_ID);
        assertThat(saved.getGameSessionId()).isEqualTo(GAME_SESSION_ID);
        assertThat(saved.getAction()).isEqualTo(AuditAction.ADMIN_ACTION);
        assertThat(saved.getMetadata()).containsEntry("source", "test");

        var fromDb = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(fromDb.getDescription()).isEqualTo("Infrastructure audit log test");
    }

    @Test
    void readAuditLogViaServiceAndAdminApi() throws Exception {
        auditService.log(
                ADMIN_USER_ID,
                GAME_SESSION_ID,
                AuditAction.ADMIN_ACTION,
                "System",
                "setup",
                "Seed audit entry for read test"
        );

        var page = auditService.findLogs(GAME_SESSION_ID, null, null, org.springframework.data.domain.Pageable.unpaged());
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().getFirst().description()).isEqualTo("Seed audit entry for read test");

        String token = loginAsAdmin();

        mockMvc.perform(get("/api/admin/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Seed audit entry for read test"))
                .andExpect(jsonPath("$.content[0].action").value("ADMIN_ACTION"));
    }

    @Test
    void adminAuditEndpointRequiresAdminRole() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/admin/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminAuditEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isUnauthorized());
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
