package com.mos.lore.controller;

import com.mos.lore.dto.LoreRevealResponse;
import com.mos.lore.dto.LoreSeedResponse;
import com.mos.lore.dto.LoreStatsResponse;
import com.mos.lore.dto.SetLoreRevealRequest;
import com.mos.lore.service.LoreService;
import com.mos.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/lore")
@RequiredArgsConstructor
@Tag(name = "Admin Lore")
public class AdminLoreController {

    private final LoreService loreService;

    @GetMapping
    public LoreStatsResponse getStats() {
        var admin = SecurityUtils.getCurrentUser();
        return loreService.getStats(admin.gameSessionId());
    }

    @PostMapping("/reveal")
    public LoreRevealResponse setReveal(@Valid @RequestBody SetLoreRevealRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return loreService.setLoreRevealed(admin.gameSessionId(), request.revealed(), admin.userId());
    }

    @PostMapping("/seed")
    public LoreSeedResponse seed() {
        var admin = SecurityUtils.getCurrentUser();
        return loreService.seedLore(admin.gameSessionId());
    }
}
