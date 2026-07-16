package com.mos.seed;

import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.service.ItemService;
import com.mos.location.repository.LocationPointRepository;
import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.qrcode.service.QrCodeService;
import com.mos.quest.repository.QuestRepository;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.repository.WalletRepository;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private final LocationPointRepository locationPointRepository;
    private final WalletRepository walletRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final QrCodeRepository qrCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final ItemService itemService;
    private final QrCodeService qrCodeService;
    private final QuestRepository questRepository;

    @Transactional
    public void seedDemoData() {
        log.info("Seeding M-OS party accounts, items and QR codes");

        ensureDemoSessionExists();

        User hostAdmin = ensureUser(DemoSeedConstants.HOST_ADMIN);
        ensureParticipant(hostAdmin, ParticipantRole.ADMIN);
        removeLegacyUsers();

        List<User> players = new ArrayList<>();
        for (DemoSeedConstants.SeedAccount account : DemoSeedConstants.PARTY_PLAYERS) {
            User player = ensureUser(account);
            ensureParticipant(player, ParticipantRole.PLAYER);
            ensureStartingBalance(player, hostAdmin.getId());
            players.add(player);
        }

        UUID sessionId = DemoSeedConstants.SESSION_ID;
        removeDefaultDemoItems(sessionId);
        removeDefaultDemoQuests(sessionId);
        removeDefaultDemoLocations(sessionId);
        removeLegacyDemoQr(sessionId);

        prepareFreshDemoSession();
        seedPartyItemsAndQr(sessionId);

        log.info("M-OS seed complete: {} players, {} item templates",
                players.size(),
                itemTemplateRepository.countByGameSessionId(sessionId));
    }

    private void seedPartyItemsAndQr(UUID sessionId) {
        for (PartyItemCatalog.SeedItem item : PartyItemCatalog.ITEMS) {
            if (qrCodeRepository.existsByCodeAndGameSessionId(item.qrCode(), sessionId)) {
                continue;
            }

            UUID templateId = itemTemplateRepository.findByGameSessionIdOrderByNameAsc(sessionId).stream()
                    .filter(template -> template.getName().equals(item.name()))
                    .map(template -> template.getId())
                    .findFirst()
                    .orElseGet(() -> itemService.createTemplate(sessionId, new CreateItemTemplateRequest(
                            item.name(),
                            item.description(),
                            item.imageUrl(),
                            item.rarity(),
                            true
                    )).id());

            qrCodeService.createQrCode(sessionId, new CreateQrCodeRequest(
                    item.qrCode(),
                    item.name(),
                    null,
                    QrRewardType.ITEM,
                    Map.of("itemTemplateId", templateId.toString()),
                    QrScanPolicy.FIRST_PLAYER,
                    null
            ));
        }
    }

    private void ensureDemoSessionExists() {
        if (gameSessionRepository.findById(DemoSeedConstants.SESSION_ID).isPresent()) {
            return;
        }
        gameSessionRepository.save(GameSession.builder()
                .id(DemoSeedConstants.SESSION_ID)
                .name(DemoSeedConstants.DEMO_SESSION_NAME)
                .date(LocalDate.now())
                .status(GameSessionStatus.STARTING)
                .mapImageUrl("/maps/dacha.png")
                .build());
    }

    private void prepareFreshDemoSession() {
        GameSession session = gameSessionRepository.findById(DemoSeedConstants.SESSION_ID).orElseThrow();
        session.setName(DemoSeedConstants.DEMO_SESSION_NAME);
        if (session.getMapImageUrl() == null || session.getMapImageUrl().isBlank()) {
            session.setMapImageUrl("/maps/dacha.png");
        }
        gameSessionRepository.save(session);

        gameConfigRepository.findByGameSessionId(DemoSeedConstants.SESSION_ID).ifPresent(config -> {
            config.setLeaderboardEnabled(true);
            gameConfigRepository.save(config);
        });
    }

    private void removeLegacyUsers() {
        for (String username : List.of("admin", "alice", "bob")) {
            userRepository.findByUsername(username).ifPresent(user -> {
                sessionParticipantRepository.findByUserId(user.getId())
                        .forEach(sessionParticipantRepository::delete);
                userRepository.delete(user);
            });
        }
    }

    private User ensureUser(DemoSeedConstants.SeedAccount account) {
        return userRepository.findByUsername(account.username())
                .map(existing -> {
                    existing.setPasswordHash(passwordEncoder.encode(account.password()));
                    existing.setNickname(account.nickname());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(account.username())
                        .passwordHash(passwordEncoder.encode(account.password()))
                        .nickname(account.nickname())
                        .build()));
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

    private void removeDefaultDemoItems(UUID sessionId) {
        var demoNames = List.of("Bronze Key", "Explorer Badge", "Magic Token");
        itemTemplateRepository.findByGameSessionIdOrderByNameAsc(sessionId).stream()
                .filter(template -> demoNames.contains(template.getName()))
                .forEach(itemTemplateRepository::delete);
    }

    private void removeDefaultDemoQuests(UUID sessionId) {
        var demoTitles = List.of(
                "Удивить именинника",
                "Кто первый скажет тост",
                "Подарить предмет имениннику"
        );
        questRepository.findAll().stream()
                .filter(quest -> sessionId.equals(quest.getGameSessionId()))
                .filter(quest -> demoTitles.contains(quest.getTitle()))
                .forEach(questRepository::delete);
    }

    private void removeDefaultDemoLocations(UUID sessionId) {
        var demoNames = List.of("Start Point", "Forest", "Tower", "Lake", "Hidden Cave");
        locationPointRepository.findByGameSessionIdOrderByZoneAscNameAsc(sessionId).stream()
                .filter(location -> demoNames.contains(location.getName()))
                .forEach(locationPointRepository::delete);
    }

    private void removeLegacyDemoQr(UUID sessionId) {
        for (String code : List.of("QR-COIN", "QR-ITEM", "QR-LOCATION")) {
            qrCodeRepository.findByCodeAndGameSessionId(code, sessionId)
                    .ifPresent(qrCodeRepository::delete);
        }
    }

    private void ensureStartingBalance(User player, UUID adminId) {
        UUID sessionId = DemoSeedConstants.SESSION_ID;
        if (walletRepository.findByUserIdAndGameSessionId(player.getId(), sessionId).isPresent()) {
            return;
        }
        walletService.adminCredit(player.getId(), sessionId, 100L, "Starting balance", adminId);
    }
}
