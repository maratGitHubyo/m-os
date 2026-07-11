package com.mos.wallet.controller;

import com.mos.security.SecurityUtils;
import com.mos.wallet.dto.CoinTransactionResponse;
import com.mos.wallet.dto.WalletResponse;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public WalletResponse getMyWallet() {
        var currentUser = SecurityUtils.getCurrentUser();
        return walletService.getWallet(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/me/transactions")
    public Page<CoinTransactionResponse> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        var currentUser = SecurityUtils.getCurrentUser();
        return walletService.getTransactions(currentUser.userId(), currentUser.gameSessionId(), pageable);
    }
}
