package com.mos.secret.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.InvalidSecretRewardException;
import com.mos.common.exception.LoreFragmentAlreadyFoundException;
import com.mos.common.exception.SecretAlreadyUsedException;
import com.mos.common.exception.SecretNotFoundException;
import com.mos.common.exception.SecretNotOwnedException;
import com.mos.common.exception.SecretRewardNotImplementedException;
import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.entity.ItemTemplate;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.service.ItemService;
import com.mos.location.service.LocationService;
import com.mos.secret.dto.CreatePlayerSecretRequest;
import com.mos.secret.dto.CreateSharedSecretRequest;
import com.mos.secret.dto.PlayerSecretResponse;
import com.mos.secret.dto.SecretRedeemResponse;
import com.mos.secret.entity.PlayerSecret;
import com.mos.secret.enums.SecretRewardType;
import com.mos.secret.repository.PlayerSecretRepository;
import com.mos.seed.DachaMapCatalog;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerSecretService {

    private final PlayerSecretRepository playerSecretRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final WalletService walletService;
    private final ItemService itemService;
    private final ItemTemplateRepository itemTemplateRepository;
    private final LocationService locationService;
    private final AuditService auditService;

    @Transactional
    public PlayerSecretResponse createSecret(UUID gameSessionId, CreatePlayerSecretRequest request) {
        ensureParticipant(request.userId(), gameSessionId);

        if (playerSecretRepository.existsByCodeAndGameSessionId(request.code(), gameSessionId)) {
            throw new BusinessException("Secret code already exists in this game session");
        }

        PlayerSecret secret = playerSecretRepository.save(PlayerSecret.builder()
                .userId(request.userId())
                .gameSessionId(gameSessionId)
                .code(request.code())
                .title(request.title())
                .description(request.description())
                .rewardType(request.rewardType())
                .rewardPayload(request.rewardPayload() != null ? new HashMap<>(request.rewardPayload()) : new HashMap<>())
                .isShared(false)
                .build());

        validateRewardPayload(secret);

        return PlayerSecretResponse.from(secret);
    }

    @Transactional
    public PlayerSecretResponse createSharedSecret(UUID gameSessionId, CreateSharedSecretRequest request) {
        if (playerSecretRepository.existsByCodeAndGameSessionId(request.code(), gameSessionId)) {
            throw new BusinessException("Secret code already exists in this game session");
        }

        PlayerSecret secret = playerSecretRepository.save(PlayerSecret.builder()
                .userId(null)
                .gameSessionId(gameSessionId)
                .code(request.code())
                .title(request.title())
                .description(request.description())
                .rewardType(request.rewardType())
                .rewardPayload(request.rewardPayload() != null ? new HashMap<>(request.rewardPayload()) : new HashMap<>())
                .isShared(true)
                .build());

        validateRewardPayload(secret);

        return PlayerSecretResponse.from(secret);
    }

    @Transactional
    public SecretRedeemResponse redeemSecret(UUID userId, UUID gameSessionId, String code) {
        ensureParticipant(userId, gameSessionId);

        PlayerSecret secret = playerSecretRepository.findByCodeAndGameSessionIdForUpdate(code, gameSessionId)
                .orElseThrow(SecretNotFoundException::new);

        if (!Boolean.TRUE.equals(secret.getIsShared())) {
            if (secret.getUserId() == null || !secret.getUserId().equals(userId)) {
                throw new SecretNotOwnedException();
            }
        }

        if (Boolean.TRUE.equals(secret.getUsed())) {
            if (Boolean.TRUE.equals(secret.getIsShared()) && isLoreItemSecret(secret)) {
                throw new LoreFragmentAlreadyFoundException();
            }
            throw new SecretAlreadyUsedException();
        }

        if (secret.getRewardType() == SecretRewardType.NUMBER || secret.getRewardType() == SecretRewardType.QUEST) {
            throw new SecretRewardNotImplementedException(secret.getRewardType().name());
        }

        Long coinAmount = applyCoinReward(secret, userId, gameSessionId);
        PlayerItemResponse grantedItem = applyItemReward(secret, userId, gameSessionId);

        secret.setUsed(true);
        secret.setUsedAt(Instant.now());
        playerSecretRepository.save(secret);

        if (Boolean.TRUE.equals(secret.getIsShared()) && isLoreItemSecret(secret)) {
            removeLoreMapSpot(gameSessionId, secret.getCode(), userId);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("secretId", secret.getId().toString());
        metadata.put("code", secret.getCode());
        metadata.put("rewardType", secret.getRewardType().name());
        metadata.put("isShared", Boolean.TRUE.equals(secret.getIsShared()));
        if (coinAmount != null) {
            metadata.put("coinAmount", coinAmount);
        }
        if (grantedItem != null) {
            metadata.put("playerItemId", grantedItem.id().toString());
        }

        auditService.log(
                userId,
                gameSessionId,
                AuditAction.SECRET_REDEEM,
                "PlayerSecret",
                secret.getId().toString(),
                "Secret redeemed: " + secret.getCode(),
                metadata
        );

        return new SecretRedeemResponse(
                secret.getId(),
                secret.getCode(),
                secret.getTitle(),
                secret.getRewardType(),
                coinAmount,
                grantedItem
        );
    }

    private void removeLoreMapSpot(UUID gameSessionId, String loreCode, UUID performedByUserId) {
        String spotName = DachaMapCatalog.SPOTS.stream()
                .filter(spot -> spot.loreCode().equalsIgnoreCase(loreCode))
                .map(DachaMapCatalog.MapSpot::name)
                .findFirst()
                .orElse(null);
        if (spotName == null) {
            return;
        }
        locationService.findByName(gameSessionId, spotName).ifPresent(location ->
                locationService.deleteLocation(location.getId(), gameSessionId, performedByUserId)
        );
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }

    private Long applyCoinReward(PlayerSecret secret, UUID userId, UUID gameSessionId) {
        if (secret.getRewardType() != SecretRewardType.COIN) {
            return null;
        }

        long amount = extractCoinAmount(secret.getRewardPayload());
        walletService.creditWithoutAudit(
                userId,
                gameSessionId,
                amount,
                CoinTransactionType.SECRET,
                secret.getId().toString(),
                "Secret reward: " + secret.getCode()
        );
        return amount;
    }

    private PlayerItemResponse applyItemReward(PlayerSecret secret, UUID userId, UUID gameSessionId) {
        if (secret.getRewardType() != SecretRewardType.ITEM) {
            return null;
        }

        UUID itemTemplateId = extractItemTemplateId(secret.getRewardPayload());
        return itemService.grantItemFromReward(userId, itemTemplateId, gameSessionId, ItemAcquisitionSource.SECRET);
    }

    private void validateRewardPayload(PlayerSecret secret) {
        switch (secret.getRewardType()) {
            case COIN -> extractCoinAmount(secret.getRewardPayload());
            case ITEM -> extractItemTemplateId(secret.getRewardPayload());
            case NUMBER -> extractNumber(secret.getRewardPayload());
            case QUEST -> extractQuestId(secret.getRewardPayload());
            case NONE -> {
                // no payload required
            }
        }
    }

    private long extractCoinAmount(Map<String, Object> payload) {
        Object amountValue = payload != null ? payload.get("amount") : null;
        if (amountValue == null) {
            throw new InvalidSecretRewardException("Coin reward requires amount in payload");
        }

        long amount = switch (amountValue) {
            case Number number -> number.longValue();
            case String text -> Long.parseLong(text);
            default -> throw new InvalidSecretRewardException("Invalid coin amount in reward payload");
        };

        if (amount <= 0) {
            throw new InvalidSecretRewardException("Coin amount must be positive");
        }

        return amount;
    }

    private UUID extractItemTemplateId(Map<String, Object> payload) {
        Object templateValue = payload != null ? payload.get("itemTemplateId") : null;
        if (templateValue == null) {
            throw new InvalidSecretRewardException("Item reward requires itemTemplateId in payload");
        }

        try {
            return UUID.fromString(templateValue.toString());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSecretRewardException("Invalid itemTemplateId in reward payload");
        }
    }

    private int extractNumber(Map<String, Object> payload) {
        Object numberValue = payload != null ? payload.get("number") : null;
        if (numberValue == null) {
            throw new InvalidSecretRewardException("Number reward requires number in payload");
        }

        return switch (numberValue) {
            case Number number -> number.intValue();
            case String text -> Integer.parseInt(text);
            default -> throw new InvalidSecretRewardException("Invalid number in reward payload");
        };
    }

    private UUID extractQuestId(Map<String, Object> payload) {
        Object questValue = payload != null ? payload.get("questId") : null;
        if (questValue == null) {
            throw new InvalidSecretRewardException("Quest reward requires questId in payload");
        }

        try {
            return UUID.fromString(questValue.toString());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSecretRewardException("Invalid questId in reward payload");
        }
    }

    private boolean isLoreItemSecret(PlayerSecret secret) {
        if (secret.getRewardType() != SecretRewardType.ITEM) {
            return false;
        }
        try {
            UUID templateId = extractItemTemplateId(secret.getRewardPayload());
            return itemTemplateRepository.findById(templateId)
                    .map(ItemTemplate::getIsLore)
                    .map(Boolean.TRUE::equals)
                    .orElse(false);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
