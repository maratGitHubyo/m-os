package com.mos.qrcode.dto;

public record QrScanResultResponse(
        boolean success,
        String title,
        QrScanRewardInfo reward,
        String message
) {
}
