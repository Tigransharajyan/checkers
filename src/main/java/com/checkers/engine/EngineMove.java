package com.checkers.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable legal move containing its complete path, captured squares and promotion flag.
 * Intermediate landings support exact validation, animation and a sound for each capture.
 */
public final class EngineMove {

    private final List<Square> path;
    private final List<Square> captured;
    private final boolean promotion;

    /**
     * Creates an immutable complete move with at least two path squares, captured squares and a
     * promotion flag. Copies lists to prevent later modification through caller-owned collections.
     */
    public EngineMove(List<Square> path, List<Square> captured, boolean promotion) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("path must contain at least from and to");
        }
        this.path = List.copyOf(path);
        this.captured = captured == null ? List.of() : List.copyOf(captured);
        this.promotion = promotion;
    }

    /**
     * Returns the first square of the full move path.
     */
    public Square from() {
        return path.get(0);
    }

    /**
     * Returns the final destination after every intermediate capture landing.
     */
    public Square to() {
        return path.get(path.size() - 1);
    }

    /**
     * Returns the immutable square sequence, including the source and all landings.
     */
    public List<Square> path() {
        return path;
    }

    /**
     * Returns the immutable list of squares occupied by captured pieces.
     */
    public List<Square> captured() {
        return captured;
    }

    /**
     * Reports whether the complete move captures at least one piece.
     */
    public boolean isCapture() {
        return !captured.isEmpty();
    }

    /**
     * Returns the recorded flag indicating promotion from man to king during this move.
     */
    public boolean promotion() {
        return promotion;
    }

    /**
     * Converts internal squares to coordinates such as a3 and b4 for JSON, the client and move
     * persistence.
     */
    public List<String> pathAlgebraic() {
        List<String> result = new ArrayList<>(path.size());
        for (Square sq : path) {
            result.add(sq.algebraic());
        }
        return result;
    }

    /**
     * Joins captured-square coordinates with commas for the moves table. Returns null when the move
     * captures nothing.
     */
    public String capturedCsv() {
        if (captured.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < captured.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(captured.get(i).algebraic());
        }
        return sb.toString();
    }

    /**
     * Compares moves by their full paths, including intermediate landings. Distinguishes capture
     * chains with identical endpoints but different routes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EngineMove that)) {
            return false;
        }
        return Objects.equals(path, that.path);
    }

    /**
     * Hashes the path consistently with equals for use in hash-based collections.
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    /**
     * Formats the path with hyphens for diagnostics and deterministic ordering of candidates.
     */
    @Override
    public String toString() {
        return String.join("-", pathAlgebraic());
    }

    /**
     * Creates a non-capturing two-square move with its possible promotion flag.
     */
    public static EngineMove quiet(Square from, Square to, boolean promotion) {
        return new EngineMove(List.of(from, to), Collections.emptyList(), promotion);
    }
}
