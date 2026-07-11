package com.mos.qrcode.controller;

import com.mos.qrcode.dto.QrScanResponse;
import com.mos.qrcode.dto.QrScanResultResponse;
import com.mos.qrcode.dto.ScanQrCodeRequest;
import com.mos.qrcode.service.QrCodeService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
@Tag(name = "QR Codes")
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping("/scan")
    public QrScanResponse scanQrCode(@Valid @RequestBody ScanQrCodeRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        return qrCodeService.scanQrCode(currentUser.userId(), currentUser.gameSessionId(), request.code());
    }

    @PostMapping("/scan/{publicId}")
    public QrScanResultResponse scanQrCodeByPublicId(@PathVariable UUID publicId) {
        var currentUser = SecurityUtils.getCurrentUser();
        return qrCodeService.scanQrCodeByPublicId(currentUser.userId(), currentUser.gameSessionId(), publicId);
    }
}
