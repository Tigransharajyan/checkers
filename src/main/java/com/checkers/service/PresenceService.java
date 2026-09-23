package com.checkers.service;

import com.checkers.config.GameProperties;
import com.checkers.model.entity.Game;
import com.checkers.model.enums.EventType;
import com.checkers.model.enums.GameStatus;
import com.checkers.repository.GameRepository;
import com.checkers.security.SecurityUser;
import com.checkers.websocket.GameEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Tracks STOMP subscriptions to {@code /topic/game/{id}} and applies a reconnect
 * grace period before technical defeat.
 */
@Service
@Slf4j
public class PresenceService {

    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> sessionGames = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pendingDefeats = new ConcurrentHashMap<>();
    private final Map<String, Instant> disconnectedAt = new ConcurrentHashMap<>();

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final GameEventPublisher eventPublisher;
    private final GameProperties gameProperties;
    private final ThreadPoolTaskScheduler taskScheduler;

    public PresenceService(GameRepository gameRepository,
                           @Lazy GameService gameService,
                           GameEventPublisher eventPublisher,
                           GameProperties gameProperties,
                           ThreadPoolTaskScheduler taskScheduler) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.eventPublisher = eventPublisher;
        this.gameProperties = gameProperties;
        this.taskScheduler = taskScheduler;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = resolveUserId(accessor.getUser());
        if (userId != null && accessor.getSessionId() != null) {
            sessionUsers.put(accessor.getSessionId(), userId);
        }
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        if (destination == null || sessionId == null || !destination.startsWith("/topic/game/")) {
            return;
        }

        Long gameId = parseGameId(destination);
        if (gameId == null) {
            return;
        }

        Long resolvedUserId = sessionUsers.get(sessionId);
        if (resolvedUserId == null) {
            resolvedUserId = resolveUserId(accessor.getUser());
            if (resolvedUserId != null) {
                sessionUsers.put(sessionId, resolvedUserId);
            }
        }
        if (resolvedUserId == null) {
            return;
        }

        final Long userId = resolvedUserId;
        sessionGames.computeIfAbsent(sessionId, id -> ConcurrentHashMap.newKeySet()).add(gameId);
        String key = key(gameId, userId);
        boolean wasDisconnected = pendingDefeats.containsKey(key) || disconnectedAt.containsKey(key);
        cancelDefeat(key);

        if (wasDisconnected) {
            gameRepository.findById(gameId).ifPresent(game -> {
                if (game.getStatus() == GameStatus.IN_PROGRESS) {
                    eventPublisher.publishToGame(gameId, EventType.OPPONENT_RECONNECTED, Map.of(
                            "userId", userId,
                            "gameId", gameId,
                            "reconnectTimeoutSeconds", gameProperties.getReconnectTimeoutSeconds()
                    ));
                }
            });
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        Long userId = sessionUsers.remove(sessionId);
        Set<Long> games = sessionGames.remove(sessionId);
        if (userId == null || games == null || games.isEmpty()) {
            return;
        }

        for (Long gameId : games) {
            handlePlayerLeft(gameId, userId);
        }
    }

    public void markPresent(Long gameId, Long userId) {
        String key = key(gameId, userId);
        boolean wasDisconnected = pendingDefeats.containsKey(key) || disconnectedAt.containsKey(key);
        cancelDefeat(key);
        if (wasDisconnected) {
            eventPublisher.publishToGame(gameId, EventType.OPPONENT_RECONNECTED, Map.of(
                    "userId", userId,
                    "gameId", gameId
            ));
        }
    }

    private void handlePlayerLeft(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getStatus() != GameStatus.IN_PROGRESS) {
            return;
        }
        if (!isParticipant(game, userId)) {
            return;
        }
        String key = key(gameId, userId);
        if (pendingDefeats.containsKey(key)) {
            return;
        }

        disconnectedAt.put(key, Instant.now());
        eventPublisher.publishToGame(gameId, EventType.OPPONENT_DISCONNECTED, Map.of(
                "userId", userId,
                "gameId", gameId,
                "reconnectTimeoutSeconds", gameProperties.getReconnectTimeoutSeconds()
        ));

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> applyTechnicalDefeat(gameId, userId),
                Instant.now().plus(Duration.ofSeconds(gameProperties.getReconnectTimeoutSeconds())));
        pendingDefeats.put(key, future);
    }

    private void applyTechnicalDefeat(Long gameId, Long userId) {
        String key = key(gameId, userId);
        pendingDefeats.remove(key);
        disconnectedAt.remove(key);
        try {
            var event = gameService.technicalDefeat(gameId, userId);
            if (event != null) {
                eventPublisher.publishToGame(gameId, event);
            }
        } catch (Exception ex) {
            log.debug("Technical defeat skipped for game {}: {}", gameId, ex.getMessage());
        }
    }

    private void cancelDefeat(String key) {
        ScheduledFuture<?> future = pendingDefeats.remove(key);
        if (future != null) {
            future.cancel(false);
        }
        disconnectedAt.remove(key);
    }

    private boolean isParticipant(Game game, Long userId) {
        return (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId))
                || (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId));
    }

    private Long parseGameId(String destination) {
        try {
            String suffix = destination.substring("/topic/game/".length());
            int slash = suffix.indexOf('/');
            if (slash >= 0) {
                suffix = suffix.substring(0, slash);
            }
            return Long.valueOf(suffix);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long resolveUserId(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth
                && auth.getPrincipal() instanceof SecurityUser user) {
            return user.getId();
        }
        return null;
    }

    private static String key(Long gameId, Long userId) {
        return gameId + ":" + userId;
    }
}
