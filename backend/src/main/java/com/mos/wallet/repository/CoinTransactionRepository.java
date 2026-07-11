package com.mos.wallet.repository;

import com.mos.wallet.entity.CoinTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    Page<CoinTransaction> findByUserIdAndGameSessionIdOrderByCreatedAtDesc(
            UUID userId,
            UUID gameSessionId,
            Pageable pageable
    );
}
