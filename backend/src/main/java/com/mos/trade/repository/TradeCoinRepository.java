package com.mos.trade.repository;

import com.mos.trade.entity.TradeCoin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeCoinRepository extends JpaRepository<TradeCoin, UUID> {

    List<TradeCoin> findByTradeId(UUID tradeId);
}
