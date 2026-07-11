package com.mos.victory.repository;

import com.mos.victory.entity.VictoryCondition;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VictoryConditionRepository extends JpaRepository<VictoryCondition, UUID> {

    List<VictoryCondition> findByGameSessionIdOrderByCreatedAtAsc(UUID gameSessionId);

    List<VictoryCondition> findByGameSessionIdAndActiveTrueAndAchievedAtIsNullOrderByCreatedAtAsc(UUID gameSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vc FROM VictoryCondition vc WHERE vc.id = :id")
    Optional<VictoryCondition> findByIdForUpdate(@Param("id") UUID id);
}
