package com.mos.lore.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.SessionConfigurationException;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.entity.PlayerItem;
import com.mos.item.enums.ItemRarity;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.service.ItemService;
import com.mos.location.entity.LocationPoint;
import com.mos.location.repository.LocationPointRepository;
import com.mos.lore.dto.LoreFragmentStatsEntry;
import com.mos.lore.dto.LoreRevealResponse;
import com.mos.lore.dto.LoreSeedResponse;
import com.mos.lore.dto.LoreStatsResponse;
import com.mos.secret.dto.CreateSharedSecretRequest;
import com.mos.secret.entity.PlayerSecret;
import com.mos.secret.enums.SecretRewardType;
import com.mos.secret.repository.PlayerSecretRepository;
import com.mos.secret.service.PlayerSecretService;
import com.mos.seed.LoreCatalog;
import com.mos.session.entity.GameConfig;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoreService {

    private static final Pattern PROMO_CODE_PATTERN = Pattern.compile("Промокод:\\s*\\S+", Pattern.CASE_INSENSITIVE);

    private final GameConfigRepository gameConfigRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final PlayerSecretRepository playerSecretRepository;
    private final LocationPointRepository locationPointRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final PlayerSecretService playerSecretService;
    private final ItemService itemService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public LoreStatsResponse getStats(UUID gameSessionId) {
        boolean revealed = itemService.isLoreRevealed(gameSessionId);

        Map<String, ItemTemplate> templatesByName = itemTemplateRepository
                .findByGameSessionIdOrderByNameAsc(gameSessionId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsLore()))
                .collect(Collectors.toMap(ItemTemplate::getName, t -> t, (a, b) -> a));

        Map<UUID, PlayerItem> ownedByTemplateId = itemService.findLoreItemsBySession(gameSessionId).stream()
                .collect(Collectors.toMap(pi -> pi.getItemTemplate().getId(), pi -> pi, (a, b) -> a));

        Map<UUID, String> nicknames = sessionParticipantRepository
                .findByGameSessionIdAndRole(gameSessionId, ParticipantRole.PLAYER).stream()
                .collect(Collectors.toMap(
                        p -> p.getUser().getId(),
                        SessionParticipant::getNicknameSnapshot,
                        (a, b) -> a
                ));

        Map<String, String> codeByTemplateId = new HashMap<>();
        for (PlayerSecret secret : playerSecretRepository.findByGameSessionIdAndIsSharedTrueOrderByCodeAsc(gameSessionId)) {
            if (secret.getRewardType() != SecretRewardType.ITEM) {
                continue;
            }
            Object templateIdValue = secret.getRewardPayload() != null
                    ? secret.getRewardPayload().get("itemTemplateId")
                    : null;
            if (templateIdValue != null) {
                codeByTemplateId.put(templateIdValue.toString(), secret.getCode());
            }
        }

        List<LoreFragmentStatsEntry> fragments = new ArrayList<>();
        for (LoreCatalog.LoreFragment fragment : LoreCatalog.FRAGMENTS) {
            ItemTemplate template = templatesByName.get(fragment.name());
            if (template == null) {
                fragments.add(new LoreFragmentStatsEntry(
                        null,
                        fragment.name(),
                        fragment.code(),
                        false,
                        null,
                        null
                ));
                continue;
            }

            PlayerItem owned = ownedByTemplateId.get(template.getId());
            UUID ownerId = owned != null ? owned.getOwnerId() : null;
            fragments.add(new LoreFragmentStatsEntry(
                    template.getId(),
                    template.getName(),
                    codeByTemplateId.getOrDefault(template.getId().toString(), fragment.code()),
                    owned != null,
                    ownerId,
                    ownerId != null ? nicknames.get(ownerId) : null
            ));
        }

        int foundCount = (int) fragments.stream().filter(LoreFragmentStatsEntry::found).count();
        Set<UUID> ownersWithLore = fragments.stream()
                .filter(LoreFragmentStatsEntry::found)
                .map(LoreFragmentStatsEntry::ownerUserId)
                .collect(Collectors.toSet());

        int playerCount = nicknames.size();
        int withLore = ownersWithLore.size();

        return new LoreStatsResponse(
                revealed,
                LoreCatalog.FRAGMENTS.size(),
                foundCount,
                LoreCatalog.FRAGMENTS.size() - foundCount,
                withLore,
                Math.max(0, playerCount - withLore),
                fragments
        );
    }

    @Transactional
    public LoreRevealResponse setLoreRevealed(UUID gameSessionId, boolean revealed, UUID performedByUserId) {
        GameConfig config = gameConfigRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new SessionConfigurationException("Game config not found for session"));
        config.setLoreRevealed(revealed);
        gameConfigRepository.save(config);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.LORE_REVEAL_CHANGE,
                "GameConfig",
                config.getId().toString(),
                revealed ? "Lore revealed" : "Lore sealed",
                Map.of("loreRevealed", revealed)
        );

        return new LoreRevealResponse(revealed);
    }

    @Transactional
    public LoreSeedResponse seedLore(UUID gameSessionId) {
        int templatesCreated = 0;
        int templatesExisting = 0;
        int secretsCreated = 0;
        int secretsExisting = 0;

        Map<String, ItemTemplate> existingByName = itemTemplateRepository
                .findByGameSessionIdOrderByNameAsc(gameSessionId).stream()
                .collect(Collectors.toMap(ItemTemplate::getName, t -> t, (a, b) -> a));

        List<String> codes = new ArrayList<>();

        for (LoreCatalog.LoreFragment fragment : LoreCatalog.FRAGMENTS) {
            ItemTemplate template = existingByName.get(fragment.name());
            if (template == null) {
                template = itemTemplateRepository.save(ItemTemplate.builder()
                        .gameSessionId(gameSessionId)
                        .name(fragment.name())
                        .description(fragment.text())
                        .imageUrl(null)
                        .rarity(ItemRarity.LEGENDARY)
                        .isUnique(true)
                        .isLore(true)
                        .build());
                templatesCreated++;
            } else {
                boolean dirty = false;
                if (!fragment.text().equals(template.getDescription())) {
                    template.setDescription(fragment.text());
                    dirty = true;
                }
                if (!Boolean.TRUE.equals(template.getIsLore())) {
                    template.setIsLore(true);
                    dirty = true;
                }
                if (!Boolean.TRUE.equals(template.getIsUnique())) {
                    template.setIsUnique(true);
                    dirty = true;
                }
                if (dirty) {
                    itemTemplateRepository.save(template);
                }
                templatesExisting++;
            }

            codes.add(fragment.code());

            PlayerSecret existingSecret = findSharedSecretForTemplate(gameSessionId, template.getId());
            if (existingSecret != null) {
                if (!fragment.code().equals(existingSecret.getCode())) {
                    if (playerSecretRepository.existsByCodeAndGameSessionId(fragment.code(), gameSessionId)
                            && !existingSecret.getCode().equals(fragment.code())) {
                        // Target code already taken by another row — skip rename.
                        secretsExisting++;
                    } else {
                        existingSecret.setCode(fragment.code());
                        existingSecret.setTitle(fragment.name());
                        existingSecret.setDescription("Физический промокод фрагмента лора " + fragment.roman());
                        playerSecretRepository.save(existingSecret);
                        secretsExisting++;
                    }
                } else {
                    secretsExisting++;
                }
            } else if (playerSecretRepository.existsByCodeAndGameSessionId(fragment.code(), gameSessionId)) {
                secretsExisting++;
            } else {
                playerSecretService.createSharedSecret(gameSessionId, new CreateSharedSecretRequest(
                        fragment.code(),
                        fragment.name(),
                        "Физический промокод фрагмента лора " + fragment.roman(),
                        SecretRewardType.ITEM,
                        Map.of("itemTemplateId", template.getId().toString())
                ));
                secretsCreated++;
            }
        }

        // Keep lore sealed by default; promo codes are physical printouts, not map text.
        GameConfig config = gameConfigRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new SessionConfigurationException("Game config not found for session"));
        if (Boolean.TRUE.equals(config.getLoreRevealed())) {
            config.setLoreRevealed(false);
            gameConfigRepository.save(config);
        }

        int locationsCleaned = stripPromoCodesFromLocations(gameSessionId);

        return new LoreSeedResponse(
                false,
                templatesCreated,
                templatesExisting,
                secretsCreated,
                secretsExisting,
                locationsCleaned,
                codes
        );
    }

    /**
     * Removes "Промокод: LOR-N" lines from map locations — codes are printed and placed offline.
     */
    private int stripPromoCodesFromLocations(UUID gameSessionId) {
        int cleaned = 0;
        for (LocationPoint location : locationPointRepository.findByGameSessionIdOrderByZoneAscNameAsc(gameSessionId)) {
            String description = location.getDescription();
            if (description == null || description.isBlank()) {
                continue;
            }
            String stripped = PROMO_CODE_PATTERN.matcher(description)
                    .replaceAll("")
                    .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
                    .replaceAll("\\n{2,}", "\n")
                    .trim();
            if (!stripped.equals(description.trim())) {
                location.setDescription(stripped.isEmpty() ? location.getName() : stripped);
                locationPointRepository.save(location);
                cleaned++;
            }
        }
        return cleaned;
    }

    private PlayerSecret findSharedSecretForTemplate(UUID gameSessionId, UUID templateId) {
        String templateIdText = templateId.toString();
        for (PlayerSecret secret : playerSecretRepository.findByGameSessionIdAndIsSharedTrueOrderByCodeAsc(gameSessionId)) {
            if (secret.getRewardType() != SecretRewardType.ITEM) {
                continue;
            }
            Object value = secret.getRewardPayload() != null
                    ? secret.getRewardPayload().get("itemTemplateId")
                    : null;
            if (value != null && templateIdText.equals(value.toString())) {
                return secret;
            }
        }
        return null;
    }
}
