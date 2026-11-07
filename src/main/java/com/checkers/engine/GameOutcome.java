package com.checkers.engine;

/**
 * Engine outcome: ongoing play, White victory, Black victory or draw. The persisted game records
 * the specific finish reason separately.
 */
public enum GameOutcome {
    IN_PROGRESS,
    WHITE_WINS,
    BLACK_WINS,
    DRAW
}
