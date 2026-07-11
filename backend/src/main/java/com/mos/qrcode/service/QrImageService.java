package com.mos.qrcode.service;

import com.mos.common.exception.BusinessException;
import com.mos.qrcode.config.MosQrProperties;
import com.mos.qrcode.entity.QrCode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrImageService {

    private static final int IMAGE_SIZE = 512;

    private final MosQrProperties mosQrProperties;

    public byte[] generateQrImage(QrCode qrCode) {
        String content = buildQrContent(qrCode.getPublicId());
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException("Failed to generate QR image");
        }
    }

    public String buildQrContent(java.util.UUID publicId) {
        String baseUrl = mosQrProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://mos.local/qr";
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + "/" + publicId;
    }
}
