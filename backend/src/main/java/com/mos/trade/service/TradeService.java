package com.mos.trade.service;

import com.mos.auction.service.AuctionModeService;
import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.InsufficientBalanceException;
import com.mos.common.exception.BusinessException;
import com.mos.common.exception.ItemNotFoundException;
import com.mos.common.exception.ItemSessionMismatchException;
import com.mos.common.exception.TradeInsufficientCoinsException;
import com.mos.common.exception.TradeInvalidStateException;
import com.mos.common.exception.TradeItemNotOwnedException;
import com.mos.common.exception.TradeNotFoundException;
import com.mos.common.exception.TradeNotParticipantException;
import com.mos.item.entity.PlayerItem;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.item.service.ItemService;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.trade.dto.CreateTradeRequest;
import com.mos.trade.dto.TradeResponse;
import com.mos.trade.entity.Trade;
import com.mos.trade.entity.TradeCoin;
import com.mos.trade.entity.TradeItem;
import com.mos.trade.enums.TradeStatus;
import com.mos.trade.repository.TradeCoinRepository;
import com.mos.trade.repository.TradeItemRepository;
import com.mos.trade.repository.TradeRepository;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.repository.WalletRepository;
import com.mos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeItemRepository tradeItemRepository;
    private final TradeCoinRepository tradeCoinRepository;
    private final PlayerItemRepository playerItemRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final WalletRepository walletRepository;
    private final ItemService itemService;
    private final WalletService walletService;
    private final AuditService auditService;
    private final AuctionModeService auctionModeService;

    @Transactional
    public TradeResponse createTrade(UUID initiatorId, UUID gameSessionId, CreateTradeRequest request) {
        auctionModeService.ensureDisabled(gameSessionId);

        UUID receiverId = request.receiverId();

        if (initiatorId.equals(receiverId)) {
            throw new BusinessException("Cannot create a trade with yourself");
        }

        ensureParticipant(initiatorId, gameSessionId);
        ensureParticipant(receiverId, gameSessionId);

        List<UUID> initiatorItemIds = normalizeIds(request.initiatorItemIds());
        List<UUID> receiverItemIds = normalizeIds(request.receiverItemIds());
        long initiatorCoins = normalizeCoins(request.initiatorCoins());
        long receiverCoins = normalizeCoins(request.receiverCoins());

        if (initiatorItemIds.isEmpty() && receiverItemIds.isEmpty() && initiatorCoins == 0 && receiverCoins == 0) {
            throw new BusinessException("Trade must include at least one item or coin offer");
        }

        validateItemOwnership(initiatorItemIds, initiatorId, gameSessionId);
        validateItemOwnership(receiverItemIds, receiverId, gameSessionId);
        ensureSufficientCoins(initiatorId, gameSessionId, initiatorCoins);

        Trade trade = tradeRepository.save(Trade.builder()
                .gameSessionId(gameSessionId)
                .initiatorId(initiatorId)
                .receiverId(receiverId)
                .status(TradeStatus.PENDING)
                .build());

        saveTradeItems(trade, initiatorItemIds, initiatorId);
        saveTradeItems(trade, receiverItemIds, receiverId);
        saveTradeCoins(trade, initiatorId, initiatorCoins);
        saveTradeCoins(trade, receiverId, receiverCoins);

        auditService.log(
                initiatorId,
                gameSessionId,
                AuditAction.TRADE_CREATE,
                "Trade",
                trade.getId().toString(),
                "Trade created",
                Map.of(
                        "tradeId", trade.getId().toString(),
                        "receiverId", receiverId.toString(),
                        "initiatorItemCount", initiatorItemIds.size(),
                        "receiverItemCount", receiverItemIds.size(),
                        "initiatorCoins", initiatorCoins,
                        "receiverCoins", receiverCoins
                )
        );

        return toResponse(trade);
    }

    @Transactional
    public TradeResponse acceptTrade(UUID tradeId, UUID receiverId, UUID gameSessionId) {
        auctionModeService.ensureDisabled(gameSessionId);

        Trade trade = getTradeForParticipant(tradeId, receiverId, gameSessionId);

        if (!trade.getReceiverId().equals(receiverId)) {
            throw new TradeNotParticipantException();
        }
        ensurePending(trade);

        List<TradeItem> items = tradeItemRepository.findByTradeIdWithItems(trade.getId());
        List<TradeCoin> coins = tradeCoinRepository.findByTradeId(trade.getId());

        validateTradeItemsOwnership(items, gameSessionId);
        validateTradeCoinsBalances(coins, gameSessionId);

        for (TradeItem tradeItem : items) {
            UUID fromUserId = tradeItem.getOwnerId();
            UUID toUserId = fromUserId.equals(trade.getInitiatorId())
                    ? trade.getReceiverId()
                    : trade.getInitiatorId();
            itemService.transferItem(tradeItem.getPlayerItem().getId(), fromUserId, toUserId, gameSessionId);
        }

        try {
            transferTradeCoins(trade, coins, receiverId, gameSessionId);
        } catch (InsufficientBalanceException ex) {
            throw new TradeInsufficientCoinsException();
        }

        trade.setStatus(TradeStatus.ACCEPTED);
        tradeRepository.save(trade);

        auditService.log(
                receiverId,
                gameSessionId,
                AuditAction.TRADE_ACCEPT,
                "Trade",
                trade.getId().toString(),
                "Trade accepted",
                Map.of(
                        "tradeId", trade.getId().toString(),
                        "initiatorId", trade.getInitiatorId().toString(),
                        "receiverId", trade.getReceiverId().toString()
                )
        );

        return toResponse(trade);
    }

    @Transactional
    public TradeResponse declineTrade(UUID tradeId, UUID receiverId, UUID gameSessionId) {
        Trade trade = getTradeForParticipant(tradeId, receiverId, gameSessionId);

        if (!trade.getReceiverId().equals(receiverId)) {
            throw new TradeNotParticipantException();
        }
        ensurePending(trade);

        trade.setStatus(TradeStatus.DECLINED);
        tradeRepository.save(trade);

        auditService.log(
                receiverId,
                gameSessionId,
                AuditAction.TRADE_DECLINE,
                "Trade",
                trade.getId().toString(),
                "Trade declined",
                Map.of("tradeId", trade.getId().toString())
        );

        return toResponse(trade);
    }

    @Transactional
    public TradeResponse cancelTrade(UUID tradeId, UUID initiatorId, UUID gameSessionId) {
        Trade trade = getTradeForParticipant(tradeId, initiatorId, gameSessionId);

        if (!trade.getInitiatorId().equals(initiatorId)) {
            throw new TradeNotParticipantException();
        }
        ensurePending(trade);

        trade.setStatus(TradeStatus.CANCELLED);
        tradeRepository.save(trade);

        auditService.log(
                initiatorId,
                gameSessionId,
                AuditAction.TRADE_CANCEL,
                "Trade",
                trade.getId().toString(),
                "Trade cancelled",
                Map.of("tradeId", trade.getId().toString())
        );

        return toResponse(trade);
    }

    @Transactional(readOnly = true)
    public List<TradeResponse> getMyTrades(UUID userId, UUID gameSessionId) {
        return tradeRepository.findByGameSessionIdAndParticipant(gameSessionId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeResponse getTrade(UUID tradeId, UUID userId, UUID gameSessionId) {
        Trade trade = getTradeForParticipant(tradeId, userId, gameSessionId);
        return toResponse(trade);
    }

    private void transferTradeCoins(Trade trade, List<TradeCoin> coins, UUID performedByUserId, UUID gameSessionId) {
        String referenceId = trade.getId().toString();

        for (TradeCoin coin : coins) {
            UUID fromUserId = coin.getUserId();
            UUID toUserId = fromUserId.equals(trade.getInitiatorId())
                    ? trade.getReceiverId()
                    : trade.getInitiatorId();
            long amount = coin.getAmount().longValue();

            walletService.debit(
                    fromUserId,
                    gameSessionId,
                    amount,
                    CoinTransactionType.TRADE,
                    referenceId,
                    "Trade debit: " + trade.getId(),
                    performedByUserId,
                    AuditAction.COIN_DEBIT
            );
            walletService.credit(
                    toUserId,
                    gameSessionId,
                    amount,
                    CoinTransactionType.TRADE,
                    referenceId,
                    "Trade credit: " + trade.getId(),
                    performedByUserId,
                    AuditAction.COIN_CREDIT
            );
        }
    }

    private void validateTradeCoinsBalances(List<TradeCoin> coins, UUID gameSessionId) {
        for (TradeCoin coin : coins) {
            ensureSufficientCoins(coin.getUserId(), gameSessionId, coin.getAmount().longValue());
        }
    }

    private void validateTradeItemsOwnership(List<TradeItem> items, UUID gameSessionId) {
        for (TradeItem tradeItem : items) {
            PlayerItem playerItem = tradeItem.getPlayerItem();
            if (!playerItem.getGameSessionId().equals(gameSessionId)) {
                throw new ItemSessionMismatchException();
            }
            if (!playerItem.getOwnerId().equals(tradeItem.getOwnerId())) {
                throw new TradeItemNotOwnedException();
            }
        }
    }

    private void saveTradeItems(Trade trade, List<UUID> playerItemIds, UUID ownerId) {
        for (UUID playerItemId : playerItemIds) {
            PlayerItem playerItem = playerItemRepository.findByIdAndGameSessionId(playerItemId, trade.getGameSessionId())
                    .orElseThrow(ItemNotFoundException::new);
            tradeItemRepository.save(TradeItem.builder()
                    .trade(trade)
                    .playerItem(playerItem)
                    .ownerId(ownerId)
                    .build());
        }
    }

    private void saveTradeCoins(Trade trade, UUID userId, long amount) {
        if (amount > 0) {
            tradeCoinRepository.save(TradeCoin.builder()
                    .trade(trade)
                    .userId(userId)
                    .amount(Math.toIntExact(amount))
                    .build());
        }
    }

    private void validateItemOwnership(List<UUID> playerItemIds, UUID ownerId, UUID gameSessionId) {
        for (UUID playerItemId : playerItemIds) {
            PlayerItem playerItem = playerItemRepository.findByIdAndGameSessionId(playerItemId, gameSessionId)
                    .orElseThrow(ItemNotFoundException::new);

            if (!playerItem.getOwnerId().equals(ownerId)) {
                throw new TradeItemNotOwnedException();
            }
        }
    }

    private void ensureSufficientCoins(UUID userId, UUID gameSessionId, long amount) {
        if (amount <= 0) {
            return;
        }

        long balance = walletRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .map(wallet -> wallet.getBalance())
                .orElse(0L);

        if (balance < amount) {
            throw new TradeInsufficientCoinsException();
        }
    }

    private Trade getTradeForParticipant(UUID tradeId, UUID userId, UUID gameSessionId) {
        Trade trade = tradeRepository.findByIdAndGameSessionId(tradeId, gameSessionId)
                .orElseThrow(TradeNotFoundException::new);

        if (!trade.getInitiatorId().equals(userId) && !trade.getReceiverId().equals(userId)) {
            throw new TradeNotParticipantException();
        }

        return trade;
    }

    private void ensurePending(Trade trade) {
        if (trade.getStatus() != TradeStatus.PENDING) {
            throw new TradeInvalidStateException();
        }
    }

    private void ensureParticipant(UUID userId, UUID gameSessionId) {
        sessionParticipantRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseThrow(() -> new BusinessException("User is not a participant of this game session"));
    }

    private TradeResponse toResponse(Trade trade) {
        List<TradeItem> items = tradeItemRepository.findByTradeIdWithItems(trade.getId());
        List<TradeCoin> coins = tradeCoinRepository.findByTradeId(trade.getId());
        return TradeResponse.from(trade, items, coins);
    }

    private List<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        return new ArrayList<>(ids);
    }

    private long normalizeCoins(Long coins) {
        return coins != null ? coins : 0L;
    }
}
