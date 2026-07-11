package com.mos.session.dto;

import com.mos.session.entity.GameConfig;

import java.util.Map;
import java.util.UUID;

public record GameConfigResponse(
        UUID id,
        Integer startingCoins,
        Integer maxTradeOffers,
        Integer numbersTotal,
        Boolean fogOfWarEnabled,
        Boolean secretsEnabled,
        Boolean leaderboardEnabled,
        Map<String, Object> customSettings
) {

    public static GameConfigResponse from(GameConfig config) {
        return new GameConfigResponse(
                config.getId(),
                config.getStartingCoins(),
                config.getMaxTradeOffers(),
                config.getNumbersTotal(),
                config.getFogOfWarEnabled(),
                config.getSecretsEnabled(),
                config.getLeaderboardEnabled(),
                config.getCustomSettings()
        );
    }
}
