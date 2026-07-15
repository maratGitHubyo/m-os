package com.mos.auction.repository;

import com.mos.auction.entity.AuctionLot;
import com.mos.auction.enums.AuctionLotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionLotRepository extends JpaRepository<AuctionLot, UUID> {

    List<AuctionLot> findByGameSessionIdOrderByCreatedAtDesc(UUID gameSessionId);

    Optional<AuctionLot> findByIdAndGameSessionId(UUID id, UUID gameSessionId);

    Optional<AuctionLot> findByGameSessionIdAndStatus(UUID gameSessionId, AuctionLotStatus status);

    boolean existsByGameSessionIdAndStatus(UUID gameSessionId, AuctionLotStatus status);
}
