package com.checkers.puzzle;

import com.checkers.engine.*;
import com.checkers.model.enums.PieceColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact bounded proof search for a White win. Unlike the bot's heuristic minimax score, -1 means
 * that a win was not proved against every defense within the depth budget.
 */
public final class PuzzleSolver {
    private final CheckersRulesEngine rules;
    private final Map<String, Integer> proof = new ConcurrentHashMap<>();

    /**
     * Receives the rules engine used to validate every explored puzzle continuation.
     */
    public PuzzleSolver(CheckersRulesEngine rules) {
        this.rules = rules;
    }

    /**
     * Proves a White win within the remaining ply budget. White selects the shortest proven win;
     * Black selects the longest defense. Any defense not proven losing within the budget yields -1.
     * Caches results by position and remaining depth.
     */
    public int distance(Board board, int remaining) {
        if (board.outcome() == GameOutcome.WHITE_WINS) return 0;
        if (board.outcome() != GameOutcome.IN_PROGRESS || remaining <= 0) return -1;
        // Include remaining depth in the key: a five-ply proof does not imply a three-ply proof.
        String key = remaining + ":" + board.toJson();
        Integer known = proof.get(key);
        if (known != null) return known;
        boolean white = board.turn() == PieceColor.WHITE;
        int best = white ? Integer.MAX_VALUE : -1;
        for (EngineMove move : rules.legalMoves(board)) {
            Board next = rules.applyMove(board, move.pathAlgebraic()).board();
            int distance = distance(next, remaining - 1);
            if (white) {
                // White needs at least one continuation with a proven win.
                if (distance >= 0) best = Math.min(best, distance + 1);
                if (best == 1) break;
            } else {
                // Every Black defense must be proved losing; one unproved branch invalidates the
                // proof.
                if (distance < 0) { best = -1; break; }
                best = Math.max(best, distance + 1);
            }
        }
        if (best == Integer.MAX_VALUE) best = -1;
        proof.putIfAbsent(key, best);
        return best;
    }
}
