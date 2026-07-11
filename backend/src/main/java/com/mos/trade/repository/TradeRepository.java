package com.mos.trade.repository;

import com.mos.trade.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByIdAndGameSessionId(UUID id, UUID gameSessionId);

    @Query("""
            SELECT t FROM Trade t
            WHERE t.gameSessionId = :gameSessionId
              AND (t.initiatorId = :userId OR t.receiverId = :userId)
            ORDER BY t.createdAt DESC
            """)
    List<Trade> findByGameSessionIdAndParticipant(
            @Param("gameSessionId") UUID gameSessionId,
            @Param("userId") UUID userId
    );
}
