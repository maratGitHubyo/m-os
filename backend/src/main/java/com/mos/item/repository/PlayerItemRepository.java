package com.mos.item.repository;

import com.mos.item.entity.PlayerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerItemRepository extends JpaRepository<PlayerItem, UUID> {

    @Query("""
            SELECT pi FROM PlayerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE pi.ownerId = :ownerId AND pi.gameSessionId = :gameSessionId
            ORDER BY pi.acquiredAt DESC
            """)
    List<PlayerItem> findByOwnerIdAndGameSessionIdOrderByAcquiredAtDesc(
            @Param("ownerId") UUID ownerId,
            @Param("gameSessionId") UUID gameSessionId
    );

    boolean existsByItemTemplateIdAndGameSessionId(UUID itemTemplateId, UUID gameSessionId);

    @Query("""
            SELECT pi FROM PlayerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE pi.id = :id AND pi.gameSessionId = :gameSessionId
            """)
    Optional<PlayerItem> findByIdAndGameSessionId(
            @Param("id") UUID id,
            @Param("gameSessionId") UUID gameSessionId
    );
}
