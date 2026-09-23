package com.checkers.engine;

import com.checkers.model.enums.PieceColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Russian draughts (шашки) rules on 8×8. Pure Java — no Spring dependencies.
 *
 * <ul>
 *   <li>Men move forward diagonally; capture in any diagonal direction</li>
 *   <li>Capture is mandatory, but maximal capture is NOT required</li>
 *   <li>Multi-jump chains in one ply</li>
 *   <li>Flying kings (дамки)</li>
 *   <li>Man promotes on last rank; mid-capture promotion continues as king</li>
 * </ul>
 */
public final class CheckersRulesEngine {

    private static final int[][] DIAGONALS = {
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Board createInitialBoard() {
        return Board.initial();
    }

    public List<EngineMove> legalMoves(Board board) {
        if (board.outcome() != GameOutcome.IN_PROGRESS) {
            return List.of();
        }
        List<EngineMove> captures = allCaptures(board, board.turn());
        if (!captures.isEmpty()) {
            return captures;
        }
        return allQuietMoves(board, board.turn());
    }

    public MoveResult applyMove(Board board, List<String> pathAlgebraic) {
        if (board.outcome() != GameOutcome.IN_PROGRESS) {
            return MoveResult.fail(MoveError.GAME_OVER);
        }
        if (pathAlgebraic == null || pathAlgebraic.size() < 2) {
            return MoveResult.fail(MoveError.INVALID_PATH);
        }

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

        Board next = board.copy();
        applyEngineMove(next, matched);
        next.switchTurn();
        refreshOutcome(next);
        return MoveResult.ok(next, matched);
    }

    public MoveResult applyMove(Board board, String from, String to) {
        return applyMove(board, List.of(from, to));
    }

    public GameOutcome evaluateOutcome(Board board) {
        Board copy = board.copy();
        refreshOutcome(copy);
        return copy.outcome();
    }

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
        if (legalMoves(board).isEmpty()) {
            board.setOutcome(board.turn() == PieceColor.WHITE
                    ? GameOutcome.BLACK_WINS
                    : GameOutcome.WHITE_WINS);
        }
    }

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

    private List<EngineMove> allCaptures(Board board, PieceColor side) {
        List<EngineMove> moves = new ArrayList<>();
        forEachPiece(board, side, (sq, piece) ->
                exploreCaptures(board, sq, piece, List.of(sq), List.of(), Set.of(), moves));
        return moves;
    }

    private void addManQuietMoves(Board board, Square from, Piece piece, List<EngineMove> out) {
        int forward = piece.color() == PieceColor.WHITE ? 1 : -1;
        for (int df : new int[]{-1, 1}) {
            Square to = from.step(df, forward);
            if (to != null && to.isDark() && board.get(to) == null) {
                out.add(EngineMove.quiet(from, to, isPromotionRank(to, piece.color())));
            }
        }
    }

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

    private void exploreCaptures(Board board,
                                 Square current,
                                 Piece piece,
                                 List<Square> pathSoFar,
                                 List<Square> capturedSoFar,
                                 Set<Square> capturedSet,
                                 List<EngineMove> out) {
        List<CaptureStep> steps = findCaptureSteps(board, current, piece, capturedSet);
        if (steps.isEmpty()) {
            return;
        }

        for (CaptureStep step : steps) {
            Board nextBoard = board.copy();
            nextBoard.clear(current);
            nextBoard.clear(step.captured());

            Piece moving = piece;
            boolean promoted = false;
            if (!moving.isKing() && isPromotionRank(step.landing(), moving.color())) {
                moving = moving.promote();
                promoted = true;
            }
            nextBoard.set(step.landing(), moving);

            List<Square> nextPath = new ArrayList<>(pathSoFar);
            nextPath.add(step.landing());

            List<Square> nextCaptured = new ArrayList<>(capturedSoFar);
            nextCaptured.add(step.captured());

            Set<Square> nextSet = new HashSet<>(capturedSet);
            nextSet.add(step.captured());

            List<CaptureStep> further = findCaptureSteps(nextBoard, step.landing(), moving, nextSet);
            if (further.isEmpty()) {
                out.add(new EngineMove(nextPath, nextCaptured, promoted));
            } else {
                exploreCaptures(nextBoard, step.landing(), moving, nextPath, nextCaptured, nextSet, out);
            }
        }
    }

