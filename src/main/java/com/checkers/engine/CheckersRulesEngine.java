package com.checkers.engine;

import com.checkers.model.enums.PieceColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure Java Russian draughts rules on an eight-by-eight board, independent of Spring and
 * persistence. Supports mandatory captures, any complete capture branch, flying kings and promotion
 * during a chain. One EngineMove is one ply even with multiple jumps.
 */
public final class CheckersRulesEngine {

    // File/rank offsets for the four diagonal directions.
    private static final int[][] DIAGONALS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    /**
     * Creates the standard Russian draughts starting position through Board.initial.
     */
    public Board createInitialBoard() {
        return Board.initial();
    }

    /**
     * Returns complete legal moves for the active side. Captures are generated first and suppress
     * all quiet moves when available; a finished game has no legal moves.
     */
    public List<EngineMove> legalMoves(Board board) {
        if (board.outcome() != GameOutcome.IN_PROGRESS) {
            return List.of();
        }
        // Mandatory capture is enforced here on the server, independently of interface hints.
        List<EngineMove> captures = allCaptures(board, board.turn());
        if (!captures.isEmpty()) {
            return captures;
        }
        return allQuietMoves(board, board.turn());
    }

    /**
     * Validates the submitted path against complete legal moves and applies it to a board copy.
     * Leaves the original unchanged, switches the turn after the entire chain and checks the
     * resulting outcome.
     */
    public MoveResult applyMove(Board board, List<String> pathAlgebraic) {
        if (board.outcome() != GameOutcome.IN_PROGRESS) {
            return MoveResult.fail(MoveError.GAME_OVER);
        }
        if (pathAlgebraic == null || pathAlgebraic.size() < 2) {
            return MoveResult.fail(MoveError.INVALID_PATH);
        }

        // The client supplies coordinates, but the server validates each square itself.
        List<Square> path = new ArrayList<>(pathAlgebraic.size());
        for (String alg : pathAlgebraic) {
            if (!Square.isValidAlgebraic(alg)) {
                return MoveResult.fail(MoveError.INVALID_SQUARE);
            }
            Square sq = Square.parse(alg);
            if (!sq.isDark()) {
                return MoveResult.fail(MoveError.INVALID_SQUARE);
            }
            path.add(sq);
        }

        // Compare the entire path: identical endpoints do not guarantee the same capture chain.
        List<EngineMove> legal = legalMoves(board);
        EngineMove matched = null;
        for (EngineMove candidate : legal) {
            if (candidate.path().equals(path)) {
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            return diagnoseIllegal(board, path, legal);
        }

        // Keep the original position intact for rejected requests and independent bot-search
        // branches.
        Board next = board.copy();
        applyEngineMove(next, matched);
        // The complete jump chain is one ply; switch turns only after the final landing.
        next.switchTurn();
        refreshOutcome(next);
        return MoveResult.ok(next, matched);
    }

    /**
     * Builds a two-square path from the supplied source and destination and delegates to the common
     * validation method. Capture chains with intermediate landings use the list overload.
     */
    public MoveResult applyMove(Board board, String from, String to) {
        return applyMove(board, List.of(from, to));
    }

    /**
     * Evaluates the outcome on a board copy without changing the supplied position. Persistent draw
     * counters are maintained separately in DrawState.
     */
    public GameOutcome evaluateOutcome(Board board) {
        Board copy = board.copy();
        refreshOutcome(copy);
        return copy.outcome();
    }

    /**
     * Awards victory to the opponent if a side has no pieces or no legal moves. Does not overwrite
     * an outcome that has already been established.
     */
    public void refreshOutcome(Board board) {
        if (board.outcome() != GameOutcome.IN_PROGRESS) {
            return;
        }
        if (board.countPieces(PieceColor.WHITE) == 0) {
            board.setOutcome(GameOutcome.BLACK_WINS);
            return;
        }
        if (board.countPieces(PieceColor.BLACK) == 0) {
            board.setOutcome(GameOutcome.WHITE_WINS);
            return;
        }
        // In draughts, having no legal move means defeat even when pieces remain.
        if (legalMoves(board).isEmpty()) {
            board.setOutcome(board.turn() == PieceColor.WHITE
                    ? GameOutcome.BLACK_WINS
                    : GameOutcome.WHITE_WINS);
        }
    }

    /**
     * Collects non-capturing moves for the selected side using separate generators for men and
     * flying kings.
     */
    private List<EngineMove> allQuietMoves(Board board, PieceColor side) {
        List<EngineMove> moves = new ArrayList<>();
        forEachPiece(board, side, (sq, piece) -> {
            if (piece.isKing()) {
                addKingQuietMoves(board, sq, moves);
            } else {
                addManQuietMoves(board, sq, piece, moves);
            }
        });
        return moves;
    }

    /**
     * Starts recursive capture-chain exploration from every friendly piece. Returns every complete
     * legal branch, not only the longest chain.
     */
    private List<EngineMove> allCaptures(Board board, PieceColor side) {
        List<EngineMove> moves = new ArrayList<>();
        forEachPiece(board, side, (sq, piece) ->
                exploreCaptures(board, sq, piece, List.of(sq), List.of(), Set.of(), false, moves));
        return moves;
    }

    /**
     * Adds one-square forward diagonal moves onto empty dark squares. Marks promotion when a move
     * reaches the final rank.
     */
    private void addManQuietMoves(Board board, Square from, Piece piece, List<EngineMove> out) {
        int forward = piece.color() == PieceColor.WHITE ? 1 : -1;
        for (int df : new int[]{-1, 1}) {
            Square to = from.step(df, forward);
            if (to != null && to.isDark() && board.get(to) == null) {
                out.add(EngineMove.quiet(from, to, isPromotionRank(to, piece.color())));
            }
        }
    }

    /**
     * Scans all four diagonals and adds each empty square until the first obstacle. This implements
     * a king's unrestricted diagonal range.
     */
    private void addKingQuietMoves(Board board, Square from, List<EngineMove> out) {
        for (int[] d : DIAGONALS) {
            Square cursor = from.step(d[0], d[1]);
            while (cursor != null && cursor.isDark()) {
                if (board.get(cursor) != null) {
                    break;
                }
                out.add(EngineMove.quiet(from, cursor, false));
                cursor = cursor.step(d[0], d[1]);
            }
        }
    }

    /**
     * Recursively extends a capture with the same piece until no further jump exists. Each branch
     * receives its own board, path, captured-square set and promotion flag. Reaching the final rank
     * immediately continues the search as a king.
     */
    private void exploreCaptures(Board board,
                                 Square current,
                                 Piece piece,
                                 List<Square> pathSoFar,
                                 List<Square> capturedSoFar,
                                 Set<Square> capturedSet,
                                 boolean promotedSoFar,
                                 List<EngineMove> out) {
        List<CaptureStep> steps = findCaptureSteps(board, current, piece, capturedSet);
        if (steps.isEmpty()) {
            return;
        }

        // Depth-first search explores each possible next landing in its own branch.
        for (CaptureStep step : steps) {
            Board nextBoard = board.copy();
            nextBoard.clear(current);
            nextBoard.clear(step.captured());

            // Promotion is immediate, so the rest of this chain follows king-capture rules.
            Piece moving = piece;
            boolean promoted = promotedSoFar;
            if (!moving.isKing() && isPromotionRank(step.landing(), moving.color())) {
                moving = moving.promote();
                promoted = true;
            }
            nextBoard.set(step.landing(), moving);

            List<Square> nextPath = new ArrayList<>(pathSoFar);
            nextPath.add(step.landing());

            List<Square> nextCaptured = new ArrayList<>(capturedSoFar);
            nextCaptured.add(step.captured());

            // The copied board removes the piece, but its square remains blocked until this chain
            // ends.
            Set<Square> nextSet = new HashSet<>(capturedSet);
            nextSet.add(step.captured());

            List<CaptureStep> further = findCaptureSteps(nextBoard, step.landing(), moving, nextSet);
            if (further.isEmpty()) {
                // Record only a complete chain: a player cannot stop while another capture is
                // available.
                out.add(new EngineMove(nextPath, nextCaptured, promoted));
            } else {
                exploreCaptures(nextBoard, step.landing(), moving, nextPath, nextCaptured, nextSet, promoted, out);
            }
        }
    }

    /**
     * Chooses the next-jump generator for the moving piece's type. Returns immediate continuations
     * rather than entire capture chains.
     */
    private List<CaptureStep> findCaptureSteps(Board board,
                                               Square from,
                                               Piece piece,
                                               Set<Square> alreadyCaptured) {
        return piece.isKing()
                ? findKingCaptureSteps(board, from, piece, alreadyCaptured)
                : findManCaptureSteps(board, from, piece, alreadyCaptured);
    }

    /**
     * Finds jumps over adjacent enemy pieces onto empty squares in all four directions. Men may
     * capture backward; pieces already captured in the chain remain blocked.
     */
    private List<CaptureStep> findManCaptureSteps(Board board,
                                                  Square from,
                                                  Piece piece,
                                                  Set<Square> alreadyCaptured) {
        List<CaptureStep> steps = new ArrayList<>();
        for (int[] d : DIAGONALS) {
            // A man jumps over one adjacent enemy and may capture backward as well as forward.
            Square enemySq = from.step(d[0], d[1]);
            Square land = enemySq == null ? null : enemySq.step(d[0], d[1]);
            if (enemySq == null || land == null || !land.isDark()) {
                continue;
            }
            Piece enemy = board.get(enemySq);
            if (enemy == null || enemy.color() == piece.color()) {
                continue;
            }
            if (alreadyCaptured.contains(enemySq) || board.get(land) != null || alreadyCaptured.contains(land)) {
                continue;
            }
            steps.add(new CaptureStep(enemySq, land));
        }
        return steps;
    }

    /**
     * Scans each diagonal for the first enemy and lists empty landing squares beyond it. A friendly
     * piece, second enemy or previously captured square stops the scan until the chain ends.
     */
    private List<CaptureStep> findKingCaptureSteps(Board board,
                                                   Square from,
                                                   Piece piece,
                                                   Set<Square> alreadyCaptured) {
        List<CaptureStep> steps = new ArrayList<>();
        for (int[] d : DIAGONALS) {
            Square cursor = from.step(d[0], d[1]);
            Square enemySq = null;
            while (cursor != null && cursor.isDark()) {
                if (alreadyCaptured.contains(cursor)) {
                    // A piece already captured in this chain cannot be crossed or captured again.
                    break;
                }
                Piece at = board.get(cursor);
                if (at == null) {
                    if (enemySq != null) {
                        // Any empty landing beyond the first enemy is possible until the next
                        // obstacle.
                        steps.add(new CaptureStep(enemySq, cursor));
                    }
                    cursor = cursor.step(d[0], d[1]);
                    continue;
                }
                if (enemySq != null) {
                    // One jump cannot cross a second piece.
                    break;
                }
                if (at.color() == piece.color()) {
                    break;
                }
                enemySq = cursor;
                cursor = cursor.step(d[0], d[1]);
            }
        }
        return steps;
    }

    /**
     * Moves the piece, removes all captured opponents and preserves promotion reached at any
     * intermediate landing. Called only for an EngineMove already confirmed as legal.
     */
    private void applyEngineMove(Board board, EngineMove move) {
        Square from = move.from();
        Piece piece = board.get(from);
        board.clear(from);
        for (Square cap : move.captured()) {
            board.clear(cap);
        }
        PieceColor color = piece.color();
        // Inspect every landing: a man may reach the final rank and leave it within the same chain.
        boolean becameKing = piece.isKing();
        for (int i = 1; i < move.path().size(); i++) {
            if (!becameKing && isPromotionRank(move.path().get(i), color)) {
                becameKing = true;
            }
        }
        if (becameKing) {
            piece = new Piece(color, PieceType.KING);
        }
        board.set(move.to(), piece);
    }

    /**
     * Selects a specific reason when no legal move matches the submitted path. Checks are ordered
     * to report source ownership, mandatory capture, geometry and occupied destination before a
     * general path failure.
     */
    private MoveResult diagnoseIllegal(Board board, List<Square> path, List<EngineMove> legal) {
        Square from = path.get(0);
        Piece piece = board.get(from);
        // Report specific source/ownership errors before a general invalid-path failure.
        if (piece == null) {
            // The selected starting square contains no piece.
            return MoveResult.fail(MoveError.EMPTY_SOURCE);
        }
        if (piece.color() != board.turn()) {
            // The selected piece belongs to the side that is not currently allowed to move.
            return MoveResult.fail(MoveError.NOT_YOUR_TURN);
        }
        if (from.equals(path.get(path.size() - 1))) {
            // This path already failed legal matching; a valid closed capture chain would have
            // matched earlier.
            return MoveResult.fail(MoveError.SAME_SQUARE);
        }
        // This heuristic selects an error message; exact legal-path matching has already decided
        // legality.
        boolean anyCaptureLegal = legal.stream().anyMatch(EngineMove::isCapture);
        boolean attemptedCapture = path.size() > 2 || looksLikeCaptureAttempt(path);
        if (anyCaptureLegal && !attemptedCapture) {
            // An available capture forbids substituting a quiet move with another piece.
            return MoveResult.fail(MoveError.CAPTURE_MANDATORY);
        }
        if (!isDiagonalPath(path)) {
            // At least one submitted path segment is not diagonal.
            return MoveResult.fail(MoveError.INVALID_DIAGONAL);
        }
        Square to = path.get(path.size() - 1);
        if (board.get(to) != null) {
            // The rejected path ends on an occupied square.
            return MoveResult.fail(MoveError.LANDING_OCCUPIED);
        }
        if (attemptedCapture) {
            // The request resembles a capture but matches no complete legal capture chain.
            return MoveResult.fail(MoveError.INVALID_CAPTURE);
        }
        // Other invalid quiet attempts are reported under the general PATH_BLOCKED code.
        return MoveResult.fail(MoveError.PATH_BLOCKED);
    }

    /**
     * Uses path length and travel distance to recognize a likely capture attempt for error
     * reporting. This heuristic does not decide legality; legalMoves does.
     */
    private boolean looksLikeCaptureAttempt(List<Square> path) {
        if (path.size() > 2) {
            return true;
        }
        Square a = path.get(0);
        Square b = path.get(1);
        return Math.abs(a.file() - b.file()) > 1 || Math.abs(a.rank() - b.rank()) > 1;
    }

    /**
     * Checks that every path segment is a nonzero diagonal: absolute file and rank changes must
     * match.
     */
    private boolean isDiagonalPath(List<Square> path) {
        for (int i = 1; i < path.size(); i++) {
            Square a = path.get(i - 1);
            Square b = path.get(i);
            int df = Math.abs(a.file() - b.file());
            int dr = Math.abs(a.rank() - b.rank());
            // A nonzero diagonal requires equal absolute file and rank differences.
            if (df == 0 || df != dr) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether a piece reached its final rank: rank eight for White or rank one for Black.
     */
    private boolean isPromotionRank(Square square, PieceColor color) {
        return color == PieceColor.WHITE ? square.rank() == 7 : square.rank() == 0;
    }

    /**
     * Visits all board squares and invokes the supplied operation only for pieces of the requested
     * color. Shared by the move generators.
     */
    private void forEachPiece(Board board, PieceColor side, PieceConsumer consumer) {
        for (int r = 0; r < Square.SIZE; r++) {
            for (int f = 0; f < Square.SIZE; f++) {
                Square sq = Square.of(f, r);
                Piece p = board.get(sq);
                if (p != null && p.color() == side) {
                    consumer.accept(sq, p);
                }
            }
        }
    }

    /**
     * Internal callback processing a piece and its square. Reuses board traversal across different
     * move generators.
     */
    @FunctionalInterface
    private interface PieceConsumer {
        /**
         * Processes one friendly piece together with its square. The caller's lambda supplies the
         * particular generation operation.
         */
        void accept(Square square, Piece piece);
    }

    /**
     * One immediate capture: the enemy square and landing square. Complete chains are assembled
     * from multiple steps.
     */
    private record CaptureStep(Square captured, Square landing) {
    }
}
