package com.mos.qrcode.dto;

import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;

import java.util.Map;
import java.util.UUID;

public record QrCodeResponse(
        UUID id,
        UUID gameSessionId,
        UUID locationPointId,
        String code,
        QrRewardType rewardType,
        Map<String, Object> rewardPayload,
        QrScanPolicy scanPolicy,
        Integer scanLimit,
        Boolean active
) {

    public static QrCodeResponse from(QrCode qrCode) {
        return new QrCodeResponse(
                qrCode.getId(),
                qrCode.getGameSessionId(),
                qrCode.getLocationPoint() != null ? qrCode.getLocationPoint().getId() : null,
                qrCode.getCode(),
                qrCode.getRewardType(),
                qrCode.getRewardPayload(),
                qrCode.getScanPolicy(),
                qrCode.getScanLimit(),
                qrCode.getActive()
        );
    }
}
