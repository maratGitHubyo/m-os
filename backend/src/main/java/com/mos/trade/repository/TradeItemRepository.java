package com.mos.trade.repository;

import com.mos.trade.entity.TradeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TradeItemRepository extends JpaRepository<TradeItem, UUID> {

    @Query("""
            SELECT ti FROM TradeItem ti
            JOIN FETCH ti.playerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE ti.trade.id = :tradeId
            """)
    List<TradeItem> findByTradeIdWithItems(@Param("tradeId") UUID tradeId);
}
