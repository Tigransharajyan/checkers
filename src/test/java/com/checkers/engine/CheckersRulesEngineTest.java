package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckersRulesEngineTest {

    private CheckersRulesEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CheckersRulesEngine();
    }

    @Test
    void quietMove_forwardDiagonal_succeeds() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "c3", "d4");

        assertTrue(result.success());
        assertNull(result.board().get(Square.parse("c3")));
        assertNotNull(result.board().get(Square.parse("d4")));
        assertEquals(PieceColor.BLACK, result.board().turn());
    }

    @Test
    void quietMove_backward_rejectedForMan() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "c3", "b2");

        assertFalse(result.success());
    }

    @Test
    void capture_manCanCaptureBackward() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c5"), man(PieceColor.WHITE));
        board.set(Square.parse("b4"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, "c5", "a3");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertNull(result.board().get(Square.parse("b4")));
        assertNotNull(result.board().get(Square.parse("a3")));
        assertTrue(result.move().isCapture());
    }

    @Test
    void captureMandatory_quietMoveRejectedWhenCaptureExists() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));
        board.set(Square.parse("d4"), man(PieceColor.BLACK));
        board.set(Square.parse("a1"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "a1", "b2");

        assertFalse(result.success());
        assertEquals(MoveError.CAPTURE_MANDATORY, result.error());
    }

    @Test
    void multiCapture_chainInOneMove() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("b2"), man(PieceColor.WHITE));
        board.set(Square.parse("c3"), man(PieceColor.BLACK));
        board.set(Square.parse("e5"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, List.of("b2", "d4", "f6"));

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertNull(result.board().get(Square.parse("c3")));
        assertNull(result.board().get(Square.parse("e5")));
        assertNotNull(result.board().get(Square.parse("f6")));
        assertEquals(2, result.move().captured().size());
    }

    @Test
    void notMaximalCapture_eitherCaptureBranchAllowed() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("d4"), man(PieceColor.WHITE));
        board.set(Square.parse("c5"), man(PieceColor.BLACK));
        board.set(Square.parse("e5"), man(PieceColor.BLACK));

        List<EngineMove> legal = engine.legalMoves(board);
        assertTrue(legal.size() >= 2);
        assertTrue(legal.stream().anyMatch(m -> m.to().equals(Square.parse("b6"))));
        assertTrue(legal.stream().anyMatch(m -> m.to().equals(Square.parse("f6"))));

        MoveResult shorter = engine.applyMove(board, "d4", "b6");
        assertTrue(shorter.success());
        assertEquals(1, shorter.move().captured().size());
    }

    @Test
    void king_capturesAtDistance() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("a1"), king(PieceColor.WHITE));
        board.set(Square.parse("d4"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, "a1", "f6");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertNull(result.board().get(Square.parse("d4")));
        assertEquals(PieceType.KING, result.board().get(Square.parse("f6")).type());
    }

    @Test
    void king_quietLongDiagonal() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("a1"), king(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "a1", "e5");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertNotNull(result.board().get(Square.parse("e5")));
    }

    @Test
    void promotion_manBecomesKingOnLastRank() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c7"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "c7", "b8");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertTrue(result.board().get(Square.parse("b8")).isKing());
        assertTrue(result.move().promotion());
    }

    @Test
    void winner_whenOpponentHasNoPieces() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));
        board.set(Square.parse("d4"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, "c3", "e5");

        assertTrue(result.success());
        assertEquals(GameOutcome.WHITE_WINS, result.board().outcome());
    }

    @Test
    void winner_whenOpponentHasNoLegalMoves() {
        Board board = emptyBoard(PieceColor.BLACK);
        board.set(Square.parse("a1"), man(PieceColor.WHITE));
        board.set(Square.parse("b2"), man(PieceColor.BLACK));
        board.set(Square.parse("c1"), man(PieceColor.WHITE));
        board.set(Square.parse("a3"), man(PieceColor.WHITE));

        engine.refreshOutcome(board);
        assertEquals(GameOutcome.WHITE_WINS, board.outcome());
    }

    @Test
    void initialBoard_hasLegalOpeningMoves() {
        Board board = engine.createInitialBoard();
        List<EngineMove> moves = engine.legalMoves(board);
        assertFalse(moves.isEmpty());
        assertTrue(moves.stream().noneMatch(EngineMove::isCapture));
    }

    @Test
    void board_roundTripJson() {
        Board board = engine.createInitialBoard();
        Board restored = Board.fromJson(board.toJson());
        assertEquals(board.turn(), restored.turn());
        assertEquals(board.countPieces(PieceColor.WHITE), restored.countPieces(PieceColor.WHITE));
        assertEquals(board.countPieces(PieceColor.BLACK), restored.countPieces(PieceColor.BLACK));
    }

    private static Board emptyBoard(PieceColor turn) {
        return new Board(turn);
    }

    private static Piece man(PieceColor color) {
        return new Piece(color, PieceType.MAN);
    }

    private static Piece king(PieceColor color) {
        return new Piece(color, PieceType.KING);
    }
}
