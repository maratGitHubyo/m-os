package com.mos.secret.controller;

import com.mos.secret.dto.CreatePlayerSecretRequest;
import com.mos.secret.dto.PlayerSecretResponse;
import com.mos.secret.service.PlayerSecretService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/secrets")
@RequiredArgsConstructor
public class AdminSecretController {

    private final PlayerSecretService playerSecretService;

    @PostMapping
    public PlayerSecretResponse createSecret(@Valid @RequestBody CreatePlayerSecretRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return playerSecretService.createSecret(admin.gameSessionId(), request);
    }
}
