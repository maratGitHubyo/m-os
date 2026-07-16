package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.session.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @BeforeEach
    void verifySeedPassword() {
        assertThat(passwordEncoder.matches("Kv7nR2xP", "$2a$10$EXcu/UIUKBQj7efok.4FWO6qYL7d/ImcExOyeQz/1dw17q8WCFtZW"))
                .isTrue();
    }

    @Test
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"marat","password":"Kv7nR2xP"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.nickname").value("Марат"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.session.name").value("M-OS Dev Session"));
    }

    @Test
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"marat","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtClaimsAndProtectedEndpoints() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"marat","password":"Kv7nR2xP"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = body.get("token").asText();

        assertJwtClaims(token);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("marat"))
                .andExpect(jsonPath("$.nickname").value("Марат"));

        mockMvc.perform(get("/api/session/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("M-OS Dev Session"))
                .andExpect(jsonPath("$.status").value("STARTING"));
    }

    @Test
    void loginFailsWhenNoActiveSessionConfigured() throws Exception {
        gameSessionRepository.findAll().forEach(session -> {
            session.setStatus(com.mos.session.entity.GameSessionStatus.DRAFT);
            gameSessionRepository.save(session);
        });

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"marat","password":"Kv7nR2xP"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("No ACTIVE or STARTING game session configured"));
    }

    private void assertJwtClaims(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload).contains("\"userId\":\"10000000-0000-0000-0000-000000000001\"");
        assertThat(payload).contains("\"gameSessionId\":\"20000000-0000-0000-0000-000000000001\"");
        assertThat(payload).contains("\"role\":\"ADMIN\"");
    }
}