    private List<CaptureStep> findCaptureSteps(Board board,
                                               Square from,
                                               Piece piece,
                                               Set<Square> alreadyCaptured) {
        return piece.isKing()
                ? findKingCaptureSteps(board, from, piece, alreadyCaptured)
                : findManCaptureSteps(board, from, piece, alreadyCaptured);
    }

    private List<CaptureStep> findManCaptureSteps(Board board,
                                                  Square from,
                                                  Piece piece,
                                                  Set<Square> alreadyCaptured) {
        List<CaptureStep> steps = new ArrayList<>();
        for (int[] d : DIAGONALS) {
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
                    // In Russian checkers, captured pieces remain on the board as obstacles until the end of the move
                    break;
                }
                Piece at = board.get(cursor);
                if (at == null) {
                    if (enemySq != null) {
                        steps.add(new CaptureStep(enemySq, cursor));
                    }
                    cursor = cursor.step(d[0], d[1]);
                    continue;
                }
                if (enemySq != null) {
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

    private void applyEngineMove(Board board, EngineMove move) {
        Square from = move.from();
        Piece piece = board.get(from);
        board.clear(from);
        for (Square cap : move.captured()) {
            board.clear(cap);
        }
        PieceColor color = piece.color();
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

    private MoveResult diagnoseIllegal(Board board, List<Square> path, List<EngineMove> legal) {
        Square from = path.get(0);
        Piece piece = board.get(from);
        if (piece == null) {
            return MoveResult.fail(MoveError.EMPTY_SOURCE);
        }
        if (piece.color() != board.turn()) {
            return MoveResult.fail(MoveError.NOT_YOUR_TURN);
        }
        if (from.equals(path.get(path.size() - 1))) {
            return MoveResult.fail(MoveError.SAME_SQUARE);
        }
        boolean anyCaptureLegal = legal.stream().anyMatch(EngineMove::isCapture);
        boolean attemptedCapture = path.size() > 2 || looksLikeCaptureAttempt(path);
        if (anyCaptureLegal && !attemptedCapture) {
            return MoveResult.fail(MoveError.CAPTURE_MANDATORY);
        }
        if (!isDiagonalPath(path)) {
            return MoveResult.fail(MoveError.INVALID_DIAGONAL);
        }
        Square to = path.get(path.size() - 1);
        if (board.get(to) != null) {
            return MoveResult.fail(MoveError.LANDING_OCCUPIED);
        }
        if (attemptedCapture) {
            return MoveResult.fail(MoveError.INVALID_CAPTURE);
        }
        return MoveResult.fail(MoveError.PATH_BLOCKED);
    }

    private boolean looksLikeCaptureAttempt(List<Square> path) {
        if (path.size() > 2) {
            return true;
        }
        Square a = path.get(0);
        Square b = path.get(1);
        return Math.abs(a.file() - b.file()) > 1 || Math.abs(a.rank() - b.rank()) > 1;
    }

    private boolean isDiagonalPath(List<Square> path) {
        for (int i = 1; i < path.size(); i++) {
            Square a = path.get(i - 1);
            Square b = path.get(i);
            int df = Math.abs(a.file() - b.file());
            int dr = Math.abs(a.rank() - b.rank());
            if (df == 0 || df != dr) {
                return false;
            }
        }
        return true;
    }

    private boolean isPromotionRank(Square square, PieceColor color) {
        return color == PieceColor.WHITE ? square.rank() == 7 : square.rank() == 0;
    }

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

    @FunctionalInterface
    private interface PieceConsumer {
        void accept(Square square, Piece piece);
    }

    private record CaptureStep(Square captured, Square landing) {
    }
}
