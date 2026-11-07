package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit checks for DrawState. Each test specifies a behavior or regression boundary; helper methods
 * build the required test data.
 */
class DrawStateTest {
    private final CheckersRulesEngine rules = new CheckersRulesEngine();

    /**
     * Builds a readable ending position from coordinate/piece-code pairs.
     */
    private Board board(String... entries) {
        Board board = new Board(PieceColor.WHITE);
        for (int i = 0; i < entries.length; i += 2) board.set(Square.parse(entries[i]), Piece.fromCode(entries[i + 1]));
        return board;
    }

    /**
     * Applies a legal move through the real engine and records it in draw history. Reports the
     * engine's rejection immediately if the test setup is invalid.
     */
    private Board move(DrawState state, Board before, String from, String to) {
        MoveResult result = rules.applyMove(before, from, to);
        assertTrue(result.success(), from + "-" + to + ": " + result.error());
        state.record(before, result.move(), result.board());
        return result.board();
    }

    /**
     * Checks that the third occurrence includes the starting position and that repetition counts
     * survive JSON restoration.
     */
    @Test
    void thirdOccurrenceIncludesTheStartingPositionAndSurvivesSerialization() {
        Board board = board("a1", "WK", "h6", "BK");
        DrawState state = DrawState.start(board);
        for (int cycle = 0; cycle < 2; cycle++) {
            board = move(state, board, "a1", "b2");
            board = move(state, board, "h6", "g5");
            board = move(state, board, "b2", "a1");
            board = move(state, board, "g5", "h6");
            state = DrawState.fromJson(state.toJson());
            assertEquals(cycle == 0 ? null : "DRAW_REPETITION", state.reason(board, rules));
        }
    }

    /**
     * Checks that a different side to move or promoted piece produces a different repetition key.
     */
    @Test
    void repetitionDistinguishesSideToMoveAndPieceTypes() {
        Board original = board("a1", "WK", "h6", "BK");
        Board changed = original.copy();
        changed.switchTurn();
        assertNotEquals(DrawState.positionKey(original), DrawState.positionKey(changed));
        changed = original.copy();
        changed.set(Square.parse("a1"), Piece.fromCode("WM"));
        assertNotEquals(DrawState.positionKey(original), DrawState.positionKey(changed));
    }

    /**
     * Checks the fifteen/twenty-five move boundaries for the relevant material totals. Both sides
     * must independently reach their limits.
     */
    @ParameterizedTest
    @CsvSource({"4,15", "5,15", "6,25", "7,25"})
    void kingsOnlyNeedsTheRequiredNumberOfMovesFromBothSides(int pieces, int limit) {
        Board board = board("c1", "WK", "f8", "BK", "h2", "WM", "a7", "BM");
        String[] extra = {"f2", "d2", "h4"};
        for (int i = 0; i < pieces - 4; i++) board.set(Square.parse(extra[i]), Piece.fromCode("WM"));
        DrawState state = DrawState.start(board);
        state.setWhiteQuiet(limit - 1); state.setBlackQuiet(limit - 1);
        board = move(state, board, "c1", "b2");
        assertNull(state.reason(board, rules), "One side alone must not exhaust the counter");
        board = move(state, board, "f8", "g7");
        assertEquals("DRAW_KINGS_" + limit, state.reason(board, rules));
    }

    /**
     * Checks independent White and Black counters for the five/thirty/sixty material-stability
     * limits.
     */
    @ParameterizedTest
    @CsvSource({"2,5", "3,5", "4,30", "5,30", "6,60", "7,60"})
    void unchangedMaterialHasIndependentPerSideCounters(int pieces, int limit) {
        Board board = board("c1", "WK", "f8", "BK");
        String[] extra = {"h2", "a7", "f2", "d2", "h4"};
        for (int i = 0; i < pieces - 2; i++) board.set(Square.parse(extra[i]), Piece.fromCode(i == 1 ? "BM" : "WM"));
        DrawState state = DrawState.start(board);
        state.setWhiteStable(limit - 1); state.setBlackStable(limit - 1);
        board = move(state, board, "c1", "b2");
        assertNull(state.reason(board, rules));
        board = move(state, board, "f8", "g7");
        assertEquals("DRAW_MATERIAL_" + limit, state.reason(board, rules));
    }

