package com.mos.auction.controller;

import com.mos.auction.dto.AuctionLotResponse;
import com.mos.auction.dto.AuctionStateResponse;
import com.mos.auction.dto.PlaceBidRequest;
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
@RequestMapping("/api/auction")
@RequiredArgsConstructor
@Tag(name = "Auction")
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public AuctionStateResponse getState() {
        var currentUser = SecurityUtils.getCurrentUser();
        return auctionService.getPlayerState(currentUser.gameSessionId());
    }

    @PostMapping("/lots/{id}/bids")
    public AuctionLotResponse placeBid(
            @PathVariable UUID id,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        var currentUser = SecurityUtils.getCurrentUser();
        return auctionService.placeBid(id, currentUser.userId(), currentUser.gameSessionId(), request);
    }
}
