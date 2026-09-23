package com.checkers.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A complete legal ply: quiet move or a full multi-capture path.
 * {@code path} always includes from and to (size ≥ 2).
 */
public final class EngineMove {

    private final List<Square> path;
    private final List<Square> captured;
    private final boolean promotion;

    public EngineMove(List<Square> path, List<Square> captured, boolean promotion) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("path must contain at least from and to");
        }
        this.path = List.copyOf(path);
        this.captured = captured == null ? List.of() : List.copyOf(captured);
        this.promotion = promotion;
    }

    public Square from() {
        return path.get(0);
    }

    public Square to() {
        return path.get(path.size() - 1);
    }

    public List<Square> path() {
        return path;
    }

    public List<Square> captured() {
        return captured;
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }

    public boolean promotion() {
        return promotion;
    }

    public List<String> pathAlgebraic() {
        List<String> result = new ArrayList<>(path.size());
        for (Square sq : path) {
            result.add(sq.algebraic());
        }
        return result;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return String.join("-", pathAlgebraic());
    }

    public static EngineMove quiet(Square from, Square to, boolean promotion) {
        return new EngineMove(List.of(from, to), Collections.emptyList(), promotion);
    }
}
