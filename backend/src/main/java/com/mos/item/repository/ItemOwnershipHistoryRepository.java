package com.mos.item.repository;

import com.mos.item.entity.ItemOwnershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemOwnershipHistoryRepository extends JpaRepository<ItemOwnershipHistory, UUID> {

    List<ItemOwnershipHistory> findByPlayerItemIdOrderByCreatedAtAsc(UUID playerItemId);
}
