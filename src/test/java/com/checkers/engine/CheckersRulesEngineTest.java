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

/**
 * JUnit checks for CheckersRulesEngine. Each test specifies a behavior or regression boundary;
 * helper methods build the required test data.
 */
class CheckersRulesEngineTest {

    private CheckersRulesEngine engine;

    /**
     * Creates a fresh engine before each independent rules test.
     */
    @BeforeEach
    void setUp() {
        engine = new CheckersRulesEngine();
    }

    /**
     * Checks a man's legal one-square forward diagonal move.
     */
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

    /**
     * Checks rejection of a man's non-capturing backward move.
     */
    @Test
    void quietMove_backward_rejectedForMan() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "c3", "b2");

        assertFalse(result.success());
    }

    /**
     * Checks that a man may capture backward under Russian draughts rules.
     */
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

    /**
     * Checks that an available capture prevents a quiet move.
     */
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

    /**
     * Checks a complete multi-jump chain as one move and removal of every captured piece.
     */
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

    /**
     * Checks that any complete capture branch is legal, even when another branch captures more
     * pieces.
     */
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

    /**
     * Checks a flying king's capture across a diagonal distance.
     */
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

    /**
     * Checks a king's long diagonal move through empty squares without capture.
     */
    @Test
    void king_quietLongDiagonal() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("a1"), king(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "a1", "e5");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertNotNull(result.board().get(Square.parse("e5")));
    }

    /**
     * Checks promotion when a man reaches the final rank.
     */
    @Test
    void promotion_manBecomesKingOnLastRank() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c7"), man(PieceColor.WHITE));

        MoveResult result = engine.applyMove(board, "c7", "b8");

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertTrue(result.board().get(Square.parse("b8")).isKing());
        assertTrue(result.move().promotion());
    }

    /**
     * Checks victory after the opponent's last piece is captured.
     */
    @Test
    void winner_whenOpponentHasNoPieces() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("c3"), man(PieceColor.WHITE));
        board.set(Square.parse("d4"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, "c3", "e5");

        assertTrue(result.success());
        assertEquals(GameOutcome.WHITE_WINS, result.board().outcome());
    }

    /**
     * Checks defeat of a side that cannot make any legal move.
     */
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

    /**
     * Checks that the standard initial setup generates valid opening moves.
     */
    @Test
    void initialBoard_hasLegalOpeningMoves() {
        Board board = engine.createInitialBoard();
        List<EngineMove> moves = engine.legalMoves(board);
        assertFalse(moves.isEmpty());
        assertTrue(moves.stream().noneMatch(EngineMove::isCapture));
    }

    /**
     * Checks preservation of the position through Board-to-JSON serialization and restoration.
     */
    @Test
    void board_roundTripJson() {
        Board board = engine.createInitialBoard();
        Board restored = Board.fromJson(board.toJson());
        assertEquals(board.turn(), restored.turn());
        assertEquals(board.countPieces(PieceColor.WHITE), restored.countPieces(PieceColor.WHITE));
        assertEquals(board.countPieces(PieceColor.BLACK), restored.countPieces(PieceColor.BLACK));
    }

    /**
     * Checks that promotion in the middle of a chain remains recorded in the complete move result.
     */
    @Test
    void promotionDuringMultiJump_preservesPromotedFlag() {
        Board board = emptyBoard(PieceColor.WHITE);
        board.set(Square.parse("b6"), man(PieceColor.WHITE));
        board.set(Square.parse("c7"), man(PieceColor.BLACK));
        board.set(Square.parse("e7"), man(PieceColor.BLACK));

        MoveResult result = engine.applyMove(board, List.of("b6", "d8", "f6"));

        assertTrue(result.success(), () -> String.valueOf(result.error()));
        assertTrue(result.move().promotion(), "Move should be marked as promoted");
        assertNotNull(result.board().get(Square.parse("f6")));
        assertTrue(result.board().get(Square.parse("f6")).isKing(), "Landing piece should be a King");
        assertNull(result.board().get(Square.parse("c7")));
        assertNull(result.board().get(Square.parse("e7")));
    }

    /**
     * Creates an empty test board with the supplied side to move.
     */
    private static Board emptyBoard(PieceColor turn) {
        return new Board(turn);
    }

    /**
     * Places a man of the supplied color on a test-board square.
     */
    private static Piece man(PieceColor color) {
        return new Piece(color, PieceType.MAN);
    }

    /**
     * Places a king of the supplied color on a test-board square.
     */
    private static Piece king(PieceColor color) {
        return new Piece(color, PieceType.KING);
    }
}
