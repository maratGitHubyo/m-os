package com.mos.seed;

import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.service.ItemService;
import com.mos.location.dto.CreateLocationRequest;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.service.LocationService;
import com.mos.lore.service.LoreService;
import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.qrcode.service.QrCodeService;
import com.mos.quest.repository.QuestRepository;
import com.mos.secret.repository.PlayerSecretRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final LoreService loreService;
    private final LocationService locationService;
    private final PlayerSecretRepository playerSecretRepository;

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
        removeDefaultDemoQuests(sessionId);
        removeDefaultDemoLocations(sessionId);
        removeNonPartyItemsAndQr(sessionId);

        prepareFreshDemoSession();
        seedPartyItemsAndQr(sessionId);
        loreService.seedLore(sessionId);
        seedMapSpots(sessionId);

        log.info("M-OS seed complete: {} players, {} item templates, {} QR codes, {} locations",
                players.size(),
                itemTemplateRepository.countByGameSessionId(sessionId),
                qrCodeRepository.countByGameSessionId(sessionId),
                locationPointRepository.countByGameSessionId(sessionId));
    }

    private void seedMapSpots(UUID sessionId) {
        int created = 0;
        int updated = 0;
        for (DachaMapCatalog.MapSpot spot : DachaMapCatalog.SPOTS) {
            var existing = locationPointRepository.findByGameSessionIdAndName(sessionId, spot.name());
            if (existing.isPresent()) {
                var location = existing.get();
                boolean dirty = false;
                if (!spot.description().equals(location.getDescription())) {
                    location.setDescription(spot.description());
                    dirty = true;
                }
                if (Double.compare(location.getX(), spot.x()) != 0) {
                    location.setX(spot.x());
                    dirty = true;
                }
                if (Double.compare(location.getY(), spot.y()) != 0) {
                    location.setY(spot.y());
                    dirty = true;
                }
                if (!spot.zone().equals(location.getZone())) {
                    location.setZone(spot.zone());
                    dirty = true;
                }
                if (!Boolean.valueOf(spot.hidden()).equals(location.getHidden())) {
                    location.setHidden(spot.hidden());
                    dirty = true;
                }
                if (dirty) {
                    locationPointRepository.save(location);
                    updated++;
                }
                continue;
            }

            // Don't recreate a spot if its lore promo was already claimed
            boolean loreAlreadyClaimed = playerSecretRepository
                    .findByCodeAndGameSessionId(spot.loreCode(), sessionId)
                    .map(secret -> Boolean.TRUE.equals(secret.getUsed()))
                    .orElse(false);
            if (loreAlreadyClaimed) {
                continue;
            }
            locationService.createLocation(sessionId, new CreateLocationRequest(
                    spot.name(),
                    spot.description(),
                    spot.x(),
                    spot.y(),
                    spot.zone(),
                    spot.hidden()
            ));
            created++;
        }
        if (created > 0 || updated > 0) {
            log.info("Map spots sync: created={}, updated={}", created, updated);
        }
    }

    private void seedPartyItemsAndQr(UUID sessionId) {
        for (PartyItemCatalog.SeedItem item : PartyItemCatalog.ITEMS) {
            ItemTemplate template = itemTemplateRepository.findByGameSessionIdOrderByNameAsc(sessionId).stream()
                    .filter(existing -> existing.getName().equals(item.name()))
                    .findFirst()
                    .orElse(null);

            if (template == null) {
                template = itemTemplateRepository.findById(
                        itemService.createTemplate(sessionId, new CreateItemTemplateRequest(
                                item.name(),
                                item.description(),
                                item.imageUrl(),
                                item.rarity(),
                                true,
                                false
                        )).id()
                ).orElseThrow();
            } else {
                boolean dirty = false;
                if (!item.imageUrl().equals(template.getImageUrl())) {
                    template.setImageUrl(item.imageUrl());
                    dirty = true;
                }
                if (!item.description().equals(template.getDescription())) {
                    template.setDescription(item.description());
                    dirty = true;
                }
                if (template.getRarity() != item.rarity()) {
                    template.setRarity(item.rarity());
                    dirty = true;
                }
                if (!Boolean.TRUE.equals(template.getIsUnique())) {
                    template.setIsUnique(true);
                    dirty = true;
                }
                if (dirty) {
                    itemTemplateRepository.save(template);
                }
            }

            if (!qrCodeRepository.existsByCodeAndGameSessionId(item.qrCode(), sessionId)) {
                qrCodeService.createQrCode(sessionId, new CreateQrCodeRequest(
                        item.qrCode(),
                        item.name(),
                        null,
                        QrRewardType.ITEM,
                        Map.of("itemTemplateId", template.getId().toString()),
                        QrScanPolicy.FIRST_PLAYER,
                        null
                ));
            }
        }
    }

    private void removeNonPartyItemsAndQr(UUID sessionId) {
        Set<String> partyNames = PartyItemCatalog.ITEMS.stream()
                .map(PartyItemCatalog.SeedItem::name)
                .collect(Collectors.toSet());
        Set<String> loreNames = LoreCatalog.FRAGMENTS.stream()
                .map(LoreCatalog.LoreFragment::name)
                .collect(Collectors.toSet());
        Set<String> keepNames = new java.util.HashSet<>(partyNames);
        keepNames.addAll(loreNames);

        Set<String> partyQrCodes = PartyItemCatalog.ITEMS.stream()
                .map(PartyItemCatalog.SeedItem::qrCode)
                .collect(Collectors.toSet());
        Set<String> loreCodes = LoreCatalog.FRAGMENTS.stream()
                .map(LoreCatalog.LoreFragment::code)
                .collect(Collectors.toSet());

        List<QrCode> obsoleteQr = qrCodeRepository.findByGameSessionIdOrderByCreatedAtAsc(sessionId).stream()
                .filter(qr -> !partyQrCodes.contains(qr.getCode()) && !loreCodes.contains(qr.getCode()))
                .toList();
        if (!obsoleteQr.isEmpty()) {
            qrCodeRepository.deleteAll(obsoleteQr);
            log.info("Removed {} obsolete QR codes", obsoleteQr.size());
        }

        List<ItemTemplate> obsoleteItems = itemTemplateRepository.findByGameSessionIdOrderByNameAsc(sessionId).stream()
                .filter(template -> !keepNames.contains(template.getName())
                        && !Boolean.TRUE.equals(template.getIsLore()))
                .toList();
        if (!obsoleteItems.isEmpty()) {
            itemTemplateRepository.deleteAll(obsoleteItems);
            log.info("Removed {} obsolete item templates", obsoleteItems.size());
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
            config.setLoreRevealed(false);
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

    private void ensureStartingBalance(User player, UUID adminId) {
        UUID sessionId = DemoSeedConstants.SESSION_ID;
        if (walletRepository.findByUserIdAndGameSessionId(player.getId(), sessionId).isPresent()) {
            return;
        }
        walletService.adminCredit(player.getId(), sessionId, 100L, "Starting balance", adminId);
    }
}
