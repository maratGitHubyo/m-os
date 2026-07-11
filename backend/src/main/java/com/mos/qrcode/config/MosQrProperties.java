package com.mos.qrcode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mos.qr")
public record MosQrProperties(
        String baseUrl
) {
}
