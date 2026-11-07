package com.checkers.service;

import com.checkers.engine.Board;
import com.checkers.engine.CheckersRulesEngine;
import com.checkers.engine.EngineMove;
import com.checkers.engine.GameOutcome;
import com.checkers.engine.MoveResult;
import com.checkers.engine.Piece;
import com.checkers.engine.PieceType;
import com.checkers.engine.Square;
import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.PieceColor;
import com.checkers.websocket.dto.MoveMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Minimax bot with alpha-beta pruning. EASY/MEDIUM/HARD search two/four/six plies and choose
 * randomly among the top five/three/one candidates.
 */
@Service
public class BotService {

    // Heuristic units, not win probabilities: a king is valued at about 2.8 men.
    private static final int MAN_VALUE = 100;
    private static final int KING_VALUE = 280;
    private static final int MOBILITY_WEIGHT = 3;
    private static final int WIN_SCORE = 100_000;

    private final CheckersRulesEngine rulesEngine = new CheckersRulesEngine();

    /**
     * Scores legal continuations with minimax and alpha-beta pruning. Difficulty controls depth and
     * how many top candidates participate in the random choice. Returns null when no legal move
     * exists.
     */
    public EngineMove chooseMove(Board board, BotDifficulty difficulty) {
        List<EngineMove> legal = rulesEngine.legalMoves(board);
        if (legal.isEmpty()) {
            return null;
        }

        BotDifficulty level = difficulty == null ? BotDifficulty.MEDIUM : difficulty;
        int depth = depthFor(level);
        int topN = topNFor(level);
        PieceColor botColor = board.turn();

        List<ScoredMove> scored = new ArrayList<>(legal.size());
        int alpha = Integer.MIN_VALUE / 2;
        int beta = Integer.MAX_VALUE / 2;

        // At the root, try a bot move and then assume the opponent chooses its strongest reply.
        for (EngineMove move : legal) {
            Board next = apply(board, move);
            int score = minimax(next, depth - 1, alpha, beta, false, botColor);
            scored.add(new ScoredMove(move, score));
            alpha = Math.max(alpha, score);
        }

        // Apply randomness after search, and only within the top-scored candidate group.
        scored.sort(Comparator.comparingInt(ScoredMove::score).reversed());
        int limit = Math.min(topN, scored.size());
        return scored.get(ThreadLocalRandom.current().nextInt(limit)).move();
    }

    /**
     * Retains an unsupported legacy signature and throws UnsupportedOperationException. The working
     * move-selection method accepts a Board rather than a game id.
     */
    public MoveMessage chooseMove(Long gameId, BotDifficulty difficulty) {
        throw new UnsupportedOperationException("Use chooseMove(Board, BotDifficulty)");
    }

    /**
     * Returns a small randomized response delay for the chosen difficulty. This presentation delay
     * is separate from search depth.
     */
    public long thinkingDelayMs(BotDifficulty difficulty) {
        return switch (difficulty == null ? BotDifficulty.MEDIUM : difficulty) {
            case EASY -> 300L + ThreadLocalRandom.current().nextLong(400);
            case MEDIUM -> 600L + ThreadLocalRandom.current().nextLong(700);
            case HARD -> 900L + ThreadLocalRandom.current().nextLong(900);
        };
    }

    /**
     * Heuristically recognizes a balanced king ending without an immediate capture. Used only to
     * offer or accept an agreed draw, never to force an automatic result.
     */
    public boolean considersDraw(Board board) {
        int pieces = board.countPieces(PieceColor.WHITE) + board.countPieces(PieceColor.BLACK);
        boolean whiteKing = board.occupiedAlgebraic().containsValue("WK");
        boolean blackKing = board.occupiedAlgebraic().containsValue("BK");
        return pieces <= 7 && whiteKing && blackKing
                && rulesEngine.legalMoves(board).stream().noneMatch(EngineMove::isCapture)
                && Math.abs(evaluate(board, PieceColor.BLACK)) <= 60;
    }

