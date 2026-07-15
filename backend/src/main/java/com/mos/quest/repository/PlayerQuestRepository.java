package com.mos.quest.repository;

import com.mos.quest.entity.PlayerQuest;
import com.mos.quest.enums.PlayerQuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerQuestRepository extends JpaRepository<PlayerQuest, UUID> {

    Optional<PlayerQuest> findByQuestIdAndUserId(UUID questId, UUID userId);

    long countByQuestIdAndStatus(UUID questId, PlayerQuestStatus status);

    List<PlayerQuest> findByQuestIdAndStatus(UUID questId, PlayerQuestStatus status);

    List<PlayerQuest> findByGameSessionIdAndStatus(UUID gameSessionId, PlayerQuestStatus status);

    @Query("""
            SELECT pq FROM PlayerQuest pq
            JOIN FETCH pq.quest
            WHERE pq.userId = :userId AND pq.gameSessionId = :gameSessionId
            ORDER BY pq.createdAt DESC
            """)
    List<PlayerQuest> findByUserIdAndGameSessionIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("gameSessionId") UUID gameSessionId
    );

    @Query("""
            SELECT pq FROM PlayerQuest pq
            JOIN FETCH pq.quest
            WHERE pq.userId = :userId
              AND pq.gameSessionId = :gameSessionId
              AND pq.status = :status
            """)
    List<PlayerQuest> findByUserIdAndGameSessionIdAndStatus(
            @Param("userId") UUID userId,
            @Param("gameSessionId") UUID gameSessionId,
            @Param("status") PlayerQuestStatus status
    );

    @Query("""
            SELECT pq FROM PlayerQuest pq
            JOIN FETCH pq.quest
            WHERE pq.id = :id AND pq.gameSessionId = :gameSessionId
            """)
    Optional<PlayerQuest> findByIdAndGameSessionId(
            @Param("id") UUID id,
            @Param("gameSessionId") UUID gameSessionId
    );
}
