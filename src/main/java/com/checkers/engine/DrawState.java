package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent draw history for actual game moves. Kept separate from Board so speculative bot-search
 * positions cannot increment real counters or repetitions.
 */
@Getter
@Setter
@NoArgsConstructor
public final class DrawState {
    private static final ObjectMapper JSON = new ObjectMapper();
    // Keys encode pieces and side to move; values count occurrences of that exact position.
    private Map<String, Integer> repetitions = new HashMap<>();
    // Quiet counts capture-free king moves; Stable counts moves without material changes.
    private int whiteQuiet, blackQuiet, whiteStable, blackStable;
    // Hunter/road count only attacking turns. Plies counts every actual move by either side.
    private int hunterMoves, roadMoves, plies;
    private int lastBotOfferPly = -20;

    /**
     * Creates draw history and records the initial position as its first occurrence for threefold
     * repetition.
     */
    public static DrawState start(Board board) {
        DrawState state = new DrawState();
        state.repetitions.put(positionKey(board), 1);
        return state;
    }

    /**
     * Records one confirmed ply and updates repetitions and independent counters for both sides.
     * Man moves reset the kings-only clock; captures and promotion also reset the
     * material-stability counters.
     */
    public void record(Board before, EngineMove move, Board after) {
        plies++;
        Piece moving = before.get(move.from());
        boolean irreversible = move.isCapture() || move.promotion();
        // An irreversible change prevents the earlier position from recurring with the same pieces.
        if (irreversible || moving == null || !moving.isKing()) {
            whiteQuiet = blackQuiet = 0;
            repetitions.clear();
        } else if (before.turn() == PieceColor.WHITE) {
            whiteQuiet++;
        } else {
            blackQuiet++;
        }
        repetitions.merge(positionKey(after), 1, Integer::sum);

        // Material-stability clocks are independent: ordinary man movement does not change
        // material.
        Material material = material(after);
        if (irreversible || !material.bothKings() || material.total() > 7) {
            whiteStable = blackStable = 0;
        } else if (before.turn() == PieceColor.WHITE) {
            whiteStable++;
        } else {
            blackStable++;
        }

        // A different attacking side or changed material restarts the special ending clock.
        PieceColor hunter = hunter(after);
        if (irreversible || hunter == null || hunter != hunter(before)) hunterMoves = 0;
        else if (before.turn() == hunter) hunterMoves++;

        PieceColor roadAttacker = roadAttacker(after);
        if (irreversible || roadAttacker == null || roadAttacker != roadAttacker(before)) roadMoves = 0;
        else if (before.turn() == roadAttacker) roadMoves++;
    }

    /**
     * Returns the exact automatic-draw reason or null. Preserves an existing victory before
     * checking repetition and move limits; no approximate bot evaluation participates here.
     */
    public String reason(Board board, CheckersRulesEngine rules) {
        // Capturing the last piece or blocking every legal move takes precedence over a draw.
        if (board.outcome() != GameOutcome.IN_PROGRESS) return null;
        if (repetitions.getOrDefault(positionKey(board), 0) >= 3) return "DRAW_REPETITION";
        Material material = material(board);
        if (hunter(board) != null && hunterMoves >= 15) return "DRAW_THREE_KINGS_15";
        if (roadAttacker(board) != null && roadMoves >= 5 && !hasTacticalContinuation(board, rules)) {
            return "DRAW_LONG_DIAGONAL_5";
        }
        if (material.total() >= 4 && material.total() <= 5 && whiteQuiet >= 15 && blackQuiet >= 15) {
            return "DRAW_KINGS_15";
        }
        if (material.total() >= 6 && material.total() <= 7 && whiteQuiet >= 25 && blackQuiet >= 25) {
            return "DRAW_KINGS_25";
        }
        // Moves by each side require two independent thresholds, not one combined ply counter.
        if (material.bothKings()) {
            int limit = material.total() <= 3 ? 5 : material.total() <= 5 ? 30 : material.total() <= 7 ? 60 : 0;
            if (limit > 0 && whiteStable >= limit && blackStable >= limit) return "DRAW_MATERIAL_" + limit;
        }
        return null;
    }

