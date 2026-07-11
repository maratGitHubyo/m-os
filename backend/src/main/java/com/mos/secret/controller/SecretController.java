package com.mos.secret.controller;

import com.mos.secret.dto.RedeemSecretRequest;
import com.mos.secret.dto.SecretRedeemResponse;
import com.mos.secret.service.PlayerSecretService;
import com.mos.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
@Tag(name = "Secrets")
public class SecretController {

    private final PlayerSecretService playerSecretService;

    @PostMapping("/redeem")
    public SecretRedeemResponse redeemSecret(@Valid @RequestBody RedeemSecretRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        return playerSecretService.redeemSecret(
                currentUser.userId(),
                currentUser.gameSessionId(),
                request.code()
        );
    }
}
