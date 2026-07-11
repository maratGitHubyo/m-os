package com.mos.common.audit.service;

import com.mos.common.audit.enums.AuditAction;

import java.util.Map;
import java.util.UUID;

public record AuditLogEntry(
        UUID userId,
        UUID gameSessionId,
        AuditAction action,
        String entityType,
        String entityId,
        String description,
        Map<String, Object> metadata
) {
}
