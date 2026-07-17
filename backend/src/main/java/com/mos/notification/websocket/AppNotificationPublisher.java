package com.mos.notification.websocket;

import com.mos.notification.dto.AppNotificationMessage;
import com.mos.notification.enums.AppNotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(
            AppNotificationType type,
            UUID gameSessionId,
            UUID targetUserId,
            String title,
            String body
    ) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + gameSessionId + "/notifications",
                new AppNotificationMessage(
                        type,
                        gameSessionId,
                        targetUserId,
                        title,
                        body,
                        Instant.now()
                )
        );
    }
}
