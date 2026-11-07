package com.checkers.service;

import com.checkers.config.GameProperties;
import com.checkers.model.entity.Game;
import com.checkers.model.enums.EventType;
import com.checkers.model.enums.GameMode;
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
 * Associates STOMP sessions with users and games. Allows a reconnect grace period before requesting
 * technical defeat.
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
    private final MatchmakingService matchmakingService;
    private final GameEventPublisher eventPublisher;
    private final GameProperties gameProperties;
    private final ThreadPoolTaskScheduler taskScheduler;

    /**
     * Receives persistence, game collaborators, settings and the scheduler. Lazy dependencies
     * connect presence with game finishing and matchmaking without immediate circular
     * initialization.
     */
    public PresenceService(GameRepository gameRepository,
                           @Lazy GameService gameService,
                           @Lazy MatchmakingService matchmakingService,
                           GameEventPublisher eventPublisher,
                           GameProperties gameProperties,
                           ThreadPoolTaskScheduler taskScheduler) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.matchmakingService = matchmakingService;
        this.eventPublisher = eventPublisher;
        this.gameProperties = gameProperties;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Records the association between the STOMP session id and authenticated user after connection.
     */
    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = resolveUserId(accessor.getUser());
        if (userId != null && accessor.getSessionId() != null) {
            sessionUsers.put(accessor.getSessionId(), userId);
        }
    }

    /**
     * Associates a session with its game, cancels pending technical defeat and notifies the
     * opponent on return. Processes only /topic/game/{id} subscriptions.
     */
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
                if (game.getStatus() == GameStatus.IN_PROGRESS && game.getMode() != GameMode.BOT) {
                    eventPublisher.publishToGame(gameId, EventType.OPPONENT_RECONNECTED, Map.of(
                            "userId", userId,
                            "gameId", gameId,
                            "reconnectTimeoutSeconds", gameProperties.getReconnectTimeoutSeconds()
                    ));
                }
            });
        }
    }

    /**
     * Removes the closed session, leaves matchmaking and processes departure from its associated
     * games.
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        Long userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            matchmakingService.removeFromQueue(userId);
        }
        Set<Long> games = sessionGames.remove(sessionId);
        if (userId == null || games == null || games.isEmpty()) {
            return;
        }

        for (Long gameId : games) {
            handlePlayerLeft(gameId, userId);
        }
    }

    /**
     * Cancels pending technical defeat and reports whether the player had been considered
     * disconnected before this call.
     */
    public boolean markPresent(Long gameId, Long userId) {
        String key = key(gameId, userId);
        boolean wasDisconnected = pendingDefeats.containsKey(key) || disconnectedAt.containsKey(key);
        cancelDefeat(key);
        return wasDisconnected;
    }

    /**
     * Checks participation in an active human game, publishes disconnection and schedules defeat
     * after the reconnect grace period. Does not create a second timer for the same game/user pair.
     */
    private void handlePlayerLeft(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getStatus() != GameStatus.IN_PROGRESS || game.getMode() == GameMode.BOT) {
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

    /**
     * Removes the waiting marker, requests completion through GameService and publishes the result.
     * Safely skips obsolete scheduled actions.
     */
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

    /**
     * Cancels the specific delayed defeat task and removes its recorded disconnection time.
     */
    private void cancelDefeat(String key) {
        ScheduledFuture<?> future = pendingDefeats.remove(key);
        if (future != null) {
            future.cancel(false);
        }
        disconnectedAt.remove(key);
    }

    /**
     * Checks whether the user belongs to the White or Black side of the game.
     */
    private boolean isParticipant(Game game, Long userId) {
        return (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId))
                || (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId));
    }

    /**
     * Extracts a numeric identifier from a game STOMP destination. Returns null when the suffix is
     * not a valid game id.
     */
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

    /**
     * Reads the id from a validated SecurityUser inside Authentication. An incompatible principal
     * is not treated as a participant.
     */
    private Long resolveUserId(Principal principal) {
        if (principal instanceof org.springframework.security.core.Authentication auth
                && auth.getPrincipal() instanceof SecurityUser user) {
            return user.getId();
        }
        return null;
    }

    /**
     * Combines game and user ids into one key so independent reconnection deadlines cannot overlap.
     */
    private static String key(Long gameId, Long userId) {
        return gameId + ":" + userId;
    }
}
