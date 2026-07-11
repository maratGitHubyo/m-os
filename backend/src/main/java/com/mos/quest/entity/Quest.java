package com.mos.quest.entity;

import com.mos.quest.enums.QuestDefinitionStatus;
import com.mos.quest.enums.QuestType;
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
@Table(name = "quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QuestDefinitionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_config", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> targetConfig = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_config", columnDefinition = "jsonb")
    private Map<String, Object> rewardConfig;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (targetConfig == null) {
            targetConfig = new HashMap<>();
        }
        if (status == null) {
            status = QuestDefinitionStatus.ACTIVE;
        }
    }
}
