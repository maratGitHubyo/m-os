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
            SELECT CASE WHEN COUNT(pi) > 0 THEN true ELSE false END
            FROM PlayerItem pi
            WHERE pi.ownerId = :ownerId
              AND pi.gameSessionId = :gameSessionId
              AND pi.itemTemplate.isLore = true
            """)
    boolean existsLoreItemByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("gameSessionId") UUID gameSessionId
    );

    @Query("""
            SELECT pi FROM PlayerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE pi.ownerId = :ownerId
              AND pi.gameSessionId = :gameSessionId
              AND pi.itemTemplate.isLore = true
            """)
    List<PlayerItem> findLoreItemsByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("gameSessionId") UUID gameSessionId
    );

    @Query("""
            SELECT pi FROM PlayerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE pi.gameSessionId = :gameSessionId
              AND pi.itemTemplate.isLore = true
            """)
    List<PlayerItem> findLoreItemsByGameSessionId(@Param("gameSessionId") UUID gameSessionId);

    @Query("""
            SELECT pi FROM PlayerItem pi
            JOIN FETCH pi.itemTemplate
            WHERE pi.id = :id AND pi.gameSessionId = :gameSessionId
            """)
    Optional<PlayerItem> findByIdAndGameSessionId(
            @Param("id") UUID id,
            @Param("gameSessionId") UUID gameSessionId
    );

    @Query("""
            SELECT COUNT(DISTINCT pi.itemTemplate.id) FROM PlayerItem pi
            WHERE pi.ownerId = :ownerId
              AND pi.gameSessionId = :gameSessionId
              AND pi.itemTemplate.isUnique = true
            """)
    long countDistinctUniqueItemTemplatesByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("gameSessionId") UUID gameSessionId
    );

    @Query("""
            SELECT pi.ownerId, pi.itemTemplate.rarity, COUNT(pi)
            FROM PlayerItem pi
            WHERE pi.gameSessionId = :gameSessionId
              AND pi.itemTemplate.isLore = false
            GROUP BY pi.ownerId, pi.itemTemplate.rarity
            """)
    List<Object[]> countByOwnerAndRarity(@Param("gameSessionId") UUID gameSessionId);
}
