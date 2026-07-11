package com.mos.qrcode.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.InvalidQrRewardException;
import com.mos.common.exception.LocationSessionMismatchException;
import com.mos.common.exception.QrAlreadyScannedException;
import com.mos.common.exception.QrCodeNotFoundException;
import com.mos.common.exception.QrScanLimitReachedException;
import com.mos.item.dto.PlayerItemResponse;
import com.mos.item.enums.ItemAcquisitionSource;
import com.mos.item.service.ItemService;
import com.mos.location.entity.LocationPoint;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.service.LocationService;
import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.dto.QrCodeResponse;
import com.mos.qrcode.dto.QrScanResponse;
import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.entity.QrScan;
import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.qrcode.repository.QrScanRepository;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final QrScanRepository qrScanRepository;
    private final LocationPointRepository locationPointRepository;
    private final LocationService locationService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final AuditService auditService;

    @Transactional
    public QrCodeResponse createQrCode(UUID gameSessionId, CreateQrCodeRequest request) {
        validateScanLimit(request.scanPolicy(), request.scanLimit());

        if (qrCodeRepository.existsByCodeAndGameSessionId(request.code(), gameSessionId)) {
            throw new BusinessException("QR code already exists in this game session");
        }

        LocationPoint locationPoint = resolveLocationPoint(request.locationPointId(), gameSessionId);

        QrCode qrCode = qrCodeRepository.save(QrCode.builder()
                .gameSessionId(gameSessionId)
                .locationPoint(locationPoint)
                .code(request.code())
                .rewardType(request.rewardType())
                .rewardPayload(request.rewardPayload() != null ? new HashMap<>(request.rewardPayload()) : new HashMap<>())
                .scanPolicy(request.scanPolicy())
                .scanLimit(request.scanLimit())
                .build());

        validateRewardConfiguration(qrCode);

        return QrCodeResponse.from(qrCode);
    }

    @Transactional
    public QrScanResponse scanQrCode(UUID userId, UUID gameSessionId, String code) {
        QrCode qrCode = qrCodeRepository.findByCodeAndGameSessionId(code, gameSessionId)
                .orElseThrow(QrCodeNotFoundException::new);

        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            throw new BusinessException("QR code is inactive");
        }

        ensureScanAllowed(qrCode, userId);

        QrScan scan = qrScanRepository.save(QrScan.builder()
                .qrCode(qrCode)
                .userId(userId)
                .gameSessionId(gameSessionId)
                .build());

        Long coinAmount = applyCoinReward(qrCode, userId, gameSessionId);
        PlayerItemResponse grantedItem = applyItemReward(qrCode, userId, gameSessionId);
        boolean locationDiscovered = discoverLinkedLocation(qrCode, userId, gameSessionId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("qrCodeId", qrCode.getId().toString());
        metadata.put("code", qrCode.getCode());
        metadata.put("scanPolicy", qrCode.getScanPolicy().name());
        metadata.put("rewardType", qrCode.getRewardType().name());
        metadata.put("scanId", scan.getId().toString());
        if (coinAmount != null) {
            metadata.put("coinAmount", coinAmount);
        }
        if (grantedItem != null) {
            metadata.put("playerItemId", grantedItem.id().toString());
            metadata.put("itemTemplateId", grantedItem.template().id().toString());
        }
        metadata.put("locationDiscovered", locationDiscovered);

        auditService.log(
                userId,
                gameSessionId,
                AuditAction.QR_SCAN,
                "QrCode",
                qrCode.getId().toString(),
                "QR code scanned: " + qrCode.getCode(),
                metadata
        );

        return new QrScanResponse(
                qrCode.getId(),
                qrCode.getCode(),
                qrCode.getRewardType(),
                coinAmount,
                grantedItem,
                locationDiscovered
        );
    }

    private void ensureScanAllowed(QrCode qrCode, UUID userId) {
        if (qrScanRepository.existsByUserIdAndQrCodeId(userId, qrCode.getId())) {
            throw new QrAlreadyScannedException();
        }

        long totalScans = qrScanRepository.countByQrCodeId(qrCode.getId());

        switch (qrCode.getScanPolicy()) {
            case FIRST_PLAYER -> {
                if (totalScans > 0) {
                    throw new QrScanLimitReachedException();
                }
            }
            case EVERY_PLAYER -> {
                // per-user uniqueness is enforced above
            }
            case LIMITED -> {
                if (totalScans >= qrCode.getScanLimit()) {
                    throw new QrScanLimitReachedException();
                }
            }
        }
    }

    private Long applyCoinReward(QrCode qrCode, UUID userId, UUID gameSessionId) {
        if (qrCode.getRewardType() != QrRewardType.COIN) {
            return null;
        }

        long amount = extractCoinAmount(qrCode.getRewardPayload());
        walletService.creditWithoutAudit(
                userId,
                gameSessionId,
                amount,
                CoinTransactionType.QR,
                qrCode.getId().toString(),
                "QR reward: " + qrCode.getCode()
        );
        return amount;
    }

    private PlayerItemResponse applyItemReward(QrCode qrCode, UUID userId, UUID gameSessionId) {
        if (qrCode.getRewardType() != QrRewardType.ITEM) {
            return null;
        }

        UUID itemTemplateId = extractItemTemplateId(qrCode.getRewardPayload());
        return itemService.grantItemFromReward(userId, itemTemplateId, gameSessionId, ItemAcquisitionSource.QR);
    }

    private boolean discoverLinkedLocation(QrCode qrCode, UUID userId, UUID gameSessionId) {
        if (qrCode.getLocationPoint() == null) {
            return false;
        }

        locationService.discoverLocation(userId, qrCode.getLocationPoint().getId(), gameSessionId);
        return true;
    }

    private LocationPoint resolveLocationPoint(UUID locationPointId, UUID gameSessionId) {
        if (locationPointId == null) {
            return null;
        }

        LocationPoint locationPoint = locationPointRepository.findById(locationPointId)
                .orElseThrow(() -> new BusinessException("Location point not found"));

        if (!locationPoint.getGameSessionId().equals(gameSessionId)) {
            throw new LocationSessionMismatchException();
        }

        return locationPoint;
    }

    private void validateScanLimit(QrScanPolicy scanPolicy, Integer scanLimit) {
        if (scanPolicy == QrScanPolicy.LIMITED && (scanLimit == null || scanLimit <= 0)) {
            throw new BusinessException("Scan limit is required for LIMITED policy");
        }
    }

    private void validateRewardConfiguration(QrCode qrCode) {
        switch (qrCode.getRewardType()) {
            case COIN -> extractCoinAmount(qrCode.getRewardPayload());
            case ITEM -> extractItemTemplateId(qrCode.getRewardPayload());
            case NONE -> {
                // no reward payload required
            }
        }
    }

    private long extractCoinAmount(Map<String, Object> payload) {
        Object amountValue = payload != null ? payload.get("amount") : null;
        if (amountValue == null) {
            throw new InvalidQrRewardException("Coin reward requires amount in payload");
        }

        long amount = switch (amountValue) {
            case Number number -> number.longValue();
            case String text -> Long.parseLong(text);
            default -> throw new InvalidQrRewardException("Invalid coin amount in reward payload");
        };

        if (amount <= 0) {
            throw new InvalidQrRewardException("Coin amount must be positive");
        }

        return amount;
    }

    private UUID extractItemTemplateId(Map<String, Object> payload) {
        Object templateValue = payload != null ? payload.get("itemTemplateId") : null;
        if (templateValue == null) {
            throw new InvalidQrRewardException("Item reward requires itemTemplateId in payload");
        }

        try {
            return UUID.fromString(templateValue.toString());
        } catch (IllegalArgumentException ex) {
            throw new InvalidQrRewardException("Invalid itemTemplateId in reward payload");
        }
    }
}
