package com.mos.wallet.controller;

import com.mos.security.SecurityUtils;
import com.mos.wallet.dto.AdminWalletOperationRequest;
import com.mos.wallet.dto.CoinTransactionResponse;
import com.mos.wallet.service.WalletService;
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
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
@Tag(name = "Admin Wallet")
public class AdminWalletController {

    private final WalletService walletService;

    @PostMapping("/{userId}/credit")
    public CoinTransactionResponse credit(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminWalletOperationRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return walletService.adminCredit(
                userId,
                admin.gameSessionId(),
                request.amount(),
                request.description(),
                admin.userId()
        );
    }

    @PostMapping("/{userId}/debit")
    public CoinTransactionResponse debit(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminWalletOperationRequest request
    ) {
        var admin = SecurityUtils.getCurrentUser();
        return walletService.adminDebit(
                userId,
                admin.gameSessionId(),
                request.amount(),
                request.description(),
                admin.userId()
        );
    }
}