    /**
     * Checks both sides for captures on temporary boards. Such tactical continuations postpone the
     * special five-move long-diagonal draw.
     */
    private static boolean hasTacticalContinuation(Board board, CheckersRulesEngine rules) {
        for (PieceColor side : PieceColor.values()) {
            Board probe = board.copy();
            probe.setTurn(side);
            if (rules.legalMoves(probe).stream().anyMatch(EngineMove::isCapture)) return true;
        }
        return false;
    }

    /**
     * Requires five material-stable moves from each side and a twenty-ply interval since the
     * previous bot offer. Permits an offer only, never a forced draw.
     */
    public boolean mayOfferBotDraw() {
        return whiteStable >= 5 && blackStable >= 5 && plies - lastBotOfferPly >= 20;
    }

    /**
     * Builds a stable key from the side to move and piece colors/types in fixed dark-square order.
     * Excludes timestamps, user data and counters.
     */
    public static String positionKey(Board board) {
        // Fixed square order makes the key independent of JSON or Map iteration order.
        StringBuilder key = new StringBuilder(board.turn().name()).append(':');
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Square square = Square.of(file, rank);
                if (!square.isDark()) continue;
                Piece piece = board.get(square);
                key.append(piece == null ? "--" : piece.code());
            }
        }
        return key.toString();
    }

    /**
     * Finds the side with at least three kings against a single enemy king. Returns null outside
     * that ending.
     */
    private static PieceColor hunter(Board board) {
        Material m = material(board);
        if (m.whiteKings >= 3 && m.black == 1 && m.blackKings == 1) return PieceColor.WHITE;
        if (m.blackKings >= 3 && m.white == 1 && m.whiteKings == 1) return PieceColor.BLACK;
        return null;
    }

    /**
     * Finds the attacking side with three pieces off the a1-h8 long diagonal against a lone king on
     * it. An attacking piece on that diagonal disables this special condition.
     */
    private static PieceColor roadAttacker(Board board) {
        Material m = material(board);
        PieceColor attacker = m.white == 3 && m.black == 1 && m.blackKings == 1 ? PieceColor.WHITE
                : m.black == 3 && m.white == 1 && m.whiteKings == 1 ? PieceColor.BLACK : null;
        if (attacker == null) return null;
        boolean defenderOnRoad = false;
        for (int index = 0; index < 8; index++) {
            Piece piece = board.get(Square.of(index, index));
            if (piece == null) continue;
            if (piece.color() == attacker) return null;
            defenderOnRoad = piece.isKing();
        }
        return defenderOnRoad ? attacker : null;
    }

    /**
     * Counts all pieces and kings independently for each side. These totals determine which draw
     * limit applies.
     */
    private static Material material(Board board) {
        int white = 0, black = 0, whiteKings = 0, blackKings = 0;
        for (String code : board.occupiedAlgebraic().values()) {
            if (code.charAt(0) == 'W') { white++; if (code.charAt(1) == 'K') whiteKings++; }
            else { black++; if (code.charAt(1) == 'K') blackKings++; }
        }
        return new Material(white, black, whiteKings, blackKings);
    }

    /**
     * Recognizes both the agreement reason DRAW and automatic DRAW_* reasons. Prevents bot-game
     * draws from being counted as human losses.
     */
    public static boolean isDrawReason(String reason) {
        return reason != null && (reason.equals("DRAW") || reason.startsWith("DRAW_"));
    }

    /**
     * Serializes repetition history, counters and the last bot-offer time for durable storage with
     * the game.
     */
    public String toJson() {
        try { return JSON.writeValueAsString(this); }
        catch (Exception error) { throw new IllegalStateException("Cannot serialize draw history", error); }
    }

    /**
     * Restores persisted draw history. Rejects malformed JSON instead of silently resetting
     * counters that affect the outcome.
     */
    public static DrawState fromJson(String json) {
        try { return JSON.readValue(json, DrawState.class); }
        catch (Exception error) { throw new IllegalStateException("Invalid draw history", error); }
    }

    /**
     * Internal ending profile containing total pieces and kings for each side. It is not a
     * standalone board position.
     */
    private record Material(int white, int black, int whiteKings, int blackKings) {
        /**
         * Returns the combined number of pieces in this material profile.
         */
        int total() { return white + black; }
        /**
         * Reports whether both sides have at least one king, as required by the material-stability
         * limits of five, thirty or sixty moves.
         */
        boolean bothKings() { return whiteKings > 0 && blackKings > 0; }
    }
}
