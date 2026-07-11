package com.mos.qrcode.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanQrCodeRequest(
        @NotBlank String code
) {
}
