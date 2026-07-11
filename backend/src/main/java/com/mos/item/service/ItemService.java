package com.mos.item.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.ItemNotFoundException;
import com.mos.common.exception.ItemNotOwnedException;
import com.mos.common.exception.ItemSessionMismatchException;
import com.mos.common.exception.UniqueItemAlreadyExistsException;
import com.mos.item.dto.CreateItemTemplateRequest;
import com.mos.item.dto.ItemOwnershipHistoryResponse;
import com.mos.item.dto.ItemTemplateResponse;
import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.entity.ItemOwnershipHistory;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.entity.PlayerItem;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.enums.OwnershipTransferReason;
import com.mos.item.repository.ItemOwnershipHistoryRepository;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.session.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemTemplateRepository itemTemplateRepository;
    private final PlayerItemRepository playerItemRepository;
    private final ItemOwnershipHistoryRepository itemOwnershipHistoryRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final AuditService auditService;

    @Transactional
    public ItemTemplateResponse createTemplate(UUID gameSessionId, CreateItemTemplateRequest request) {
        ItemTemplate template = itemTemplateRepository.save(ItemTemplate.builder()
                .gameSessionId(gameSessionId)
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .rarity(request.rarity())
                .isUnique(request.isUnique())
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

        return PlayerItemResponse.from(playerItem);
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

        return PlayerItemResponse.from(playerItem);
    }

    @Transactional(readOnly = true)
    public List<PlayerItemResponse> getInventory(UUID userId, UUID gameSessionId) {
        return playerItemRepository.findByOwnerIdAndGameSessionIdOrderByAcquiredAtDesc(userId, gameSessionId).stream()
                .map(PlayerItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerItemResponse getInventoryItem(UUID playerItemId, UUID userId, UUID gameSessionId) {
        PlayerItem playerItem = getPlayerItemForSession(playerItemId, gameSessionId);

        if (!playerItem.getOwnerId().equals(userId)) {
            throw new ItemNotOwnedException();
        }

        return PlayerItemResponse.from(playerItem);
    }

    @Transactional(readOnly = true)
    public List<ItemOwnershipHistoryResponse> getOwnershipHistory(UUID playerItemId, UUID gameSessionId) {
        PlayerItem playerItem = getPlayerItemForSession(playerItemId, gameSessionId);

        return itemOwnershipHistoryRepository.findByPlayerItemIdOrderByCreatedAtAsc(playerItem.getId()).stream()
                .map(ItemOwnershipHistoryResponse::from)
                .toList();
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
            throw new UniqueItemAlreadyExistsException();
        }
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
