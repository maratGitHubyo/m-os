package com.mos.item.entity;

import com.mos.item.enums.ItemAcquisitionSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_template_id", nullable = false)
    private ItemTemplate itemTemplate;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquired_from", nullable = false, length = 50)
    private ItemAcquisitionSource acquiredFrom;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private Instant acquiredAt;

    @PrePersist
    void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = Instant.now();
        }
    }
}
