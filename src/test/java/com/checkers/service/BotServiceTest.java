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

class BotServiceTest {

    private BotService botService;
    private CheckersRulesEngine engine;

    @BeforeEach
    void setUp() {
        botService = new BotService();
        engine = new CheckersRulesEngine();
    }

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

    @Test
    void returnsMoveOnInitialPosition() {
        Board board = engine.createInitialBoard();
        EngineMove move = botService.chooseMove(board, BotDifficulty.EASY);
        assertNotNull(move);
        assertTrue(move.path().size() >= 2);
    }

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
