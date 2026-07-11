package com.mos.common.audit.repository;

import com.mos.common.audit.entity.AuditLog;
import com.mos.common.audit.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.gameSessionId = :gameSessionId
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:action IS NULL OR a.action = :action)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("gameSessionId") UUID gameSessionId,
            @Param("userId") UUID userId,
            @Param("action") AuditAction action,
            Pageable pageable
    );
}
