package com.mos.auction.repository;

import com.mos.auction.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, UUID> {

    List<AuctionBid> findByLotIdOrderByCreatedAtDesc(UUID lotId);

    List<AuctionBid> findTop20ByLotIdOrderByCreatedAtDesc(UUID lotId);
}
