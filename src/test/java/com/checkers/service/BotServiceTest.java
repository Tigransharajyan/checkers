package com.checkers.service;

import com.checkers.engine.Board;
import com.checkers.engine.CheckersRulesEngine;
import com.checkers.engine.EngineMove;
import com.checkers.engine.Piece;
import com.checkers.engine.PieceType;
import com.checkers.engine.Square;
import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.PieceColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit checks for BotService. Each test specifies a behavior or regression boundary; helper
 * methods build the required test data.
 */
class BotServiceTest {

    private BotService botService;
    private CheckersRulesEngine engine;

    /**
     * Creates the engine and bot for isolated legality and move-selection checks.
     */
    @BeforeEach
    void setUp() {
        botService = new BotService();
        engine = new CheckersRulesEngine();
    }

    /**
     * Checks that the bot selects a mandatory capture from legal moves.
     */
    @Test
    void choosesCaptureWhenMandatory() {
        Board board = new Board(PieceColor.WHITE);
        board.set(Square.parse("c3"), new Piece(PieceColor.WHITE, PieceType.MAN));
        board.set(Square.parse("d4"), new Piece(PieceColor.BLACK, PieceType.MAN));

        EngineMove move = botService.chooseMove(board, BotDifficulty.HARD);

        assertNotNull(move);
        assertTrue(move.isCapture());
        assertTrue(engine.legalMoves(board).stream().anyMatch(m -> m.equals(move)));
    }

    /**
     * Checks that the bot can select a legal move in the standard starting position.
     */
    @Test
    void returnsMoveOnInitialPosition() {
        Board board = engine.createInitialBoard();
        EngineMove move = botService.chooseMove(board, BotDifficulty.EASY);
        assertNotNull(move);
        assertTrue(move.path().size() >= 2);
    }

    /**
     * Checks that the hard bot prefers a capture that wins the game.
     */
    @Test
    void hardPrefersWinningCapture() {
        // White can capture black's last piece
        Board board = new Board(PieceColor.WHITE);
        board.set(Square.parse("c3"), new Piece(PieceColor.WHITE, PieceType.MAN));
        board.set(Square.parse("d4"), new Piece(PieceColor.BLACK, PieceType.MAN));
        board.set(Square.parse("a1"), new Piece(PieceColor.WHITE, PieceType.MAN));

        EngineMove move = botService.chooseMove(board, BotDifficulty.HARD);
        assertNotNull(move);
        assertTrue(move.isCapture());
    }
}
