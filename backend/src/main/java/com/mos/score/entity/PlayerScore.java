package com.mos.score.entity;

import com.mos.score.enums.ScoreCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "player_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_session_id", "category"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "game_session_id", nullable = false)
    private UUID gameSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScoreCategory category;

    @Column(nullable = false)
    @Builder.Default
    private Long points = 0L;
}
