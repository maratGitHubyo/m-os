package com.mos.location.repository;

import com.mos.location.entity.PlayerLocationDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerLocationDiscoveryRepository extends JpaRepository<PlayerLocationDiscovery, UUID> {

    Optional<PlayerLocationDiscovery> findByUserIdAndLocationPointId(UUID userId, UUID locationPointId);

    boolean existsByUserIdAndLocationPointId(UUID userId, UUID locationPointId);

    List<PlayerLocationDiscovery> findByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);

    long countByUserIdAndGameSessionId(UUID userId, UUID gameSessionId);
}
