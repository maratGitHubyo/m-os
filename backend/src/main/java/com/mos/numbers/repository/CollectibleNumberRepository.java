package com.mos.numbers.repository;

import com.mos.numbers.entity.CollectibleNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectibleNumberRepository extends JpaRepository<CollectibleNumber, UUID> {

    boolean existsByGameSessionIdAndNumberValue(UUID gameSessionId, Integer numberValue);

    Optional<CollectibleNumber> findByIdAndGameSessionId(UUID id, UUID gameSessionId);
}
