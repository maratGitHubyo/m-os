package com.mos.numbers.controller;

import com.mos.numbers.dto.CollectibleNumberResponse;
import com.mos.numbers.dto.CreateCollectibleNumberRequest;
import com.mos.numbers.dto.PlayerNumberResponse;
import com.mos.numbers.service.NumberService;
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
@RequestMapping("/api/admin/numbers")
@RequiredArgsConstructor
@Tag(name = "Admin Numbers")
public class AdminNumberController {

    private final NumberService numberService;

    @PostMapping
    public CollectibleNumberResponse createNumber(@Valid @RequestBody CreateCollectibleNumberRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return numberService.createNumber(admin.gameSessionId(), request);
    }

    @PostMapping("/{numberId}/grant/{userId}")
    public PlayerNumberResponse grantNumber(
            @PathVariable UUID numberId,
            @PathVariable UUID userId
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return numberService.grantNumber(
                userId,
                numberId,
                admin.gameSessionId(),
                admin.userId()
        );
    }
}
