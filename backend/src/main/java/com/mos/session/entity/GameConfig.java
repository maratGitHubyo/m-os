package com.mos.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_session_id", nullable = false, unique = true)
    private GameSession gameSession;

    @Column(name = "starting_coins", nullable = false)
    @Builder.Default
    private Integer startingCoins = 0;

    @Column(name = "max_trade_offers", nullable = false)
    @Builder.Default
    private Integer maxTradeOffers = 5;

    @Column(name = "numbers_total", nullable = false)
    @Builder.Default
    private Integer numbersTotal = 50;

    @Column(name = "fog_of_war_enabled", nullable = false)
    @Builder.Default
    private Boolean fogOfWarEnabled = true;

    @Column(name = "secrets_enabled", nullable = false)
    @Builder.Default
    private Boolean secretsEnabled = true;

    @Column(name = "leaderboard_enabled", nullable = false)
    @Builder.Default
    private Boolean leaderboardEnabled = true;

    @Column(name = "auction_mode_enabled", nullable = false)
    @Builder.Default
    private Boolean auctionModeEnabled = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_settings", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> customSettings = new HashMap<>();
}
