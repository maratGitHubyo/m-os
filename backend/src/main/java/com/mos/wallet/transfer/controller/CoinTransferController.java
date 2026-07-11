package com.mos.wallet.transfer.controller;

import com.mos.security.SecurityUtils;
import com.mos.wallet.transfer.dto.CoinTransferResponse;
import com.mos.wallet.transfer.dto.CreateCoinTransferRequest;
import com.mos.wallet.transfer.service.CoinTransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class CoinTransferController {

    private final CoinTransferService coinTransferService;

    @PostMapping("/transfer")
    public CoinTransferResponse transfer(@Valid @RequestBody CreateCoinTransferRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        return coinTransferService.transfer(
                currentUser.userId(),
                request,
                currentUser.gameSessionId()
        );
    }

    @GetMapping("/transfers")
    public Page<CoinTransferResponse> getTransfers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        var currentUser = SecurityUtils.getCurrentUser();
        return coinTransferService.getTransfers(
                currentUser.userId(),
                currentUser.gameSessionId(),
                pageable
        );
    }
}
