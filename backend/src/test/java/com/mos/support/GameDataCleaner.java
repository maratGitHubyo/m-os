package com.mos.support;

import com.mos.auction.repository.AuctionBidRepository;
import com.mos.auction.repository.AuctionLotRepository;
import com.mos.common.audit.repository.AuditLogRepository;
import com.mos.event.repository.GameEventRepository;
import com.mos.item.repository.ItemOwnershipHistoryRepository;
import com.mos.item.repository.ItemTemplateRepository;
import com.mos.item.repository.PlayerItemRepository;
import com.mos.location.repository.LocationPointRepository;
import com.mos.location.repository.PlayerLocationDiscoveryRepository;
import com.mos.numbers.repository.CollectibleNumberRepository;
import com.mos.numbers.repository.PlayerNumberRepository;
import com.mos.qrcode.repository.QrCodeRepository;
import com.mos.qrcode.repository.QrScanRepository;
import com.mos.quest.repository.PlayerQuestRepository;
import com.mos.quest.repository.QuestRepository;
import com.mos.secret.repository.PlayerSecretRepository;
import com.mos.seed.DemoSeedConstants;
import com.mos.session.entity.GameSessionStatus;
import com.mos.session.repository.GameConfigRepository;
import com.mos.session.repository.GameSessionRepository;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.trade.repository.TradeCoinRepository;
import com.mos.trade.repository.TradeItemRepository;
import com.mos.trade.repository.TradeRepository;
import com.mos.user.repository.UserRepository;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import com.mos.wallet.transfer.repository.CoinTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clears mutable game data before integration tests.
 * Preserves V3/V25 host user (marat) and session baseline.
 */
@Component
@RequiredArgsConstructor
public class GameDataCleaner {

    private final AuctionBidRepository auctionBidRepository;
    private final AuctionLotRepository auctionLotRepository;
    private final TradeItemRepository tradeItemRepository;
    private final TradeCoinRepository tradeCoinRepository;
    private final TradeRepository tradeRepository;
    private final PlayerQuestRepository playerQuestRepository;
    private final QuestRepository questRepository;
    private final PlayerNumberRepository playerNumberRepository;
    private final CollectibleNumberRepository collectibleNumberRepository;
    private final PlayerSecretRepository playerSecretRepository;
    private final QrScanRepository qrScanRepository;
    private final QrCodeRepository qrCodeRepository;
    private final PlayerLocationDiscoveryRepository playerLocationDiscoveryRepository;
    private final LocationPointRepository locationPointRepository;
    private final ItemOwnershipHistoryRepository itemOwnershipHistoryRepository;
    private final PlayerItemRepository playerItemRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final CoinTransferRepository coinTransferRepository;
    private final WalletRepository walletRepository;
    private final GameEventRepository gameEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameConfigRepository gameConfigRepository;

    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Transactional
    public void clean() {
        auctionBidRepository.deleteAllInBatch();
        auctionLotRepository.deleteAllInBatch();
        tradeItemRepository.deleteAllInBatch();
        tradeCoinRepository.deleteAllInBatch();
        tradeRepository.deleteAllInBatch();
        playerQuestRepository.deleteAllInBatch();
        questRepository.deleteAllInBatch();
        playerNumberRepository.deleteAllInBatch();
        collectibleNumberRepository.deleteAllInBatch();
        playerSecretRepository.deleteAllInBatch();
        qrScanRepository.deleteAllInBatch();
        qrCodeRepository.deleteAllInBatch();
        playerLocationDiscoveryRepository.deleteAllInBatch();
        locationPointRepository.deleteAllInBatch();
        itemOwnershipHistoryRepository.deleteAllInBatch();
        playerItemRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
        coinTransactionRepository.deleteAllInBatch();
        coinTransferRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
        gameEventRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        removeDemoUsers();
        resetSessionBaseline();
    }

    private void removeDemoUsers() {
        List<String> usernames = new ArrayList<>();
        DemoSeedConstants.PARTY_PLAYERS.forEach(account -> usernames.add(account.username()));
        usernames.add("alice");
        usernames.add("bob");
        usernames.add("admin");

        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(user -> {
                sessionParticipantRepository.findByUserId(user.getId())
                        .forEach(sessionParticipantRepository::delete);
                userRepository.delete(user);
            });
        }
    }

    private void resetSessionBaseline() {
        gameSessionRepository.findById(SESSION_ID).ifPresent(session -> {
            session.setName("M-OS Dev Session");
            session.setStatus(GameSessionStatus.STARTING);
            gameSessionRepository.save(session);
        });
        gameConfigRepository.findByGameSessionId(SESSION_ID).ifPresent(config -> {
            config.setNumbersTotal(50);
            config.setAuctionModeEnabled(false);
            gameConfigRepository.save(config);
        });
    }
}
