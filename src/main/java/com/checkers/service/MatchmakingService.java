package com.checkers.service;

import com.checkers.config.GameProperties;
import com.checkers.engine.CheckersRulesEngine;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.User;
import com.checkers.model.enums.EventType;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.PieceColor;
import com.checkers.repository.GameRepository;
import com.checkers.repository.UserRepository;
import com.checkers.security.CustomUserDetailsService;
import com.checkers.websocket.GameEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * In-memory matchmaking queue ({@link ConcurrentLinkedQueue}).
 * <p>
 * For horizontal scaling across multiple app instances, replace this with a
 * shared store such as Redis (LIST / ZSET + pub-sub for MATCHMAKING_* events).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingService {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, Instant> queuedAt = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final GameEventPublisher eventPublisher;
    private final GameProperties gameProperties;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final CheckersRulesEngine rulesEngine = new CheckersRulesEngine();

    public void enqueue() {
        enqueue(CustomUserDetailsService.requireCurrentUserId());
    }

    public void enqueue(Long userId) {
        if (queue.contains(userId)) {
            return;
        }
        queue.offer(userId);
        queuedAt.put(userId, Instant.now());
        scheduleTimeout(userId);
    }

    public void dequeue() {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        removeFromQueue(userId);
    }

    public void removeFromQueue(Long userId) {
        queue.remove(userId);
        queuedAt.remove(userId);
        cancelTimeout(userId);
    }

    @Transactional
    public Game tryMatch() {
        synchronized (queue) {
            Long first = pollValid();
            if (first == null) {
                return null;
            }
            Long second = pollValid();
            if (second == null) {
                queue.offer(first);
                return null;
            }
            if (first.equals(second)) {
                queue.offer(first);
                return null;
            }

            cancelTimeout(first);
            cancelTimeout(second);
            queuedAt.remove(first);
            queuedAt.remove(second);

            User white = userRepository.findById(first)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            User black = userRepository.findById(second)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            Game game = Game.builder()
                    .mode(GameMode.MATCHMAKING)
                    .status(GameStatus.IN_PROGRESS)
                    .whitePlayer(white)
                    .blackPlayer(black)
                    .currentTurn(PieceColor.WHITE)
                    .boardState(rulesEngine.createInitialBoard().toJson())
                    .startedAt(Instant.now())
                    .build();
            return gameRepository.save(game);
        }
    }

    public boolean isQueued(Long userId) {
        return queue.contains(userId);
    }

    private Long pollValid() {
        Long id;
        while ((id = queue.poll()) != null) {
            if (queuedAt.containsKey(id)) {
                return id;
            }
        }
        return null;
    }

    private void scheduleTimeout(Long userId) {
        cancelTimeout(userId);
        Duration timeout = Duration.ofSeconds(gameProperties.getMatchmakingTimeoutSeconds());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> onTimeout(userId),
                Instant.now().plus(timeout));
        timeoutTasks.put(userId, future);
    }

    private void cancelTimeout(Long userId) {
        ScheduledFuture<?> future = timeoutTasks.remove(userId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void onTimeout(Long userId) {
        if (!queue.contains(userId) && !queuedAt.containsKey(userId)) {
            return;
        }
        removeFromQueue(userId);
        userRepository.findById(userId).ifPresent(user -> {
            log.debug("Matchmaking timeout for user {}", user.getUsername());
            eventPublisher.publishMatchmaking(user.getUsername(), EventType.MATCHMAKING_TIMEOUT, Map.of(
                    "userId", userId,
                    "timeoutSeconds", gameProperties.getMatchmakingTimeoutSeconds()
            ));
        });
    }
}
