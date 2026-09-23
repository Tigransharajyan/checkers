package com.checkers.engine;

/**
 * Machine-readable move validation errors. Frontend maps codes to i18n (Stage 4).
 */
public enum MoveError {
    NOT_YOUR_TURN,
    GAME_OVER,
    INVALID_SQUARE,
    EMPTY_SOURCE,
    WRONG_PIECE,
    SAME_SQUARE,
    INVALID_DIAGONAL,
    PATH_BLOCKED,
    LANDING_OCCUPIED,
    CAPTURE_MANDATORY,
    INVALID_CAPTURE,
    INVALID_PATH,
    NO_LEGAL_MOVE
}
