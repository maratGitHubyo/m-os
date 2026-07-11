package com.mos.qrcode.dto;

import com.mos.item.dto.PlayerItemResponse;
import com.mos.qrcode.enums.QrRewardType;

import java.util.UUID;

public record QrScanResponse(
        UUID qrCodeId,
        String code,
        QrRewardType rewardType,
        Long coinAmount,
        PlayerItemResponse grantedItem,
        boolean locationDiscovered
) {
}
