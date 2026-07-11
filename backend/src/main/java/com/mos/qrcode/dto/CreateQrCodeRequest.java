package com.mos.qrcode.dto;

import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateQrCodeRequest(
        @NotBlank String code,
        UUID locationPointId,
        @NotNull QrRewardType rewardType,
        Map<String, Object> rewardPayload,
        @NotNull QrScanPolicy scanPolicy,
        Integer scanLimit
) {
}
