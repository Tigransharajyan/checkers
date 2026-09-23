package com.checkers.websocket.controller;

import com.checkers.model.enums.EventType;
import com.checkers.service.BotPlayService;
import com.checkers.service.GameService;
import com.checkers.websocket.dto.DrawOfferMessage;
import com.checkers.websocket.dto.GameEventMessage;
import com.checkers.websocket.dto.MoveMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class GameWebSocketController {

    private final GameService gameService;
    private final BotPlayService botPlayService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client → {@code /app/game/{gameId}/move}
     * Broadcast → {@code /topic/game/{gameId}}
     * Bot reply (if any) is scheduled asynchronously on the same topic.
     */
    @MessageMapping("/game/{gameId}/move")
    public void makeMove(@DestinationVariable Long gameId, @Payload MoveMessage message, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        try {
            GameEventMessage event = gameService.applyMove(gameId, message);
            messagingTemplate.convertAndSend(topic(gameId), event);
            botPlayService.scheduleBotMove(gameId);
        } catch (Exception ex) {
            log.warn("makeMove failed for game {}: {}", gameId, ex.getMessage(), ex);
            String user = principal != null ? principal.getName() : currentUsername();
            messagingTemplate.convertAndSendToUser(
                    user,
                    "/queue/errors",
                    errorEvent(gameId, ex));
        }
    }

    @MessageMapping("/game/{gameId}/resign")
    public void resign(@DestinationVariable Long gameId, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        GameEventMessage event = gameService.resign(gameId);
        messagingTemplate.convertAndSend(topic(gameId), event);
    }

    @MessageMapping("/game/{gameId}/draw")
    public void draw(@DestinationVariable Long gameId, @Payload DrawOfferMessage message, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        GameEventMessage event;
        if (message != null && message.getAccept() != null) {
            event = gameService.respondToDraw(gameId, message.getAccept());
        } else {
            event = gameService.offerDraw(gameId);
        }
        messagingTemplate.convertAndSend(topic(gameId), event);
    }

    @MessageMapping("/game/{gameId}/presence")
    public void presence(@DestinationVariable Long gameId, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        GameEventMessage event = gameService.presence(gameId);
        messagingTemplate.convertAndSend(topic(gameId), event);
    }

    private void setAuthFromPrincipal(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    private String topic(Long gameId) {
        return "/topic/game/" + gameId;
    }

    private String currentUsername() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private GameEventMessage errorEvent(Long gameId, Exception ex) {
        String code = ex instanceof com.checkers.exception.BusinessException bex
                ? bex.getErrorCode().name()
                : "INTERNAL_ERROR";
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("errorCode", code);
        if (ex instanceof com.checkers.exception.BusinessException bex && bex.getDetails() != null) {
            data.putAll(bex.getDetails());
        }
        return GameEventMessage.builder()
                .type(EventType.ERROR)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }
}
