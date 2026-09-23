package com.checkers.websocket.controller;

import com.checkers.model.entity.Game;
import com.checkers.model.enums.EventType;
import com.checkers.service.MatchmakingService;
import com.checkers.websocket.GameEventPublisher;
import com.checkers.websocket.dto.MatchmakingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MatchmakingWebSocketController {

    private final MatchmakingService matchmakingService;
    private final GameEventPublisher eventPublisher;

    @MessageMapping("/matchmaking/join")
    public void joinQueue(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        }
        matchmakingService.enqueue();
        String name = principal != null ? principal.getName() : null;
        if (name != null) {
            eventPublisher.publishMatchmaking(name, EventType.MATCHMAKING_QUEUED, Map.of());
        }

        Game matched = matchmakingService.tryMatch();
        if (matched != null) {
            notifyMatched(matched);
        }
    }

    @MessageMapping("/matchmaking/leave")
    public void leaveQueue(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        }
        matchmakingService.dequeue();
        String name = principal != null ? principal.getName() : null;
        if (name != null) {
            eventPublisher.publishMatchmaking(name, EventType.MATCHMAKING_CANCELLED, Map.of());
        }
    }

    private void notifyMatched(Game game) {
        Map<String, Object> whiteData = baseMatchData(game);
        whiteData.put("yourColor", "WHITE");
        whiteData.put("opponentId", game.getBlackPlayer().getId());
        whiteData.put("opponentUsername", game.getBlackPlayer().getUsername());

        Map<String, Object> blackData = baseMatchData(game);
        blackData.put("yourColor", "BLACK");
        blackData.put("opponentId", game.getWhitePlayer().getId());
        blackData.put("opponentUsername", game.getWhitePlayer().getUsername());

        eventPublisher.publishMatchmaking(game.getWhitePlayer().getUsername(),
                MatchmakingMessage.builder()
                        .type(EventType.MATCHMAKING_MATCHED)
                        .timestamp(Instant.now())
                        .data(whiteData)
                        .build());
        eventPublisher.publishMatchmaking(game.getBlackPlayer().getUsername(),
                MatchmakingMessage.builder()
                        .type(EventType.MATCHMAKING_MATCHED)
                        .timestamp(Instant.now())
                        .data(blackData)
                        .build());

        eventPublisher.publishToGame(game.getId(), EventType.GAME_STARTED, Map.of(
                "gameId", game.getId(),
                "whitePlayerId", game.getWhitePlayer().getId(),
                "blackPlayerId", game.getBlackPlayer().getId()
        ));
    }

    private Map<String, Object> baseMatchData(Game game) {
        Map<String, Object> data = new HashMap<>();
        data.put("gameId", game.getId());
        return data;
    }
}
