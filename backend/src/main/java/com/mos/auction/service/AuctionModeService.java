package com.mos.auction.service;

import com.mos.common.exception.AuctionModeActiveException;
import com.mos.common.exception.AuctionModeInactiveException;
import com.mos.common.exception.BusinessException;
import com.mos.session.entity.GameConfig;
import com.mos.session.repository.GameConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionModeService {

    private final GameConfigRepository gameConfigRepository;

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID gameSessionId) {
        return gameConfigRepository.findByGameSessionId(gameSessionId)
                .map(config -> Boolean.TRUE.equals(config.getAuctionModeEnabled()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void ensureEnabled(UUID gameSessionId) {
        if (!isEnabled(gameSessionId)) {
            throw new AuctionModeInactiveException();
        }
    }

    @Transactional(readOnly = true)
    public void ensureDisabled(UUID gameSessionId) {
        if (isEnabled(gameSessionId)) {
            throw new AuctionModeActiveException();
        }
    }

    @Transactional
    public GameConfig requireConfig(UUID gameSessionId) {
        return gameConfigRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() -> new BusinessException("Game config not found"));
    }
}
