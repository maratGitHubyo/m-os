package com.mos.wallet.controller;

import com.mos.security.SecurityUtils;
import com.mos.wallet.dto.LeaderboardEntryResponse;
import com.mos.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Leaderboard")
public class LeaderboardController {

    private final WalletService walletService;

    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard() {
        var currentUser = SecurityUtils.getCurrentUser();
        return walletService.getCoinLeaderboard(currentUser.gameSessionId());
    }
}
