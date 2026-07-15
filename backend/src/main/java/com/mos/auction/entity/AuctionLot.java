package com.mos.auction.entity;

import com.mos.auction.enums.AuctionLotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auction_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Column(nullable = false)
    private String title;

    @Column(name = "starting_price", nullable = false)
    @Builder.Default
    private Long startingPrice = 50L;

    @Column(name = "min_bid_increment", nullable = false)
    @Builder.Default
    private Long minBidIncrement = 50L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuctionLotStatus status;

    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "current_leader_id")
    private UUID currentLeaderId;

    @Column(name = "winner_user_id")
    private UUID winnerUserId;

    @Column(name = "final_price")
    private Long finalPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = AuctionLotStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
