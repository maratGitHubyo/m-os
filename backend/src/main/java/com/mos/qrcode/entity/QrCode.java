package com.mos.qrcode.entity;

import com.mos.qrcode.enums.QrRewardType;
import com.mos.qrcode.enums.QrScanPolicy;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_point_id")
    private com.mos.location.entity.LocationPoint locationPoint;

    @Column(nullable = false)
    private String code;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 50)
    private QrRewardType rewardType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_payload", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> rewardPayload = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_policy", nullable = false, length = 50)
    private QrScanPolicy scanPolicy;

    @Column(name = "scan_limit")
    private Integer scanLimit;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (title == null || title.isBlank()) {
            title = code;
        }
        if (active == null) {
            active = true;
        }
        if (rewardPayload == null) {
            rewardPayload = new HashMap<>();
        }
    }
}
