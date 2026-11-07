package com.checkers.engine;

import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/** Persistent game history, separate from the board used by the bot's search. */
@Getter
@Setter
@NoArgsConstructor
public final class DrawState {
    private static final ObjectMapper JSON = new ObjectMapper();
    private Map<String, Integer> repetitions = new HashMap<>();
    private int whiteQuiet, blackQuiet, whiteStable, blackStable;
    private int hunterMoves, roadMoves, plies;
    private int lastBotOfferPly = -20;

    public static DrawState start(Board board) {
        DrawState state = new DrawState();
        state.repetitions.put(positionKey(board), 1);
        return state;
    }

    public void record(Board before, EngineMove move, Board after) {
        plies++;
        Piece moving = before.get(move.from());
        boolean irreversible = move.isCapture() || move.promotion();
        if (irreversible || moving == null || !moving.isKing()) {
            whiteQuiet = blackQuiet = 0;
            repetitions.clear();
        } else if (before.turn() == PieceColor.WHITE) {
            whiteQuiet++;
        } else {
            blackQuiet++;
        }
        repetitions.merge(positionKey(after), 1, Integer::sum);

        Material material = material(after);
        if (irreversible || !material.bothKings() || material.total() > 7) {
            whiteStable = blackStable = 0;
        } else if (before.turn() == PieceColor.WHITE) {
            whiteStable++;
        } else {
            blackStable++;
        }

        PieceColor hunter = hunter(after);
        if (irreversible || hunter == null || hunter != hunter(before)) hunterMoves = 0;
        else if (before.turn() == hunter) hunterMoves++;

        PieceColor roadAttacker = roadAttacker(after);
        if (irreversible || roadAttacker == null || roadAttacker != roadAttacker(before)) roadMoves = 0;
        else if (before.turn() == roadAttacker) roadMoves++;
    }

    public String reason(Board board, CheckersRulesEngine rules) {
        // Capturing the last piece or blocking every move always takes precedence.
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
        if (material.bothKings()) {
            int limit = material.total() <= 3 ? 5 : material.total() <= 5 ? 30 : material.total() <= 7 ? 60 : 0;
            if (limit > 0 && whiteStable >= limit && blackStable >= limit) return "DRAW_MATERIAL_" + limit;
        }
        return null;
    }

    /** Conservative road-rule exception: finish mandatory tactical play before adjudicating. */
    private static boolean hasTacticalContinuation(Board board, CheckersRulesEngine rules) {
        for (PieceColor side : PieceColor.values()) {
            Board probe = board.copy();
            probe.setTurn(side);
            if (rules.legalMoves(probe).stream().anyMatch(EngineMove::isCapture)) return true;
        }
        return false;
    }

    public boolean mayOfferBotDraw() {
        return whiteStable >= 5 && blackStable >= 5 && plies - lastBotOfferPly >= 20;
    }

    public static String positionKey(Board board) {
        // Fixed square order, piece colour/type and the side to move; no timestamps or counters.
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

    private static PieceColor hunter(Board board) {
        Material m = material(board);
        if (m.whiteKings >= 3 && m.black == 1 && m.blackKings == 1) return PieceColor.WHITE;
        if (m.blackKings >= 3 && m.white == 1 && m.whiteKings == 1) return PieceColor.BLACK;
        return null;
    }

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

    private static Material material(Board board) {
        int white = 0, black = 0, whiteKings = 0, blackKings = 0;
        for (String code : board.occupiedAlgebraic().values()) {
            if (code.charAt(0) == 'W') { white++; if (code.charAt(1) == 'K') whiteKings++; }
            else { black++; if (code.charAt(1) == 'K') blackKings++; }
        }
        return new Material(white, black, whiteKings, blackKings);
    }

    public static boolean isDrawReason(String reason) {
        return reason != null && (reason.equals("DRAW") || reason.startsWith("DRAW_"));
    }

    public String toJson() {
        try { return JSON.writeValueAsString(this); }
        catch (Exception error) { throw new IllegalStateException("Cannot serialize draw history", error); }
    }

    public static DrawState fromJson(String json) {
        try { return JSON.readValue(json, DrawState.class); }
        catch (Exception error) { throw new IllegalStateException("Invalid draw history", error); }
    }

    private record Material(int white, int black, int whiteKings, int blackKings) {
        int total() { return white + black; }
        boolean bothKings() { return whiteKings > 0 && blackKings > 0; }
    }
}
