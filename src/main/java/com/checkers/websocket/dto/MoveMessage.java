package com.checkers.websocket.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Incoming full move path or compatible endpoint fields. Bean Validation checks required data; the
 * rules engine decides legality.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveMessage {

    private String fromSquare;
    private String toSquare;

    /**
     * Complete path with source and destination. Takes precedence over from/to when it has at least
     * two squares.
     */
    private List<String> path;

    /**
     * Prefers a full path containing at least two squares; otherwise collects the available
     * fromSquare and toSquare values. The separate isPathPresent check verifies that two
     * coordinates are present.
     */
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

    /**
     * Validates that the request contains either a complete path of at least two squares or both
     * endpoint fields.
     */
    @AssertTrue(message = "path or fromSquare+toSquare required")
    public boolean isPathPresent() {
        return resolvedPath().size() >= 2;
    }
}
