package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.common.exception.LocationSessionMismatchException;
import com.mos.location.entity.LocationPoint;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.repository.PlayerLocationDiscoveryRepository;
import com.mos.location.service.LocationService;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class LocationIntegrationTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID GAME_SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationPointRepository locationPointRepository;

    @Autowired
    private PlayerLocationDiscoveryRepository playerLocationDiscoveryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private com.mos.session.repository.GameSessionRepository gameSessionRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAsAdmin();
    }

    @Test
    void adminCreatesLocationPoint() throws Exception {
        mockMvc.perform(post("/api/admin/locations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Old Oak",
                                  "description":"A large oak tree",
                                  "x":25.5,
                                  "y":40.0,
                                  "zone":"North Garden",
                                  "hidden":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Old Oak"))
                .andExpect(jsonPath("$.zone").value("North Garden"))
                .andExpect(jsonPath("$.gameSessionId").value(GAME_SESSION_ID.toString()));

        assertThat(locationPointRepository.findAll()).hasSize(1);
    }

    @Test
    void playerGetsMapWithLocations() throws Exception {
        UUID visibleId = createLocation(adminToken, "Visible Pond", false, 10.0, 20.0, "Lake");
        createLocation(adminToken, "Hidden Cave", true, 80.0, 70.0, "Forest");

        mockMvc.perform(get("/api/locations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].zone").exists())
                .andExpect(jsonPath("$[?(@.id=='" + visibleId + "')].name").value("Visible Pond"));
    }

    @Test
    void hiddenLocationIsUnknownBeforeDiscovery() throws Exception {
        UUID hiddenId = createLocation(adminToken, "Secret Hut", true, 55.0, 45.0, "Deep Woods");

        mockMvc.perform(get("/api/locations/" + hiddenId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zone").value("Deep Woods"))
                .andExpect(jsonPath("$.x").value(55.0))
                .andExpect(jsonPath("$.y").value(45.0))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.discovered").value(false))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void discoveryRevealsHiddenLocation() {
        UUID hiddenId = createLocationQuietly("Secret Hut", true, 55.0, 45.0, "Deep Woods");

        var response = locationService.discoverLocation(ADMIN_USER_ID, hiddenId, GAME_SESSION_ID);

        assertThat(response.discovered()).isTrue();
        assertThat(response.name()).isEqualTo("Secret Hut");
        assertThat(response.description()).isEqualTo("Test location");
        assertThat(playerLocationDiscoveryRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.LOCATION_DISCOVER);
    }

    @Test
    void repeatedDiscoveryIsIdempotent() {
        UUID hiddenId = createLocationQuietly("Secret Hut", true, 55.0, 45.0, "Deep Woods");

        locationService.discoverLocation(ADMIN_USER_ID, hiddenId, GAME_SESSION_ID);
        locationService.discoverLocation(ADMIN_USER_ID, hiddenId, GAME_SESSION_ID);

        assertThat(playerLocationDiscoveryRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll())
                .filteredOn(log -> log.getAction() == AuditAction.LOCATION_DISCOVER)
                .hasSize(1);
    }

    @Test
    void locationsFromDifferentGameSessionAreIsolated() {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        LocationPoint otherLocation = locationPointRepository.save(LocationPoint.builder()
                .gameSessionId(otherSession.getId())
                .name("Foreign Point")
                .description("Outside session")
                .x(50.0)
                .y(50.0)
                .zone("Elsewhere")
                .hidden(false)
                .build());

        assertThatThrownBy(() -> locationService.discoverLocation(
                ADMIN_USER_ID,
                otherLocation.getId(),
                GAME_SESSION_ID
        )).isInstanceOf(LocationSessionMismatchException.class);

        assertThat(playerLocationDiscoveryRepository.findAll()).isEmpty();
    }

    @Test
    void adminCanUpdateLocation() throws Exception {
        UUID locationId = createLocation(adminToken, "Bench", false, 30.0, 30.0, "Park");

        mockMvc.perform(patch("/api/admin/locations/" + locationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated Bench","hidden":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Bench"))
                .andExpect(jsonPath("$.hidden").value(true));
    }

    private UUID createLocation(
            String token,
            String name,
            boolean hidden,
            double x,
            double y,
            String zone
    ) throws Exception {
        var result = mockMvc.perform(post("/api/admin/locations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "description":"Test location",
                                  "x":%s,
                                  "y":%s,
                                  "zone":"%s",
                                  "hidden":%s
                                }
                                """.formatted(name, x, y, zone, hidden)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createLocationQuietly(String name, boolean hidden, double x, double y, String zone) {
        return locationService.createLocation(
                GAME_SESSION_ID,
                new com.mos.location.dto.CreateLocationRequest(name, "Test location", x, y, zone, hidden)
        ).id();
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
