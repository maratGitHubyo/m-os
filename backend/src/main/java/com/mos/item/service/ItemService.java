package com.mos.item.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.ItemNotFoundException;
import com.mos.common.exception.ItemNotOwnedException;
import com.mos.common.exception.ItemSessionMismatchException;
import com.mos.common.exception.LoreFragmentAlreadyFoundException;
import com.mos.common.exception.PlayerAlreadyHasLoreItemException;
import com.mos.common.exception.UniqueItemAlreadyExistsException;
import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.dto.ItemCollectionStatsEntry;
import com.mos.item.dto.ItemCollectionStatsResponse;
import com.mos.item.dto.ItemCollectionTotals;
import com.mos.item.dto.ItemOwnershipHistoryResponse;
import com.mos.item.dto.ItemTemplateResponse;
import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.entity.ItemOwnershipHistory;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.entity.PlayerItem;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.enums.ItemRarity;
import com.mos.item.enums.OwnershipTransferReason;
import com.mos.item.repository.ItemOwnershipHistoryRepository;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.quest.service.QuestService;
import com.mos.session.entity.ParticipantRole;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ItemService {

    private final ItemTemplateRepository itemTemplateRepository;
    private final PlayerItemRepository playerItemRepository;
    private final ItemOwnershipHistoryRepository itemOwnershipHistoryRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AuditService auditService;
    private final QuestService questService;

    public ItemService(
            ItemTemplateRepository itemTemplateRepository,
            PlayerItemRepository playerItemRepository,
            ItemOwnershipHistoryRepository itemOwnershipHistoryRepository,
            SessionParticipantRepository sessionParticipantRepository,
            GameConfigRepository gameConfigRepository,
            AuditService auditService,
            @Lazy QuestService questService
    ) {
        this.itemTemplateRepository = itemTemplateRepository;
        this.playerItemRepository = playerItemRepository;
        this.itemOwnershipHistoryRepository = itemOwnershipHistoryRepository;
        this.sessionParticipantRepository = sessionParticipantRepository;
        this.gameConfigRepository = gameConfigRepository;
        this.auditService = auditService;
        this.questService = questService;
    }

    @Transactional
    public ItemTemplateResponse createTemplate(UUID gameSessionId, CreateItemTemplateRequest request) {
        ItemTemplate template = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(gameSessionId)
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .rarity(request.rarity())
                .isUnique(request.isUnique())
                .isLore(Boolean.TRUE.equals(request.isLore()))
                .build());

        return ItemTemplateResponse.from(template);
    }

    @Transactional(readOnly = true)
    public List<ItemTemplateResponse> getTemplates(UUID gameSessionId) {
        return itemTemplateRepository.findByGameSessionIdOrderByNameAsc(gameSessionId).stream()
                .map(ItemTemplateResponse::from)
                .toList();
    }

    @Transactional
    public PlayerItemResponse grantItem(
            UUID targetUserId,
            UUID itemTemplateId,
            UUID gameSessionId,
            UUID performedByUserId
    ) {
        ItemTemplate template = getTemplateForSession(itemTemplateId, gameSessionId);
        ensureParticipant(targetUserId, gameSessionId);
        ensureUniqueConstraint(template);
        ensureLoreLimit(template, targetUserId, gameSessionId);

        PlayerItem playerItem = playerItemRepository.save(PlayerItem.builder()
                .itemTemplate(template)
                .ownerId(targetUserId)
                .gameSessionId(gameSessionId)
                .acquiredFrom(ItemAcquisitionSource.ADMIN)
                .build());

        recordOwnershipHistory(playerItem, null, targetUserId, gameSessionId, OwnershipTransferReason.GRANT);

        auditService.log(
                performedByUserId,
                gameSessionId,
                AuditAction.ITEM_GRANT,
                "PlayerItem",
                playerItem.getId().toString(),
                "Item granted: " + template.getName(),
                Map.of(
                        "itemTemplateId", template.getId().toString(),
                        "targetUserId", targetUserId.toString(),
                        "isUnique", template.getIsUnique()
                )
        );

        questService.updateProgressAfterItemGrant(targetUserId, gameSessionId);

        return PlayerItemResponse.from(playerItem, true);
    }

    @Transactional
    public PlayerItemResponse grantItemFromReward(
            UUID targetUserId,
            UUID itemTemplateId,
            UUID gameSessionId,
            ItemAcquisitionSource source
    ) {
        ItemTemplate template = getTemplateForSession(itemTemplateId, gameSessionId);
        ensureParticipant(targetUserId, gameSessionId);
        ensureUniqueConstraint(template);
        ensureLoreLimit(template, targetUserId, gameSessionId);

        PlayerItem playerItem = playerItemRepository.save(PlayerItem.builder()
                .itemTemplate(template)
                .ownerId(targetUserId)
                .gameSessionId(gameSessionId)
                .acquiredFrom(source)
                .build());

        recordOwnershipHistory(playerItem, null, targetUserId, gameSessionId, OwnershipTransferReason.GRANT);

        questService.updateProgressAfterItemGrant(targetUserId, gameSessionId);

        return PlayerItemResponse.from(playerItem, isLoreRevealed(gameSessionId));
    }

    @Transactional
    public PlayerItemResponse transferItem(
            UUID playerItemId,
            UUID fromUserId,
            UUID targetUserId,
            UUID gameSessionId
    ) {
        if (fromUserId.equals(targetUserId)) {
            throw new BusinessException("Cannot transfer item to yourself");
        }

        PlayerItem playerItem = getPlayerItemForSession(playerItemId, gameSessionId);

        if (!playerItem.getOwnerId().equals(fromUserId)) {
            throw new ItemNotOwnedException();
        }

        ensureParticipant(targetUserId, gameSessionId);

        UUID previousOwner = playerItem.getOwnerId();
        playerItem.setOwnerId(targetUserId);
        playerItem = playerItemRepository.save(playerItem);

        recordOwnershipHistory(playerItem, previousOwner, targetUserId, gameSessionId, OwnershipTransferReason.TRANSFER);

        auditService.log(
                fromUserId,
                gameSessionId,
                AuditAction.ITEM_TRANSFER,
                "PlayerItem",
                playerItem.getId().toString(),
                "Item transferred to user " + targetUserId,
                Map.of(
                        "fromUserId", previousOwner.toString(),
                        "toUserId", targetUserId.toString(),
                        "itemTemplateId", playerItem.getItemTemplate().getId().toString()
                )
        );

        return PlayerItemResponse.from(playerItem, isLoreRevealed(gameSessionId));
    }

    @Transactional(readOnly = true)
    public List<PlayerItemResponse> getInventory(UUID userId, UUID gameSessionId) {
        boolean revealLore = isLoreRevealed(gameSessionId);
        return playerItemRepository.findByOwnerIdAndGameSessionIdOrderByAcquiredAtDesc(userId, gameSessionId).stream()
                .map(item -> PlayerItemResponse.from(item, revealLore))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerItemResponse getInventoryItem(UUID playerItemId, UUID userId, UUID gameSessionId) {
        PlayerItem playerItem = getPlayerItemForSession(playerItemId, gameSessionId);

        if (!playerItem.getOwnerId().equals(userId)) {
            throw new ItemNotOwnedException();
        }

        return PlayerItemResponse.from(playerItem, isLoreRevealed(gameSessionId));
    }

    @Transactional(readOnly = true)
    public List<ItemOwnershipHistoryResponse> getOwnershipHistory(UUID playerItemId, UUID gameSessionId) {
        PlayerItem playerItem = getPlayerItemForSession(playerItemId, gameSessionId);

        return itemOwnershipHistoryRepository.findByPlayerItemIdOrderByCreatedAtAsc(playerItem.getId()).stream()
                .map(ItemOwnershipHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemCollectionStatsResponse getCollectionStats(UUID gameSessionId) {
        Map<UUID, EnumMap<ItemRarity, Long>> countsByOwner = new HashMap<>();
        EnumMap<ItemRarity, Long> collectedTotals = emptyRarityCounts();

        for (Object[] row : playerItemRepository.countByOwnerAndRarity(gameSessionId)) {
            UUID ownerId = (UUID) row[0];
            ItemRarity rarity = (ItemRarity) row[1];
            long count = (Long) row[2];
            countsByOwner
                    .computeIfAbsent(ownerId, ignored -> emptyRarityCounts())
                    .merge(rarity, count, Long::sum);
            collectedTotals.merge(rarity, count, Long::sum);
        }

        EnumMap<ItemRarity, Long> catalogTotals = emptyRarityCounts();
        for (ItemTemplate template : itemTemplateRepository.findByGameSessionIdOrderByNameAsc(gameSessionId)) {
            if (Boolean.TRUE.equals(template.getIsLore())) {
                continue;
            }
            catalogTotals.merge(template.getRarity(), 1L, Long::sum);
        }

        List<SessionParticipant> players = sessionParticipantRepository
                .findByGameSessionIdAndRole(gameSessionId, ParticipantRole.PLAYER);

        List<ItemCollectionStatsEntry> sorted = players.stream()
                .map(participant -> {
                    UUID userId = participant.getUser().getId();
                    EnumMap<ItemRarity, Long> counts = countsByOwner.getOrDefault(userId, emptyRarityCounts());
                    long common = counts.getOrDefault(ItemRarity.COMMON, 0L);
                    long rare = counts.getOrDefault(ItemRarity.RARE, 0L);
                    long epic = counts.getOrDefault(ItemRarity.EPIC, 0L);
                    long legendary = counts.getOrDefault(ItemRarity.LEGENDARY, 0L);
                    return new ItemCollectionStatsEntry(
                            0,
                            userId,
                            participant.getNicknameSnapshot(),
                            common,
                            rare,
                            epic,
                            legendary,
                            common + rare + epic + legendary
                    );
                })
                .sorted(Comparator
                        .comparingLong(ItemCollectionStatsEntry::total).reversed()
                        .thenComparing(Comparator.comparingLong(ItemCollectionStatsEntry::legendary).reversed())
                        .thenComparing(Comparator.comparingLong(ItemCollectionStatsEntry::epic).reversed())
                        .thenComparing(Comparator.comparingLong(ItemCollectionStatsEntry::rare).reversed())
                        .thenComparing(Comparator.comparingLong(ItemCollectionStatsEntry::common).reversed())
                        .thenComparing(ItemCollectionStatsEntry::nickname))
                .toList();

        List<ItemCollectionStatsEntry> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ItemCollectionStatsEntry entry = sorted.get(i);
            ranked.add(new ItemCollectionStatsEntry(
                    i + 1,
                    entry.userId(),
                    entry.nickname(),
                    entry.common(),
                    entry.rare(),
                    entry.epic(),
                    entry.legendary(),
                    entry.total()
            ));
        }

        long catalogTotal = catalogTotals.values().stream().mapToLong(Long::longValue).sum();
        long collectedTotal = collectedTotals.values().stream().mapToLong(Long::longValue).sum();

        return new ItemCollectionStatsResponse(
                ranked,
                toTotals(collectedTotals, Math.max(0, catalogTotal - collectedTotal)),
                toTotals(catalogTotals, 0)
        );
    }

    private static EnumMap<ItemRarity, Long> emptyRarityCounts() {
        EnumMap<ItemRarity, Long> counts = new EnumMap<>(ItemRarity.class);
        for (ItemRarity rarity : ItemRarity.values()) {
            counts.put(rarity, 0L);
        }
        return counts;
    }

    private static ItemCollectionTotals toTotals(EnumMap<ItemRarity, Long> counts, long unclaimed) {
        long common = counts.getOrDefault(ItemRarity.COMMON, 0L);
        long rare = counts.getOrDefault(ItemRarity.RARE, 0L);
        long epic = counts.getOrDefault(ItemRarity.EPIC, 0L);
        long legendary = counts.getOrDefault(ItemRarity.LEGENDARY, 0L);
        return new ItemCollectionTotals(
                common,
                rare,
                epic,
                legendary,
                common + rare + epic + legendary,
                unclaimed
        );
    }

    private ItemTemplate getTemplateForSession(UUID itemTemplateId, UUID gameSessionId) {
        ItemTemplate template = itemTemplateRepository.findById(itemTemplateId)
                .orElseThrow(ItemNotFoundException::new);

        if (!template.getGameSessionId().equals(gameSessionId)) {
            throw new ItemSessionMismatchException();
        }

        return template;
    }

    private PlayerItem getPlayerItemForSession(UUID playerItemId, UUID gameSessionId) {
        return playerItemRepository.findByIdAndGameSessionId(playerItemId, gameSessionId)
                .orElseThrow(ItemNotFoundException::new);
    }

    private void ensureUniqueConstraint(ItemTemplate template) {
        if (Boolean.TRUE.equals(template.getIsUnique())
                && playerItemRepository.existsByItemTemplateIdAndGameSessionId(template.getId(), template.getGameSessionId())) {
            if (Boolean.TRUE.equals(template.getIsLore())) {
                throw new LoreFragmentAlreadyFoundException();
            }
            throw new UniqueItemAlreadyExistsException();
        }
    }

    private void ensureLoreLimit(ItemTemplate template, UUID targetUserId, UUID gameSessionId) {
        if (Boolean.TRUE.equals(template.getIsLore())
                && playerItemRepository.existsLoreItemByOwner(targetUserId, gameSessionId)) {
            throw new PlayerAlreadyHasLoreItemException();
        }
    }

    public boolean isLoreRevealed(UUID gameSessionId) {
        return gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> Boolean.TRUE.equals(config.getLoreRevealed()))
                .orElse(false);
    }

    public boolean playerHasLoreItem(UUID userId, UUID gameSessionId) {
        return playerItemRepository.existsLoreItemByOwner(userId, gameSessionId);
    }

    public List<PlayerItem> findLoreItemsByOwner(UUID userId, UUID gameSessionId) {
        return playerItemRepository.findLoreItemsByOwner(userId, gameSessionId);
    }

    public List<PlayerItem> findLoreItemsBySession(UUID gameSessionId) {
        return playerItemRepository.findLoreItemsByGameSessionId(gameSessionId);
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }

    private void recordOwnershipHistory(
            PlayerItem playerItem,
            UUID fromUserId,
            UUID toUserId,
            UUID gameSessionId,
            OwnershipTransferReason reason
    ) {
        itemOwnershipHistoryRepository.save(ItemOwnershipHistory.builder()
                .playerItem(playerItem)
                .gameSessionId(gameSessionId)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .reason(reason)
                .build());
    }
}