    /**
     * Alternates the bot's maximizing turns with the opponent's minimizing replies until the depth
     * limit. Alpha and beta bound known alternatives; branches that cannot improve the ancestor's
     * choice are skipped.
     */
    private int minimax(Board board, int depth, int alpha, int beta, boolean maximizing, PieceColor botColor) {
        rulesEngine.refreshOutcome(board);
        GameOutcome outcome = board.outcome();
        if (outcome != GameOutcome.IN_PROGRESS) {
            return terminalScore(outcome, botColor);
        }
        // At the search horizon there is no proved result, so use the static heuristic evaluation.
        if (depth <= 0) {
            return evaluate(board, botColor);
        }

        List<EngineMove> moves = rulesEngine.legalMoves(board);
        if (moves.isEmpty()) {
            return evaluate(board, botColor);
        }

        // On a bot turn maximize the score; alpha bounds the best result already achievable.
        if (maximizing) {
            int best = Integer.MIN_VALUE / 2;
            for (EngineMove move : moves) {
                int score = minimax(apply(board, move), depth - 1, alpha, beta, false, botColor);
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) {
                    // This branch cannot change the ancestor's choice, so skip its remaining
                    // alternatives.
                    break;
                }
            }
            return best;
        }

        // On an opponent turn minimize the score, assuming play against the bot's interests.
        int best = Integer.MAX_VALUE / 2;
        for (EngineMove move : moves) {
            int score = minimax(apply(board, move), depth - 1, alpha, beta, true, botColor);
            best = Math.min(best, score);
            beta = Math.min(beta, best);
            if (beta <= alpha) {
                break;
            }
        }
        return best;
    }

    /**
     * Applies a search move through the same engine used for real games. An engine-generated move
     * being rejected indicates an internal inconsistency.
     */
    private Board apply(Board board, EngineMove move) {
        MoveResult result = rulesEngine.applyMove(board, move.pathAlgebraic());
        if (!result.success()) {
            throw new IllegalStateException("Illegal move generated by engine: " + move);
        }
        return result.board();
    }

    /**
     * Estimates the position from the bot's perspective using material, man advancement and
     * mobility difference. Kings are worth more than men; a positive score favors the bot.
     */
    private int evaluate(Board board, PieceColor botColor) {
        int material = 0;
        for (int r = 0; r < Square.SIZE; r++) {
            for (int f = 0; f < Square.SIZE; f++) {
                Square sq = Square.of(f, r);
                Piece piece = board.get(sq);
                if (piece == null) {
                    continue;
                }
                int value = piece.type() == PieceType.KING ? KING_VALUE : MAN_VALUE;
                // Give men a small bonus for advancement toward promotion.
                if (piece.type() == PieceType.MAN) {
                    value += piece.color() == PieceColor.WHITE ? r * 2 : (7 - r) * 2;
                }
                material += piece.color() == botColor ? value : -value;
            }
        }
        // Mobility favors the side with more available alternatives.
        int myMoves = countMoves(board, botColor);
        int oppMoves = countMoves(board, botColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE);
        return material + MOBILITY_WEIGHT * (myMoves - oppMoves);
    }

    /**
     * Counts legal moves for the supplied side on a board copy without changing the actual side to
     * move.
     */
    private int countMoves(Board board, PieceColor side) {
        Board probe = board.copy();
        probe.setTurn(side);
        return rulesEngine.legalMoves(probe).size();
    }

    /**
     * Converts a finished outcome to a large positive or negative bot-relative score, or zero for a
     * draw.
     */
    private int terminalScore(GameOutcome outcome, PieceColor botColor) {
        return switch (outcome) {
            case WHITE_WINS -> botColor == PieceColor.WHITE ? WIN_SCORE : -WIN_SCORE;
            case BLACK_WINS -> botColor == PieceColor.BLACK ? WIN_SCORE : -WIN_SCORE;
            case DRAW -> 0;
            case IN_PROGRESS -> 0;
        };
    }

    /**
     * Sets minimax depth to two plies for EASY, four for MEDIUM and six for HARD.
     */
    private int depthFor(BotDifficulty level) {
        return switch (level) {
            case EASY -> 2;
            case MEDIUM -> 4;
            case HARD -> 6;
        };
    }

    /**
     * Sets the best-candidate group size to five, three or one. Easy and medium retain variation;
     * hard selects the highest-scored candidate.
     */
    private int topNFor(BotDifficulty level) {
        return switch (level) {
            case EASY -> 5;
            case MEDIUM -> 3;
            case HARD -> 1;
        };
    }

    /**
     * Internal bot candidate paired with its search score for sorting.
     */
    private record ScoredMove(EngineMove move, int score) {
    }
}
