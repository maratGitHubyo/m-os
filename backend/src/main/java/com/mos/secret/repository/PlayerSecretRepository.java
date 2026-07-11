package com.mos.secret.repository;

import com.mos.secret.entity.PlayerSecret;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlayerSecretRepository extends JpaRepository<PlayerSecret, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM PlayerSecret s
            WHERE s.code = :code AND s.gameSessionId = :gameSessionId
            """)
    Optional<PlayerSecret> findByCodeAndGameSessionIdForUpdate(
            @Param("code") String code,
            @Param("gameSessionId") UUID gameSessionId
    );

    boolean existsByCodeAndGameSessionId(String code, UUID gameSessionId);
}
