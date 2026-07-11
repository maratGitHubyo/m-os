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
import com.mos.qrcode.dto.CreateQrCodeSimpleRequest;
import com.mos.qrcode.dto.QrCodeResponse;
import com.mos.qrcode.dto.QrScanResponse;
import com.mos.qrcode.dto.QrScanResultResponse;
import com.mos.qrcode.dto.QrScanRewardInfo;
import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.entity.QrScan;
import com.mos.qrcode.enums.QrRewardKind;
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
import java.util.Locale;
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
                .title(request.code())
                .rewardType(request.rewardType())
                .rewardPayload(request.rewardPayload() != null ? new HashMap<>(request.rewardPayload()) : new HashMap<>())
                .scanPolicy(request.scanPolicy())
                .scanLimit(request.scanLimit())
                .build());

        validateRewardConfiguration(qrCode);

        return QrCodeResponse.from(qrCode);
    }

    @Transactional
    public QrCodeResponse createQrCodeSimple(UUID gameSessionId, CreateQrCodeSimpleRequest request) {
        QrScanPolicy scanPolicy = request.scanPolicy() != null ? request.scanPolicy() : QrScanPolicy.EVERY_PLAYER;
        validateScanLimit(scanPolicy, request.scanLimit());
        validateSimpleReward(request);

        LocationPoint locationPoint = resolveLocationPoint(request.locationPointId(), gameSessionId);
        QrRewardType rewardType;
        Map<String, Object> rewardPayload = new HashMap<>();

        switch (request.rewardKind()) {
            case COIN -> {
                rewardType = QrRewardType.COIN;
                rewardPayload.put("amount", request.coinAmount());
            }
            case ITEM -> {
                rewardType = QrRewardType.ITEM;
                rewardPayload.put("itemTemplateId", request.itemTemplateId().toString());
            }
            case LOCATION -> rewardType = QrRewardType.NONE;
            case NONE -> rewardType = QrRewardType.NONE;
            default -> throw new BusinessException("Unsupported reward kind");
        }

        String code = generateUniqueCode(gameSessionId, request.title());

        QrCode qrCode = qrCodeRepository.save(QrCode.builder()
                .gameSessionId(gameSessionId)
                .locationPoint(locationPoint)
                .code(code)
                .title(request.title())
                .rewardType(rewardType)
                .rewardPayload(rewardPayload)
                .scanPolicy(scanPolicy)
                .scanLimit(request.scanLimit())
                .build());

        validateRewardConfiguration(qrCode);

        return QrCodeResponse.from(qrCode);
    }

    @Transactional(readOnly = true)
    public QrCode getQrCodeForSession(UUID qrCodeId, UUID gameSessionId) {
        QrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(QrCodeNotFoundException::new);

        if (!qrCode.getGameSessionId().equals(gameSessionId)) {
            throw new QrCodeNotFoundException();
        }

        return qrCode;
    }

    @Transactional
    public QrScanResponse scanQrCode(UUID userId, UUID gameSessionId, String code) {
        QrCode qrCode = qrCodeRepository.findByCodeAndGameSessionId(code, gameSessionId)
                .orElseThrow(QrCodeNotFoundException::new);

        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            throw new BusinessException("QR code is inactive");
        }

        ensureScanAllowed(qrCode, userId);

        return performScan(userId, gameSessionId, qrCode);
    }

    @Transactional
    public QrScanResultResponse scanQrCodeByPublicId(UUID userId, UUID gameSessionId, UUID publicId) {
        QrCode qrCode = qrCodeRepository.findByPublicId(publicId)
                .orElseThrow(QrCodeNotFoundException::new);

        String title = displayTitle(qrCode);

        if (!qrCode.getGameSessionId().equals(gameSessionId)) {
            return new QrScanResultResponse(false, title, null, "QR unavailable");
        }

        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            return new QrScanResultResponse(false, title, null, "QR unavailable");
        }

        if (qrScanRepository.existsByUserIdAndQrCodeId(userId, qrCode.getId())) {
            return new QrScanResultResponse(false, title, null, "QR already scanned");
        }

        try {
            ensureScanAllowed(qrCode, userId);
        } catch (QrScanLimitReachedException ex) {
            return new QrScanResultResponse(false, title, null, "QR unavailable");
        }

        QrScanResponse scanResult = performScan(userId, gameSessionId, qrCode);

        QrScanRewardInfo reward = new QrScanRewardInfo(
                scanResult.coinAmount(),
                scanResult.grantedItem() != null ? scanResult.grantedItem().template().name() : null
        );

        return new QrScanResultResponse(true, title, reward, "Reward received");
    }

    private QrScanResponse performScan(UUID userId, UUID gameSessionId, QrCode qrCode) {
        QrScan scan = qrScanRepository.save(QrScan.builder()
                .qrCode(qrCode)
                .userId(userId)
                .gameSessionId(gameSessionId)
                .rewardGiven(true)
                .build());

        Long coinAmount = applyCoinReward(qrCode, userId, gameSessionId);
        PlayerItemResponse grantedItem = applyItemReward(qrCode, userId, gameSessionId);
        boolean locationDiscovered = discoverLinkedLocation(qrCode, userId, gameSessionId);
        boolean rewardGiven = coinAmount != null || grantedItem != null || locationDiscovered;

        if (!rewardGiven) {
            scan.setRewardGiven(false);
            qrScanRepository.save(scan);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("qrCodeId", qrCode.getId().toString());
        metadata.put("publicId", qrCode.getPublicId().toString());
        metadata.put("code", qrCode.getCode());
        metadata.put("scanPolicy", qrCode.getScanPolicy().name());
        metadata.put("rewardType", qrCode.getRewardType().name());
        metadata.put("scanId", scan.getId().toString());
        metadata.put("rewardGiven", rewardGiven);
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

    private void validateSimpleReward(CreateQrCodeSimpleRequest request) {
        switch (request.rewardKind()) {
            case COIN -> {
                if (request.coinAmount() == null || request.coinAmount() <= 0) {
                    throw new BusinessException("Coin amount is required for COIN reward");
                }
            }
            case ITEM -> {
                if (request.itemTemplateId() == null) {
                    throw new BusinessException("Item template is required for ITEM reward");
                }
            }
            case LOCATION -> {
                if (request.locationPointId() == null) {
                    throw new BusinessException("Location is required for LOCATION reward");
                }
            }
            case NONE -> {
                // no extra validation
            }
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

    private String generateUniqueCode(UUID gameSessionId, String title) {
        String slug = slugify(title);
        String candidate = slug;
        int attempt = 0;
        while (qrCodeRepository.existsByCodeAndGameSessionId(candidate, gameSessionId)) {
            attempt++;
            candidate = slug + "-" + attempt;
        }
        return candidate;
    }

    private String slugify(String title) {
        String slug = title.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = "qr";
        }
        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String displayTitle(QrCode qrCode) {
        if (qrCode.getTitle() != null && !qrCode.getTitle().isBlank()) {
            return qrCode.getTitle();
        }
        return qrCode.getCode();
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
