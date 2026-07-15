package com.mos.seed;

import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.enums.ItemRarity;
import com.mos.item.service.ItemService;
import com.mos.location.dto.CreateLocationRequest;
import com.mos.location.service.LocationService;
import com.mos.numbers.dto.CreateCollectibleNumberRequest;
import com.mos.numbers.service.NumberService;
import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
import com.mos.qrcode.service.QrCodeService;
import com.mos.quest.dto.CreateQuestRequest;
import com.mos.quest.enums.QuestCompletionPolicy;
import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
import com.mos.quest.service.QuestService;
import com.mos.secret.dto.CreatePlayerSecretRequest;
import com.mos.secret.enums.SecretRewardType;
import com.mos.secret.service.PlayerSecretService;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final ItemService itemService;
    private final LocationService locationService;
    private final QrCodeService qrCodeService;
    private final PlayerSecretService playerSecretService;
    private final NumberService numberService;
    private final QuestService questService;

    @Transactional
    public void seedDemoData() {
        if (userRepository.existsByUsername("alice")) {
            log.info("Demo data already seeded, skipping");
            return;
        }

        log.info("Seeding M-OS demo data");

        ensureSingleDemoSession();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        User alice = createPlayer("alice", "Alice");
        User bob = createPlayer("bob", "Bob");

        ensureParticipant(admin, ParticipantRole.ADMIN);
        ensureParticipant(alice, ParticipantRole.PLAYER);
        ensureParticipant(bob, ParticipantRole.PLAYER);

        UUID sessionId = DemoSeedConstants.SESSION_ID;
        UUID adminId = admin.getId();

        walletService.adminCredit(alice.getId(), sessionId, 100L, "Demo starting balance", adminId);
        walletService.adminCredit(bob.getId(), sessionId, 100L, "Demo starting balance", adminId);

        UUID bronzeKeyId = createItemTemplate(sessionId, "Bronze Key", ItemRarity.COMMON);
        UUID explorerBadgeId = createItemTemplate(sessionId, "Explorer Badge", ItemRarity.RARE);
        UUID magicTokenId = createItemTemplate(sessionId, "Magic Token", ItemRarity.EPIC);

        itemService.grantItem(alice.getId(), bronzeKeyId, sessionId, adminId);
        itemService.grantItem(bob.getId(), explorerBadgeId, sessionId, adminId);

        createLocation(sessionId, "Start Point", "Where the adventure begins", 10.0, 10.0, "village", false);
        createLocation(sessionId, "Forest", "A dense forest path", 30.0, 25.0, "north", false);
        createLocation(sessionId, "Tower", "An old stone tower", 55.0, 20.0, "east", false);
        createLocation(sessionId, "Lake", "A calm lakeside", 40.0, 70.0, "south", false);
        UUID hiddenCaveId = createLocation(sessionId, "Hidden Cave", "A secret cave entrance", 80.0, 80.0, "west", true);

        createQrCode(sessionId, "QR-COIN", null, QrRewardType.COIN, Map.of("amount", 50), QrScanPolicy.EVERY_PLAYER, null);
        createQrCode(sessionId, "QR-ITEM", null, QrRewardType.ITEM, Map.of("itemTemplateId", magicTokenId.toString()), QrScanPolicy.EVERY_PLAYER, null);
        createQrCode(sessionId, "QR-LOCATION", hiddenCaveId, QrRewardType.NONE, Map.of(), QrScanPolicy.EVERY_PLAYER, null);

        playerSecretService.createSecret(sessionId, new CreatePlayerSecretRequest(
                alice.getId(),
                "STAR-DEMO",
                "Demo Star Secret",
                "A secret reward for the demo",
                SecretRewardType.COIN,
                Map.of("amount", 25)
        ));

        for (int value = 1; value <= 5; value++) {
            numberService.createNumber(sessionId, new CreateCollectibleNumberRequest(value));
        }

        questService.createQuest(sessionId, new CreateQuestRequest(
                "Удивить именинника",
                "Сделай что-то приятное и неожиданное для именинника. Честное слово.",
                QuestType.SOCIAL,
                Map.of(),
                Map.of("type", "COIN", "amount", 40),
                QuestDefinitionStatus.ACTIVE,
                QuestCompletionPolicy.EVERY_PLAYER,
                null,
                null
        ), adminId);

        questService.createQuest(sessionId, new CreateQuestRequest(
                "Кто первый скажет тост",
                "Придумай и произнеси короткий тост. Награда — первому успевшему.",
                QuestType.SOCIAL,
                Map.of(),
                Map.of("type", "COIN", "amount", 60),
                QuestDefinitionStatus.ACTIVE,
                QuestCompletionPolicy.LIMITED,
                1,
                null
        ), adminId);

        questService.createQuest(sessionId, new CreateQuestRequest(
                "Подарить предмет имениннику",
                "Передай имениннику любой предмет (в жизни или через обмен в M-OS).",
                QuestType.SOCIAL,
                Map.of(),
                Map.of("type", "COIN", "amount", 30),
                QuestDefinitionStatus.ACTIVE,
                QuestCompletionPolicy.EVERY_PLAYER,
                null,
                null
        ), adminId);

        log.info("M-OS demo data seeded for session {}", sessionId);
    }

    private void ensureSingleDemoSession() {
        GameSession session = gameSessionRepository.findById(DemoSeedConstants.SESSION_ID)
                .orElseGet(() -> gameSessionRepository.save(GameSession.builder()
                        .id(DemoSeedConstants.SESSION_ID)
                        .name(DemoSeedConstants.DEMO_SESSION_NAME)
                        .date(LocalDate.now())
                        .status(GameSessionStatus.STARTING)
                        .build()));

        session.setName(DemoSeedConstants.DEMO_SESSION_NAME);
        session.setStatus(GameSessionStatus.STARTING);
        session.setMapImageUrl("/maps/dacha.png");
        gameSessionRepository.save(session);

        gameSessionRepository.findByStatusIn(List.of(GameSessionStatus.ACTIVE, GameSessionStatus.STARTING)).stream()
                .filter(other -> !other.getId().equals(DemoSeedConstants.SESSION_ID))
                .forEach(other -> {
                    other.setStatus(GameSessionStatus.FINISHED);
                    gameSessionRepository.save(other);
                });

        gameConfigRepository.findByGameSessionId(DemoSeedConstants.SESSION_ID).ifPresent(config -> {
            config.setNumbersTotal(5);
            config.setLeaderboardEnabled(true);
            gameConfigRepository.save(config);
        });
    }

    private User createPlayer(String username, String nickname) {
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(DemoSeedConstants.DEMO_PLAYER_PASSWORD))
                .nickname(nickname)
                .build());
    }

    private void ensureParticipant(User user, ParticipantRole role) {
        if (sessionParticipantRepository.findByUserIdAndGameSessionId(user.getId(), DemoSeedConstants.SESSION_ID).isPresent()) {
            return;
        }

        GameSession session = gameSessionRepository.findById(DemoSeedConstants.SESSION_ID).orElseThrow();
        sessionParticipantRepository.save(SessionParticipant.builder()
                .user(user)
                .gameSession(session)
                .role(role)
                .nicknameSnapshot(user.getNickname())
                .build());
    }

    private UUID createItemTemplate(UUID sessionId, String name, ItemRarity rarity) {
        return itemService.createTemplate(sessionId, new CreateItemTemplateRequest(
                name,
                "Demo item: " + name,
                null,
                rarity,
                false
        )).id();
    }

    private UUID createLocation(
            UUID sessionId,
            String name,
            String description,
            double x,
            double y,
            String zone,
            boolean hidden
    ) {
        return locationService.createLocation(sessionId, new CreateLocationRequest(
                name,
                description,
                x,
                y,
                zone,
                hidden
        )).id();
    }

    private void createQrCode(
            UUID sessionId,
            String code,
            UUID locationPointId,
            QrRewardType rewardType,
            Map<String, Object> payload,
            QrScanPolicy scanPolicy,
            Integer scanLimit
    ) {
        qrCodeService.createQrCode(sessionId, new CreateQrCodeRequest(
                code,
                locationPointId,
                rewardType,
                payload,
                scanPolicy,
                scanLimit
        ));
    }
}
