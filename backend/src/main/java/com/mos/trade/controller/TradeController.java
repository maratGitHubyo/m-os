package com.mos.trade.controller;

import com.mos.trade.dto.CreateTradeRequest;
import com.mos.trade.dto.TradeResponse;
import com.mos.trade.service.TradeService;
import com.mos.security.SecurityUtils;
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
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    public TradeResponse createTrade(@Valid @RequestBody CreateTradeRequest request) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.createTrade(currentUser.userId(), currentUser.gameSessionId(), request);
    }

    @GetMapping
    public List<TradeResponse> getMyTrades() {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.getMyTrades(currentUser.userId(), currentUser.gameSessionId());
    }

    @GetMapping("/{id}")
    public TradeResponse getTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.getTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @PostMapping("/{id}/accept")
    public TradeResponse acceptTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.acceptTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @PostMapping("/{id}/decline")
    public TradeResponse declineTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.declineTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }

    @PostMapping("/{id}/cancel")
    public TradeResponse cancelTrade(@PathVariable UUID id) {
        var currentUser = SecurityUtils.getCurrentUser();
        return tradeService.cancelTrade(id, currentUser.userId(), currentUser.gameSessionId());
    }
}
