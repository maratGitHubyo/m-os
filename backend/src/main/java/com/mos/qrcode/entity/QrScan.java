package com.mos.qrcode.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "qr_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrScan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qr_code_id", nullable = false)
    private QrCode qrCode;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Column(name = "scanned_at", nullable = false, updatable = false)
    private Instant scannedAt;

    @Column(name = "reward_given", nullable = false)
    @Builder.Default
    private Boolean rewardGiven = true;

    @PrePersist
    void onCreate() {
        if (scannedAt == null) {
            scannedAt = Instant.now();
        }
        if (rewardGiven == null) {
            rewardGiven = true;
        }
    }
}
