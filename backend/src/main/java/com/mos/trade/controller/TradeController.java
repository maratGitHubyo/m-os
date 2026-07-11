package com.mos.trade.controller;

import com.mos.security.SecurityUtils;
import com.mos.trade.dto.CreateTradeRequest;
import com.mos.trade.dto.TradeResponse;
import com.mos.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "Trades", description = "Player trade offers")
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "Create trade offer")
    @PostMapping
    public TradeResponse createTrade(@Valid @RequestBody CreateTradeRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.createTrade(currentUser.userId(), currentUser.gameSessionId(), request);
    }

    @Operation(summary = "List my trades")
    @GetMapping
    public List<TradeResponse> getMyTrades() {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.getMyTrades(currentUser.userId(), currentUser.gameSessionId());
    }

    @Operation(summary = "Get trade by id")
    @GetMapping("/{id}")
    public TradeResponse getTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.getTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @Operation(summary = "Accept trade")
    @PostMapping("/{id}/accept")
    public TradeResponse acceptTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.acceptTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @Operation(summary = "Decline trade")
    @PostMapping("/{id}/decline")
    public TradeResponse declineTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.declineTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @Operation(summary = "Cancel trade")
    @PostMapping("/{id}/cancel")
    public TradeResponse cancelTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.cancelTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }
}
