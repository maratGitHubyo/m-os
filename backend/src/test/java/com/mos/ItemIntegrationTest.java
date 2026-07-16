package com.mos;

import com.mos.support.MosIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.common.exception.ItemNotOwnedException;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemRarity;
import com.mos.item.enums.OwnershipTransferReason;
import com.mos.item.repository.ItemOwnershipHistoryRepository;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.item.service.ItemService;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MosIntegrationTest
class ItemIntegrationTest {

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
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private PlayerItemRepository playerItemRepository;

    @Autowired
    private ItemOwnershipHistoryRepository itemOwnershipHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID playerUserId;
    private String playerUsername;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        playerUsername = "player_" + UUID.randomUUID().toString().substring(0, 8);
        User player = userRepository.save(User.builder()
                .username(playerUsername)
                .passwordHash(passwordEncoder.encode("player123"))
                .nickname("Player One")
                .build());
        playerUserId = player.getId();

        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(player)
                .gameSession(gameSessionRepository.findById(GAME_SESSION_ID).orElseThrow())
                .role(ParticipantRole.PLAYER)
                .nicknameSnapshot(player.getNickname())
                .build());

        adminToken = loginAsAdmin();
    }

    @Test
    void adminCreatesItemTemplate() throws Exception {
        mockMvc.perform(post("/api/admin/items/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Magic Sword","description":"Test item","imageUrl":null,"rarity":"RARE","isUnique":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Magic Sword"))
                .andExpect(jsonPath("$.rarity").value("RARE"))
                .andExpect(jsonPath("$.isUnique").value(false));

        assertThat(itemTemplateRepository.findByGameSessionIdOrderByNameAsc(GAME_SESSION_ID)).hasSize(1);
    }

    @Test
    void adminGrantsItemAndPlayerViewsInventory() throws Exception {
        UUID templateId = createTemplate(adminToken, "Magic Sword", false);

        mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(ADMIN_USER_ID, templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.name").value("Magic Sword"));

        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].template.name").value("Magic Sword"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.ITEM_GRANT);
        assertThat(itemOwnershipHistoryRepository.findAll()).hasSize(1);
    }

    @Test
    void playerViewsSingleInventoryItem() throws Exception {
        UUID templateId = createTemplate(adminToken, "Single Item", false);
        UUID playerItemId = grantItem(adminToken, ADMIN_USER_ID, templateId);

        mockMvc.perform(get("/api/inventory/" + playerItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerItemId.toString()))
                .andExpect(jsonPath("$.template.name").value("Single Item"));
    }

    @Test
    void uniqueItemCannotBeGrantedTwice() throws Exception {
        UUID templateId = createTemplate(adminToken, "Unique Crown", true);

        grantItem(adminToken, ADMIN_USER_ID, templateId);

        mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(playerUserId, templateId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Unique item already exists in this game session"));
    }

    @Test
    void transferItemUpdatesOwnerAndHistory() {
        UUID templateId = createTemplateQuietly("Transferable Gem", false);
        UUID playerItemId = grantItemQuietly(ADMIN_USER_ID, templateId);

        var result = itemService.transferItem(playerItemId, ADMIN_USER_ID, playerUserId, GAME_SESSION_ID);

        assertThat(result.ownerId()).isEqualTo(playerUserId);

        var history = itemOwnershipHistoryRepository.findByPlayerItemIdOrderByCreatedAtAsc(playerItemId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getReason()).isEqualTo(OwnershipTransferReason.TRANSFER);
        assertThat(history.get(1).getToUserId()).isEqualTo(playerUserId);

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditAction.ITEM_TRANSFER);
    }

    @Test
    void cannotTransferItemOwnedByAnotherUser() {
        UUID templateId = createTemplateQuietly("Locked Item", false);
        UUID playerItemId = grantItemQuietly(ADMIN_USER_ID, templateId);

        assertThatThrownBy(() -> itemService.transferItem(
                playerItemId,
                playerUserId,
                ADMIN_USER_ID,
                GAME_SESSION_ID
        )).isInstanceOf(ItemNotOwnedException.class);
    }

    @Test
    void itemsFromDifferentGameSessionAreIsolated() throws Exception {
        GameSession otherSession = gameSessionRepository.save(GameSession.builder()
                .name("Other Session")
                .date(LocalDate.now())
                .status(GameSessionStatus.DRAFT)
                .build());

        ItemTemplate otherTemplate = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(otherSession.getId())
                .name("Other Session Item")
                .description("Foreign")
                .rarity(ItemRarity.COMMON)
                .isUnique(false)
                .build());

        mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(ADMIN_USER_ID, otherTemplate.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Item belongs to a different game session"));

        assertThat(playerItemRepository.findAll()).isEmpty();
    }

    private UUID createTemplate(String token, String name, boolean isUnique) throws Exception {
        var result = mockMvc.perform(post("/api/admin/items/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"Test item","imageUrl":null,"rarity":"RARE","isUnique":%s}
                                """.formatted(name, isUnique)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createTemplateQuietly(String name, boolean isUnique) {
        return itemService.createTemplate(
                GAME_SESSION_ID,
                new com.mos.item.dto.CreateItemTemplateRequest(name, "Test item", null, ItemRarity.RARE, isUnique)
        ).id();
    }

    private UUID grantItem(String token, UUID userId, UUID templateId) throws Exception {
        var result = mockMvc.perform(post("/api/admin/items/grant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","itemTemplateId":"%s"}
                                """.formatted(userId, templateId)))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID grantItemQuietly(UUID userId, UUID templateId) {
        return itemService.grantItem(userId, templateId, GAME_SESSION_ID, ADMIN_USER_ID).id();
    }

    private String loginAsAdmin() throws Exception {
        return loginAs("Kv7nR2xP", "marat");
    }

    private String loginAs(String password, String username) throws Exception {
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
