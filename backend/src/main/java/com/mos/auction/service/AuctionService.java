package com.mos.auction.service;

import com.mos.auction.dto.AuctionBidResponse;
import com.mos.auction.dto.AuctionBroadcastMessage;
import com.mos.auction.dto.AuctionLotResponse;
import com.mos.auction.dto.AuctionStateResponse;
import com.mos.auction.dto.CreateAuctionLotRequest;
import com.mos.auction.dto.PlaceBidRequest;
import com.mos.auction.entity.AuctionBid;
import com.mos.auction.entity.AuctionLot;
import com.mos.auction.enums.AuctionLotStatus;
import com.mos.auction.repository.AuctionBidRepository;
import com.mos.auction.repository.AuctionLotRepository;
import com.mos.auction.websocket.AuctionWebSocketPublisher;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.AuctionBidTooLowException;
import com.mos.common.exception.AuctionLotInvalidStateException;
import com.mos.common.exception.AuctionLotNotFoundException;
import com.mos.common.exception.AuctionNoBidsException;
import com.mos.common.exception.CoinTransferUserNotInSessionException;
import com.mos.common.exception.InsufficientBalanceException;
import com.mos.session.entity.GameConfig;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.wallet.entity.Wallet;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.repository.WalletRepository;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionLotRepository auctionLotRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final AuctionModeService auctionModeService;
    private final AuctionWebSocketPublisher auctionWebSocketPublisher;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final GameConfigRepository gameConfigRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    @Transactional
    public AuctionStateResponse setAuctionMode(UUID gameSessionId, boolean enabled, UUID adminUserId) {
        GameConfig config = auctionModeService.requireConfig(gameSessionId);
        boolean previous = Boolean.TRUE.equals(config.getAuctionModeEnabled());
        config.setAuctionModeEnabled(enabled);
        gameConfigRepository.save(config);

        auditService.log(
                adminUserId,
                gameSessionId,
                AuditAction.AUCTION_MODE_CHANGE,
                "GameConfig",
                config.getId().toString(),
                enabled ? "Auction mode enabled" : "Auction mode disabled",
                Map.of(
                        "previous", previous,
                        "enabled", enabled
                )
        );

        auctionWebSocketPublisher.publish(new AuctionBroadcastMessage(
                "MODE",
                gameSessionId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                enabled,
                Instant.now()
        ));

        return getAdminState(gameSessionId);
    }

    @Transactional(readOnly = true)
    public AuctionStateResponse getAdminState(UUID gameSessionId) {
        boolean enabled = auctionModeService.isEnabled(gameSessionId);
        Map<UUID, String> nicknames = nicknamesByUserId(gameSessionId);
        List<AuctionLotResponse> lots = auctionLotRepository.findByGameSessionIdOrderByCreatedAtDesc(gameSessionId)
                .stream()
                .map(lot -> toLotResponse(lot, nicknames, true))
                .toList();
        AuctionLotResponse openLot = lots.stream()
                .filter(lot -> lot.status() == AuctionLotStatus.OPEN)
                .findFirst()
                .orElse(null);
        return new AuctionStateResponse(enabled, openLot, lots);
    }

    @Transactional(readOnly = true)
    public AuctionStateResponse getPlayerState(UUID gameSessionId) {
        auctionModeService.ensureEnabled(gameSessionId);
        Map<UUID, String> nicknames = nicknamesByUserId(gameSessionId);
        List<AuctionLot> lots = auctionLotRepository.findByGameSessionIdOrderByCreatedAtDesc(gameSessionId);
        AuctionLotResponse openLot = lots.stream()
                .filter(lot -> lot.getStatus() == AuctionLotStatus.OPEN)
                .findFirst()
                .map(lot -> toLotResponse(lot, nicknames, true))
                .orElse(null);
        List<AuctionLotResponse> history = lots.stream()
                .filter(lot -> lot.getStatus() == AuctionLotStatus.SOLD || lot.getStatus() == AuctionLotStatus.CANCELLED)
                .limit(10)
                .map(lot -> toLotResponse(lot, nicknames, false))
                .toList();
        return new AuctionStateResponse(true, openLot, history);
    }

    @Transactional
    public AuctionLotResponse createLot(UUID gameSessionId, CreateAuctionLotRequest request, UUID adminUserId) {
        long startingPrice = request.startingPrice() != null ? request.startingPrice() : 50L;
        long minBidIncrement = request.minBidIncrement() != null ? request.minBidIncrement() : 50L;

        AuctionLot lot = auctionLotRepository.save(AuctionLot.builder()
                .gameSessionId(gameSessionId)
                .title(request.title().trim())
                .startingPrice(startingPrice)
                .minBidIncrement(minBidIncrement)
                .status(AuctionLotStatus.DRAFT)
                .build());

        auditService.log(
                adminUserId,
                gameSessionId,
                AuditAction.AUCTION_LOT_CREATE,
                "AuctionLot",
                lot.getId().toString(),
                "Auction lot created",
                Map.of(
                        "title", lot.getTitle(),
                        "startingPrice", startingPrice,
                        "minBidIncrement", minBidIncrement
                )
        );

        return toLotResponse(lot, nicknamesByUserId(gameSessionId), false);
    }

    @Transactional
    public AuctionLotResponse openLot(UUID lotId, UUID gameSessionId, UUID adminUserId) {
        auctionModeService.ensureEnabled(gameSessionId);

        if (auctionLotRepository.existsByGameSessionIdAndStatus(gameSessionId, AuctionLotStatus.OPEN)) {
            throw new AuctionLotInvalidStateException();
        }

        AuctionLot lot = getLot(lotId, gameSessionId);
        if (lot.getStatus() != AuctionLotStatus.DRAFT) {
            throw new AuctionLotInvalidStateException();
        }

        lot.setStatus(AuctionLotStatus.OPEN);
        lot.setOpenedAt(Instant.now());
        lot.setCurrentPrice(null);
        lot.setCurrentLeaderId(null);
        auctionLotRepository.save(lot);

        auditService.log(
                adminUserId,
                gameSessionId,
                AuditAction.AUCTION_LOT_OPEN,
                "AuctionLot",
                lot.getId().toString(),
                "Auction lot opened",
                Map.of("title", lot.getTitle())
        );

        AuctionLotResponse response = toLotResponse(lot, nicknamesByUserId(gameSessionId), true);
        publishLotUpdate("LOT_OPEN", gameSessionId, response, null);
        return response;
    }

    @Transactional
    public AuctionLotResponse sellLot(UUID lotId, UUID gameSessionId, UUID adminUserId) {
        auctionModeService.ensureEnabled(gameSessionId);

        AuctionLot lot = getLot(lotId, gameSessionId);
        if (lot.getStatus() != AuctionLotStatus.OPEN) {
            throw new AuctionLotInvalidStateException();
        }
        if (lot.getCurrentLeaderId() == null || lot.getCurrentPrice() == null) {
            throw new AuctionNoBidsException();
        }

        UUID winnerId = lot.getCurrentLeaderId();
        long finalPrice = lot.getCurrentPrice();

        Wallet winnerWallet = walletRepository.findByUserIdAndGameSessionId(winnerId, gameSessionId)
                .orElseThrow(InsufficientBalanceException::new);
        if (winnerWallet.getBalance() < finalPrice) {
            throw new InsufficientBalanceException();
        }

        walletService.debit(
                winnerId,
                gameSessionId,
                finalPrice,
                CoinTransactionType.AUCTION,
                lot.getId().toString(),
                "Auction won: " + lot.getTitle(),
                adminUserId,
                AuditAction.COIN_DEBIT
        );

        lot.setStatus(AuctionLotStatus.SOLD);
        lot.setWinnerUserId(winnerId);
        lot.setFinalPrice(finalPrice);
        lot.setClosedAt(Instant.now());
        auctionLotRepository.save(lot);

        auditService.log(
                adminUserId,
                gameSessionId,
                AuditAction.AUCTION_LOT_SELL,
                "AuctionLot",
                lot.getId().toString(),
                "Auction lot sold",
                Map.of(
                        "title", lot.getTitle(),
                        "winnerUserId", winnerId.toString(),
                        "finalPrice", finalPrice
                )
        );

        AuctionLotResponse response = toLotResponse(lot, nicknamesByUserId(gameSessionId), true);
        publishLotUpdate("LOT_SOLD", gameSessionId, response, null);
        return response;
    }

    @Transactional
    public AuctionLotResponse cancelLot(UUID lotId, UUID gameSessionId, UUID adminUserId) {
        AuctionLot lot = getLot(lotId, gameSessionId);
        if (lot.getStatus() != AuctionLotStatus.DRAFT && lot.getStatus() != AuctionLotStatus.OPEN) {
            throw new AuctionLotInvalidStateException();
        }

        lot.setStatus(AuctionLotStatus.CANCELLED);
        lot.setClosedAt(Instant.now());
        auctionLotRepository.save(lot);

        auditService.log(
                adminUserId,
                gameSessionId,
                AuditAction.AUCTION_LOT_CANCEL,
                "AuctionLot",
                lot.getId().toString(),
                "Auction lot cancelled",
                Map.of("title", lot.getTitle())
        );

        AuctionLotResponse response = toLotResponse(lot, nicknamesByUserId(gameSessionId), true);
        publishLotUpdate("LOT_CANCELLED", gameSessionId, response, null);
        return response;
    }

    @Transactional
    public AuctionLotResponse placeBid(UUID lotId, UUID bidderUserId, UUID gameSessionId, PlaceBidRequest request) {
        auctionModeService.ensureEnabled(gameSessionId);
        ensureParticipant(bidderUserId, gameSessionId);

        AuctionLot lot = getLot(lotId, gameSessionId);
        if (lot.getStatus() != AuctionLotStatus.OPEN) {
            throw new AuctionLotInvalidStateException();
        }

        long amount = request.amount();
        long minimum = lot.getCurrentPrice() == null
                ? Math.max(lot.getStartingPrice(), 1L)
                : lot.getCurrentPrice() + lot.getMinBidIncrement();
        if (amount < minimum) {
            throw new AuctionBidTooLowException();
        }

        Wallet wallet = walletRepository.findByUserIdAndGameSessionId(bidderUserId, gameSessionId)
                .orElseThrow(InsufficientBalanceException::new);
        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException();
        }

        AuctionBid bid = auctionBidRepository.save(AuctionBid.builder()
                .lotId(lot.getId())
                .gameSessionId(gameSessionId)
                .bidderUserId(bidderUserId)
                .amount(amount)
                .build());

        lot.setCurrentPrice(amount);
        lot.setCurrentLeaderId(bidderUserId);
        auctionLotRepository.save(lot);

        Map<UUID, String> nicknames = nicknamesByUserId(gameSessionId);
        String bidderNickname = nicknames.get(bidderUserId);

        auditService.log(
                bidderUserId,
                gameSessionId,
                AuditAction.AUCTION_BID,
                "AuctionBid",
                bid.getId().toString(),
                "Auction bid placed",
                Map.of(
                        "lotId", lot.getId().toString(),
                        "amount", amount
                )
        );

        AuctionLotResponse response = toLotResponse(lot, nicknames, true);
        publishLotUpdate("BID", gameSessionId, response, AuctionBidResponse.from(bid, bidderNickname));
        return response;
    }

    private AuctionLot getLot(UUID lotId, UUID gameSessionId) {
        return auctionLotRepository.findByIdAndGameSessionId(lotId, gameSessionId)
                .orElseThrow(AuctionLotNotFoundException::new);
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(CoinTransferUserNotInSessionException::new);
    }

    private Map<UUID, String> nicknamesByUserId(UUID gameSessionId) {
        return sessionParticipantRepository.findByGameSessionId(gameSessionId).stream()
                .collect(Collectors.toMap(
                        participant -> participant.getUser().getId(),
                        SessionParticipant::getNicknameSnapshot,
                        (left, right) -> left,
                        HashMap::new
                ));
    }

    private AuctionLotResponse toLotResponse(AuctionLot lot, Map<UUID, String> nicknames, boolean includeBids) {
        List<AuctionBidResponse> recentBids = includeBids
                ? auctionBidRepository.findTop20ByLotIdOrderByCreatedAtDesc(lot.getId()).stream()
                .map(bid -> AuctionBidResponse.from(bid, nicknames.get(bid.getBidderUserId())))
                .toList()
                : List.of();

        return AuctionLotResponse.from(
                lot,
                lot.getCurrentLeaderId() != null ? nicknames.get(lot.getCurrentLeaderId()) : null,
                lot.getWinnerUserId() != null ? nicknames.get(lot.getWinnerUserId()) : null,
                recentBids
        );
    }

    private void publishLotUpdate(
            String type,
            UUID gameSessionId,
            AuctionLotResponse lot,
            AuctionBidResponse bid
    ) {
        auctionWebSocketPublisher.publish(new AuctionBroadcastMessage(
                type,
                gameSessionId,
                lot.id(),
                lot.title(),
                lot.status(),
                lot.currentPrice(),
                lot.currentLeaderId(),
                lot.currentLeaderNickname(),
                lot.nextMinBid(),
                bid != null ? bid.amount() : null,
                bid != null ? bid.bidderUserId() : null,
                bid != null ? bid.bidderNickname() : null,
                auctionModeService.isEnabled(gameSessionId),
                Instant.now()
        ));
    }
}
