package com.mos.notification.dto;

import com.mos.notification.enums.AppNotificationType;

import java.time.Instant;
import java.util.UUID;

public record AppNotificationMessage(
        AppNotificationType type,
        UUID gameSessionId,
        UUID targetUserId,
        String title,
        String body,
        Instant createdAt
) {
}