    /**
     * Checks distinct resets: a man move resets the kings-only clock; promotion also resets
     * material stability.
     */
    @Test
    void manMovesResetOnlyTheKingsClockAndPromotionResetsBoth() {
        Board board = board("c1", "WK", "f8", "BK", "h2", "WM", "a7", "BM");
        DrawState state = DrawState.start(board);
        state.setWhiteQuiet(14); state.setBlackQuiet(14);
        state.setWhiteStable(20); state.setBlackStable(20);
        board = move(state, board, "h2", "g3");
        assertEquals(0, state.getWhiteQuiet()); assertEquals(0, state.getBlackQuiet());
        assertEquals(21, state.getWhiteStable()); assertEquals(20, state.getBlackStable());
        Board promotion = board("a1", "WK", "h6", "BK", "c7", "WM");
        state.setWhiteStable(4); state.setBlackStable(4);
        move(state, promotion, "c7", "b8");
        assertEquals(0, state.getWhiteStable()); assertEquals(0, state.getBlackStable());
    }

    /**
     * Checks counter resets after capture and the requirement that each side has a king for a
     * material-stability draw.
     */
    @Test
    void captureResetsClocksAndMaterialDrawRequiresKingsOnBothSides() {
        Board board = board("a1", "WK", "b2", "BM", "h6", "BK");
        DrawState state = DrawState.start(board);
        state.setWhiteQuiet(24); state.setBlackQuiet(24); state.setWhiteStable(59); state.setBlackStable(59);
        board = move(state, board, "a1", "c3");
        assertEquals(0, state.getWhiteQuiet()); assertEquals(0, state.getBlackStable());
        assertNull(state.reason(board, rules));
        Board men = board("a1", "WM", "h8", "BM");
        state.setWhiteStable(100); state.setBlackStable(100);
        assertNull(state.reason(men, rules));
    }

    /**
     * Checks that only the attacking side's turns count in the three-kings ending and that an
     * actual winning capture takes precedence.
     */
    @Test
    void threeKingsCountOnlyAttackingMovesAndAnActualWinTakesPriority() {
        Board board = board("c1", "WK", "e1", "WK", "g1", "WK", "f8", "BK");
        board.setTurn(PieceColor.BLACK);
        DrawState state = DrawState.start(board); state.setHunterMoves(14);
        board = move(state, board, "f8", "g7");
        assertEquals(14, state.getHunterMoves()); assertNull(state.reason(board, rules));
        board = move(state, board, "c1", "b2");
        assertEquals("DRAW_THREE_KINGS_15", state.reason(board, rules));
        Board winning = board("a1", "WK", "c1", "WK", "e1", "WK", "b2", "BK");
        state = DrawState.start(winning); state.setHunterMoves(14);
        winning = move(state, winning, "a1", "c3");
        assertEquals(GameOutcome.WHITE_WINS, winning.outcome());
        assertNull(state.reason(winning, rules));
    }

    /**
     * Checks the long-diagonal condition, all three attackers' placement and the
     * tactical-continuation exception.
     */
    @Test
    void roadRuleRequiresTheLoneKingOnTheRoadAndNoAttackerOnIt() {
        Board road = board("c1", "WK", "e1", "WK", "g1", "WK", "h8", "BK");
        DrawState state = DrawState.start(road); state.setRoadMoves(4);
        road = move(state, road, "c1", "d2");
        assertEquals("DRAW_LONG_DIAGONAL_5", state.reason(road, rules));
        Board away = road.copy(); away.clear(Square.parse("h8")); away.set(Square.parse("h6"), Piece.fromCode("BK"));
        assertNull(state.reason(away, rules));
        Board controlled = road.copy(); controlled.clear(Square.parse("d2")); controlled.set(Square.parse("a1"), Piece.fromCode("WK"));
        assertNull(state.reason(controlled, rules));
        Board tactic = board("c3", "BK", "b4", "WK", "h2", "WK", "f2", "WK");
        assertNull(state.reason(tactic, rules), "A forced tactical continuation is an exception to the road clock");
    }

    /**
     * Checks recognition of every automatic draw code while excluding resignation and missing
     * reasons.
     */
    @Test
    void everyAutomaticDrawReasonCountsAsADraw() {
        for (String reason : new String[]{"DRAW", "DRAW_REPETITION", "DRAW_KINGS_15", "DRAW_KINGS_25", "DRAW_MATERIAL_5", "DRAW_MATERIAL_30", "DRAW_MATERIAL_60", "DRAW_THREE_KINGS_15", "DRAW_LONG_DIAGONAL_5"}) {
            assertTrue(DrawState.isDrawReason(reason));
        }
        assertFalse(DrawState.isDrawReason("RESIGN")); assertFalse(DrawState.isDrawReason(null));
    }
}
