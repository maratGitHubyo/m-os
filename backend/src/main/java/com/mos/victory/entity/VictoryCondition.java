package com.mos.victory.entity;

import com.mos.victory.enums.VictoryConditionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "victory_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VictoryCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VictoryConditionType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_value", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> targetValue = new HashMap<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @Column(name = "achieved_by_user_id")
    private UUID achievedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (targetValue == null) {
            targetValue = new HashMap<>();
        }
        if (active == null) {
            active = true;
        }
    }
}
