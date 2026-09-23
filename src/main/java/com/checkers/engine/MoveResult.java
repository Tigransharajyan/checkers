package com.checkers.engine;

import java.util.List;
import java.util.Optional;

public record MoveResult(boolean success, Board board, EngineMove move, MoveError error) {

    public static MoveResult ok(Board board, EngineMove move) {
        return new MoveResult(true, board, move, null);
    }

    public static MoveResult fail(MoveError error) {
        return new MoveResult(false, null, null, error);
    }

    public Optional<MoveError> errorOptional() {
        return Optional.ofNullable(error);
    }
}
