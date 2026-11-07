package com.checkers.websocket;

import com.checkers.model.enums.EventType;
import com.checkers.websocket.dto.GameEventMessage;
import com.checkers.websocket.dto.MatchmakingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Shared publication point for game-topic events and private user queues.
 */
@Component
@RequiredArgsConstructor
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publishes a supplied event or constructs one from its type and data for the game's shared
     * STOMP topic. Both clients receive the same server snapshot.
     */
    public void publishToGame(Long gameId, GameEventMessage message) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, message);
    }

    /**
     * Publishes a supplied event or constructs one from its type and data for the game's shared
     * STOMP topic. Both clients receive the same server snapshot.
     */
    public void publishToGame(Long gameId, EventType type, Map<String, Object> data) {
        publishToGame(gameId, GameEventMessage.builder()
                .type(type)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(data == null ? Map.of() : data)
                .build());
    }

    /**
     * Sends a supplied or constructed matchmaking event to the named user's private queue.
     */
    public void publishMatchmaking(String username, MatchmakingMessage message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/matchmaking", message);
    }

    /**
     * Sends a supplied or constructed matchmaking event to the named user's private queue.
     */
    public void publishMatchmaking(String username, EventType type, Map<String, Object> data) {
        publishMatchmaking(username, MatchmakingMessage.builder()
                .type(type)
                .timestamp(Instant.now())
                .data(data == null ? Map.of() : data)
                .build());
    }

    /**
     * Sends a game error to the user's private queue rather than broadcasting it to the opponent.
     */
    public void publishError(String username, GameEventMessage message) {
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", message);
    }
}
