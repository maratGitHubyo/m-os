package com.mos.numbers.repository;

import com.mos.numbers.entity.PlayerNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerNumberRepository extends JpaRepository<PlayerNumber, UUID> {

    @Query("""
            SELECT pn FROM PlayerNumber pn
            JOIN FETCH pn.number
            WHERE pn.userId = :userId AND pn.gameSessionId = :gameSessionId
            ORDER BY pn.acquiredAt DESC
            """)
    List<PlayerNumber> findByUserIdAndGameSessionIdOrderByAcquiredAtDesc(
            @Param("userId") UUID userId,
            @Param("gameSessionId") UUID gameSessionId
    );

    Optional<PlayerNumber> findByUserIdAndNumberId(UUID userId, UUID numberId);

    long countByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);
}
