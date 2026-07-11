package com.mos.qrcode.controller;

import com.mos.qrcode.dto.CreateQrCodeRequest;
import com.mos.qrcode.dto.QrCodeResponse;
import com.mos.qrcode.service.QrCodeService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/qr")
@RequiredArgsConstructor
@Tag(name = "Admin QR Codes")
public class AdminQrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping
    public QrCodeResponse createQrCode(@Valid @RequestBody CreateQrCodeRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return qrCodeService.createQrCode(admin.gameSessionId(), request);
    }
}
