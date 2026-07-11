package com.mos.victory.websocket;

import com.mos.victory.dto.VictoryBroadcastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VictoryWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishVictory(VictoryBroadcastMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + message.gameSessionId() + "/victory",
                message
        );
    }

    public String topicForSession(UUID gameSessionId) {
        return "/topic/session/" + gameSessionId + "/victory";
    }
}
