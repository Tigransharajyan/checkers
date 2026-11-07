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

/**
 * Handles game STOMP commands, restores sender identity, invokes transactional services and
 * publishes results. Does not duplicate draughts rules.
 */
@Controller
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class GameWebSocketController {

    private final GameService gameService;
    private final BotPlayService botPlayService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Restores the sender's security context, applies the move through GameService and broadcasts
     * the resulting game event. Schedules a possible bot reply afterward; errors are sent only to
     * the sender.
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

    /**
     * Handles a STOMP resignation command and broadcasts completion, or sends a private error to
     * the sender.
     */
    @MessageMapping("/game/{gameId}/resign")
    public void resign(@DestinationVariable Long gameId, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        try {
            GameEventMessage event = gameService.resign(gameId);
            messagingTemplate.convertAndSend(topic(gameId), event);
        } catch (Exception ex) {
            log.warn("resign failed for game {}: {}", gameId, ex.getMessage(), ex);
            String user = principal != null ? principal.getName() : currentUsername();
            messagingTemplate.convertAndSendToUser(user, "/queue/errors", errorEvent(gameId, ex));
        }
    }

    /**
     * Uses the accept field to distinguish a draw offer from acceptance or rejection. Delegates to
     * the appropriate service method and publishes the result.
     */
    @MessageMapping("/game/{gameId}/draw")
    public void draw(@DestinationVariable Long gameId, @Payload DrawOfferMessage message, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        try {
            GameEventMessage event;
            if (message != null && message.getAccept() != null) {
                event = gameService.respondToDraw(gameId, message.getAccept());
            } else {
                event = gameService.offerDraw(gameId);
            }
            messagingTemplate.convertAndSend(topic(gameId), event);
        } catch (Exception ex) {
            log.warn("draw failed for game {}: {}", gameId, ex.getMessage(), ex);
            String user = principal != null ? principal.getName() : currentUsername();
            messagingTemplate.convertAndSendToUser(user, "/queue/errors", errorEvent(gameId, ex));
        }
    }

    /**
     * Handles an explicit presence update and publishes a reconnection event when necessary.
     */
    @MessageMapping("/game/{gameId}/presence")
    public void presence(@DestinationVariable Long gameId, java.security.Principal principal) {
        setAuthFromPrincipal(principal);
        try {
            GameEventMessage event = gameService.presence(gameId);
            if (event != null) {
                messagingTemplate.convertAndSend(topic(gameId), event);
            }
        } catch (Exception ex) {
            log.warn("presence failed for game {}: {}", gameId, ex.getMessage(), ex);
        }
    }

    /**
     * Copies the already authenticated WebSocket principal into the current handler thread's
     * SecurityContextHolder.
     */
    private void setAuthFromPrincipal(java.security.Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    /**
     * Builds the shared /topic/game/{id} STOMP destination for a game.
     */
    private String topic(Long gameId) {
        return "/topic/game/" + gameId;
    }

    /**
     * Returns the name from the current security context, or anonymous as a fallback for addressing
     * errors.
     */
    private String currentUsername() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * Converts an exception into an ERROR event with a machine-readable code and details. Preserves
     * domain error codes and uses INTERNAL_ERROR for unexpected failures.
     */
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
