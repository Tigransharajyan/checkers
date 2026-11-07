package com.checkers.engine;

import java.util.List;
import java.util.Optional;

/**
 * Immutable move attempt result containing a new board and EngineMove on success, or an error code
 * on failure. Rejected moves leave the original board unchanged.
 */
public record MoveResult(boolean success, Board board, EngineMove move, MoveError error) {

    /**
     * Creates a successful result containing the new board and applied legal move.
     */
    public static MoveResult ok(Board board, EngineMove move) {
        return new MoveResult(true, board, move, null);
    }

    /**
     * Creates a failed result containing the move-error code and no new position.
     */
    public static MoveResult fail(MoveError error) {
        return new MoveResult(false, null, null, error);
    }

    /**
     * Wraps the nullable error in Optional so callers explicitly handle its presence or absence.
     */
    public Optional<MoveError> errorOptional() {
        return Optional.ofNullable(error);
    }
}
