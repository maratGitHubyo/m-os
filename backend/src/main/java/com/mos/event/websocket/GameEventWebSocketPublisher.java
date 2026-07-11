package com.mos.event.websocket;

import com.mos.event.dto.GameEventBroadcastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GameEventWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishStatusChange(GameEventBroadcastMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + message.gameSessionId() + "/events",
                message
        );
    }

    public String topicForSession(UUID gameSessionId) {
        return "/topic/session/" + gameSessionId + "/events";
    }
}
