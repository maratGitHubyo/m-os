package com.mos.location.repository;

import com.mos.location.entity.LocationPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationPointRepository extends JpaRepository<LocationPoint, UUID> {

    List<LocationPoint> findByGameSessionIdOrderByZoneAscNameAsc(UUID gameSessionId);

    Optional<LocationPoint> findByIdAndGameSessionId(UUID id, UUID gameSessionId);
}
