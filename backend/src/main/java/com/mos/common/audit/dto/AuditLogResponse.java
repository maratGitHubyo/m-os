package com.mos.common.audit.dto;

import com.mos.common.audit.entity.AuditLog;
import com.mos.common.audit.enums.AuditAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        UUID gameSessionId,
        AuditAction action,
        String entityType,
        String entityId,
        String description,
        Map<String, Object> metadata,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getGameSessionId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }
}
