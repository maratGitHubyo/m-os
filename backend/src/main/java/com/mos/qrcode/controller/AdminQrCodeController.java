package com.mos.qrcode.controller;

import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.dto.CreateQrCodeSimpleRequest;
import com.mos.qrcode.dto.QrCodeResponse;
import com.mos.qrcode.entity.QrCode;
import com.mos.qrcode.service.QrCodeService;
import com.mos.qrcode.service.QrImageService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/qr")
@RequiredArgsConstructor
@Tag(name = "Admin QR Codes")
public class AdminQrCodeController {

    private final QrCodeService qrCodeService;
    private final QrImageService qrImageService;

    @PostMapping
    public QrCodeResponse createQrCode(@Valid @RequestBody CreateQrCodeRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return qrCodeService.createQrCode(admin.gameSessionId(), request);
    }

    @PostMapping("/simple")
    public QrCodeResponse createQrCodeSimple(@Valid @RequestBody CreateQrCodeSimpleRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return qrCodeService.createQrCodeSimple(admin.gameSessionId(), request);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getQrImage(@PathVariable UUID id) {
        var admin = SecurityUtils.getCurrentUser();
        QrCode qrCode = qrCodeService.getQrCodeForSession(id, admin.gameSessionId());
        byte[] image = qrImageService.generateQrImage(qrCode);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-" + qrCode.getPublicId() + ".png\"")
                .body(image);
    }
}
