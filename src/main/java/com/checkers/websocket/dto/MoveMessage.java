package com.checkers.websocket.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Client move payload.
 * Prefer {@code path} for multi-captures (from … intermediate … to).
 * {@code fromSquare}/{@code toSquare} kept for simple two-square moves.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveMessage {

    private String fromSquare;
    private String toSquare;

    /** Full ply path including from and to. Takes precedence when size ≥ 2. */
    private List<String> path;

    public List<String> resolvedPath() {
        if (path != null && path.size() >= 2) {
            return path;
        }
        List<String> simple = new ArrayList<>(2);
        if (fromSquare != null) {
            simple.add(fromSquare);
        }
        if (toSquare != null) {
            simple.add(toSquare);
        }
        return simple;
    }

    @AssertTrue(message = "path or fromSquare+toSquare required")
    public boolean isPathPresent() {
        return resolvedPath().size() >= 2;
    }
}
