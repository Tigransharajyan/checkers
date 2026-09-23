package com.checkers.service;

import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.PieceColor;
import com.checkers.repository.GameRepository;
import com.checkers.websocket.GameEventPublisher;
import com.checkers.websocket.dto.GameEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Schedules bot thinking off the request / STOMP handler thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotPlayService {

    private final GameService gameService;
    private final GameRepository gameRepository;
    private final BotService botService;
    private final GameEventPublisher eventPublisher;

    @Async("botExecutor")
    public void scheduleBotMove(Long gameId) {
        try {
            var gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                return;
            }
            var game = gameOpt.get();
            if (game.getMode() != GameMode.BOT
                    || game.getStatus() != GameStatus.IN_PROGRESS
                    || game.getCurrentTurn() != PieceColor.BLACK) {
                return;
            }

            long delay = botService.thinkingDelayMs(
                    game.getBotDifficulty() == null ? BotDifficulty.MEDIUM : game.getBotDifficulty());
            Thread.sleep(delay);

            GameEventMessage event = gameService.applyBotMoveIfNeeded(gameId);
            if (event != null) {
                eventPublisher.publishToGame(gameId, event);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.debug("Bot think interrupted for game {}", gameId);
        } catch (Exception ex) {
            log.warn("Bot move failed for game {}: {}", gameId, ex.getMessage());
        }
    }
}
