package com.mos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.support.MosIntegrationTest;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class PlayerIntegrationTest {

    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private SessionParticipantRepository sessionParticipantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void listSessionPlayersReturnsOnlyPlayers() throws Exception {
        User player = userRepository.save(User.builder()
                .username("list_player")
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("List Player")
                .build());
        GameSession session = gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow();
        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(player)
                .gameSession(session)
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(player.getNickname())
                .build());

        String token = loginAsAdmin();

        mockMvc.perform(get("/api/players")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(player.getId().toString()))
                .andExpect(jsonPath("$[0].nickname").value("List Player"))
                .andExpect(jsonPath("$[?(@.nickname=='Марат')]").isEmpty());
    }

    @Test
    void listSessionPlayersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isUnauthorized());
    }

    private String loginAsAdmin() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"marat","password":"Kv7nR2xP"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
