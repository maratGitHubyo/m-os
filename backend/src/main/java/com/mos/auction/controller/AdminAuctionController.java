package com.mos.auction.controller;

import com.mos.auction.dto.AuctionLotResponse;
import com.mos.auction.dto.AuctionStateResponse;
import com.mos.auction.dto.CreateAuctionLotRequest;
import com.mos.auction.dto.SetAuctionModeRequest;
import com.mos.auction.service.AuctionService;
import com.mos.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/auction")
@RequiredArgsConstructor
@Tag(name = "Admin Auction")
public class AdminAuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public AuctionStateResponse getState() {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.getAdminState(admin.gameSessionId());
    }

    @PostMapping("/mode")
    public AuctionStateResponse setMode(@Valid @RequestBody SetAuctionModeRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.setAuctionMode(admin.gameSessionId(), request.enabled(), admin.userId());
    }

    @PostMapping("/lots")
    public AuctionLotResponse createLot(@Valid @RequestBody CreateAuctionLotRequest request) {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.createLot(admin.gameSessionId(), request, admin.userId());
    }

    @PostMapping("/lots/{id}/open")
    public AuctionLotResponse openLot(@PathVariable UUID id) {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.openLot(id, admin.gameSessionId(), admin.userId());
    }

    @PostMapping("/lots/{id}/sell")
    public AuctionLotResponse sellLot(@PathVariable UUID id) {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.sellLot(id, admin.gameSessionId(), admin.userId());
    }

    @PostMapping("/lots/{id}/cancel")
    public AuctionLotResponse cancelLot(@PathVariable UUID id) {
        var admin = SecurityUtils.getCurrentUser();
        return auctionService.cancelLot(id, admin.gameSessionId(), admin.userId());
    }
}
