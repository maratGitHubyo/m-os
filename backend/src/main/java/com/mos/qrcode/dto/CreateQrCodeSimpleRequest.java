package com.mos.qrcode.dto;

import com.mos.qrcode.enums.QrRewardKind;
import com.mos.qrcode.enums.QrScanPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateQrCodeSimpleRequest(
        @NotBlank String title,
        @NotNull QrRewardKind rewardKind,
        @Positive Long coinAmount,
        UUID itemTemplateId,
        UUID locationPointId,
        QrScanPolicy scanPolicy,
        @Positive Integer scanLimit
) {
}
