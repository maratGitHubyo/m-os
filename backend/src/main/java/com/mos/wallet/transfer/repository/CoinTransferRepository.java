package com.mos.wallet.transfer.repository;

import com.mos.wallet.transfer.entity.CoinTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CoinTransferRepository extends JpaRepository<CoinTransfer, UUID> {

    @Query("""
            SELECT ct FROM CoinTransfer ct
            WHERE ct.gameSessionId = :gameSessionId
              AND (ct.senderUserId = :userId OR ct.receiverUserId = :userId)
            ORDER BY ct.createdAt DESC
            """)
    Page<CoinTransfer> findByGameSessionAndParticipant(
            @Param("gameSessionId") UUID gameSessionId,
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
