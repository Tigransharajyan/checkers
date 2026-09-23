package com.checkers.websocket;

import com.checkers.model.enums.EventType;
import com.checkers.websocket.dto.GameEventMessage;
import com.checkers.websocket.dto.MatchmakingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToGame(Long gameId, GameEventMessage message) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);
    }

    public void publishToGame(Long gameId, EventType type, Map<String, Object> data) {
        publishToGame(gameId, GameEventMessage.builder()
                .type(type)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(data == null ? Map.of() : data)
                .build());
    }

    public void publishMatchmaking(String username, MatchmakingMessage message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/matchmaking", message);
    }

    public void publishMatchmaking(String username, EventType type, Map<String, Object> data) {
        publishMatchmaking(username, MatchmakingMessage.builder()
                .type(type)
                .timestamp(Instant.now())
                .data(data == null ? Map.of() : data)
                .build());
    }

    public void publishError(String username, GameEventMessage message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", message);
    }
}
