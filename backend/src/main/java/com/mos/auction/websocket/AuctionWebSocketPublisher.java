package com.mos.auction.websocket;

import com.mos.auction.dto.AuctionBroadcastMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(AuctionBroadcastMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + message.gameSessionId() + "/auction",
                message
        );
    }
}
