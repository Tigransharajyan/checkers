package com.checkers.puzzle;

import com.checkers.engine.*;
import com.checkers.model.enums.PieceColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Exact bounded proof of a white win, using the regular game's rules engine. */
public final class PuzzleSolver {
    private final CheckersRulesEngine rules;
    private final Map<String, Integer> proof = new ConcurrentHashMap<>();

    public PuzzleSolver(CheckersRulesEngine rules) {
        this.rules = rules;
    }

    /** Minimum plies to win against the longest legal defence, or -1 if unproved. */
    public int distance(Board board, int remaining) {
        if (board.outcome() == GameOutcome.WHITE_WINS) return 0;
        if (board.outcome() != GameOutcome.IN_PROGRESS || remaining <= 0) return -1;
        String key = remaining + ":" + board.toJson();
        Integer known = proof.get(key);
        if (known != null) return known;
        boolean white = board.turn() == PieceColor.WHITE;
        int best = white ? Integer.MAX_VALUE : -1;
        for (EngineMove move : rules.legalMoves(board)) {
            Board next = rules.applyMove(board, move.pathAlgebraic()).board();
            int distance = distance(next, remaining - 1);
            if (white) {
                if (distance >= 0) best = Math.min(best, distance + 1);
                if (best == 1) break;
            } else {
                if (distance < 0) { best = -1; break; }
                best = Math.max(best, distance + 1);
            }
        }
        if (best == Integer.MAX_VALUE) best = -1;
        proof.putIfAbsent(key, best);
        return best;
    }
}
