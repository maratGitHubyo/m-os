package com.mos.wallet.service;

import com.mos.common.audit.enums.AuditAction;
import com.mos.common.audit.service.AuditService;
import com.mos.common.exception.ConcurrentModificationException;
import com.mos.common.exception.InsufficientBalanceException;
import com.mos.common.exception.InvalidAmountException;
import com.mos.wallet.dto.CoinTransactionResponse;
import com.mos.wallet.dto.WalletResponse;
import com.mos.wallet.entity.CoinTransaction;
import com.mos.wallet.entity.Wallet;
import com.mos.wallet.enums.CoinTransactionType;
import com.mos.wallet.repository.CoinTransactionRepository;
import com.mos.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId, UUID gameSessionId) {
        return WalletResponse.from(getOrCreateWallet(userId, gameSessionId));
    }

    @Transactional(readOnly = true)
    public Page<CoinTransactionResponse> getTransactions(UUID userId, UUID gameSessionId, Pageable pageable) {
        getOrCreateWallet(userId, gameSessionId);
        return coinTransactionRepository.findByUserIdAndGameSessionIdOrderByCreatedAtDesc(userId, gameSessionId, pageable)
                .map(CoinTransactionResponse::from);
    }

    @Transactional
    public CoinTransactionResponse adminCredit(
            UUID targetUserId,
            UUID gameSessionId,
            Long amount,
            String description,
            UUID adminUserId
    ) {
        return CoinTransactionResponse.from(
                applyBalanceChange(targetUserId, gameSessionId, amount, CoinTransactionType.ADMIN, description, adminUserId, AuditAction.COIN_CREDIT)
        );
    }

    @Transactional
    public CoinTransactionResponse adminDebit(
            UUID targetUserId,
            UUID gameSessionId,
            Long amount,
            String description,
            UUID adminUserId
    ) {
        return CoinTransactionResponse.from(
                applyBalanceChange(targetUserId, gameSessionId, -amount, CoinTransactionType.ADMIN, description, adminUserId, AuditAction.COIN_DEBIT)
        );
    }

    @Transactional
    public CoinTransaction credit(
            UUID userId,
            UUID gameSessionId,
            Long amount,
            CoinTransactionType type,
            String referenceId,
            String description,
            UUID performedByUserId,
            AuditAction auditAction
    ) {
        validateAmount(amount);
        return applyBalanceChange(userId, gameSessionId, amount, type, description, performedByUserId, auditAction, referenceId);
    }

    @Transactional
    public CoinTransaction debit(
            UUID userId,
            UUID gameSessionId,
            Long amount,
            CoinTransactionType type,
            String referenceId,
            String description,
            UUID performedByUserId,
            AuditAction auditAction
    ) {
        validateAmount(amount);
        return applyBalanceChange(userId, gameSessionId, -amount, type, description, performedByUserId, auditAction, referenceId);
    }

    @Transactional
    public CoinTransaction creditWithoutAudit(
            UUID userId,
            UUID gameSessionId,
            Long amount,
            CoinTransactionType type,
            String referenceId,
            String description
    ) {
        validateAmount(amount);
        return applyBalanceChangeWithoutAudit(userId, gameSessionId, amount, type, description, referenceId);
    }

    private CoinTransaction applyBalanceChangeWithoutAudit(
            UUID userId,
            UUID gameSessionId,
            long signedAmount,
            CoinTransactionType type,
            String description,
            String referenceId
    ) {
        if (signedAmount == 0) {
            throw new InvalidAmountException();
        }
        if (signedAmount > 0) {
            validateAmount(signedAmount);
        } else {
            validateAmount(-signedAmount);
        }

        Wallet wallet = getOrCreateWallet(userId, gameSessionId);
        long newBalance = wallet.getBalance() + signedAmount;

        if (newBalance < 0) {
            throw new InsufficientBalanceException();
        }

        wallet.setBalance(newBalance);

        try {
            walletRepository.save(wallet);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException();
        }

        return coinTransactionRepository.save(CoinTransaction.builder()
                .userId(userId)
                .gameSessionId(gameSessionId)
                .amount(signedAmount)
                .type(type)
                .referenceId(referenceId)
                .description(description)
                .build());
    }

    private CoinTransaction applyBalanceChange(
            UUID userId,
            UUID gameSessionId,
            long signedAmount,
            CoinTransactionType type,
            String description,
            UUID performedByUserId,
            AuditAction auditAction
    ) {
        return applyBalanceChange(userId, gameSessionId, signedAmount, type, description, performedByUserId, auditAction, null);
    }

    private CoinTransaction applyBalanceChange(
            UUID userId,
            UUID gameSessionId,
            long signedAmount,
            CoinTransactionType type,
            String description,
            UUID performedByUserId,
            AuditAction auditAction,
            String referenceId
    ) {
        if (signedAmount == 0) {
            throw new InvalidAmountException();
        }
        if (signedAmount > 0) {
            validateAmount(signedAmount);
        } else {
            validateAmount(-signedAmount);
        }

        Wallet wallet = getOrCreateWallet(userId, gameSessionId);
        long newBalance = wallet.getBalance() + signedAmount;

        if (newBalance < 0) {
            throw new InsufficientBalanceException();
        }

        wallet.setBalance(newBalance);

        try {
            wallet = walletRepository.save(wallet);
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException();
        }

        CoinTransaction transaction = coinTransactionRepository.save(CoinTransaction.builder()
                .userId(userId)
                .gameSessionId(gameSessionId)
                .amount(signedAmount)
                .type(type)
                .referenceId(referenceId)
                .description(description)
                .build());

        auditService.log(
                performedByUserId,
                gameSessionId,
                auditAction,
                "Wallet",
                wallet.getId().toString(),
                description,
                Map.of(
                        "amount", signedAmount,
                        "balanceAfter", wallet.getBalance(),
                        "transactionId", transaction.getId().toString(),
                        "transactionType", type.name()
                )
        );

        return transaction;
    }

    private Wallet getOrCreateWallet(UUID userId, UUID gameSessionId) {
        return walletRepository.findByUserIdAndGameSessionId(userId, gameSessionId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .gameSessionId(gameSessionId)
                        .balance(0L)
                        .build()));
    }

    private void validateAmount(long amount) {
        if (amount <= 0) {
            throw new InvalidAmountException();
        }
    }
}
