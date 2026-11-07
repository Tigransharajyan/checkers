package com.checkers.model.enums;

/**
 * Game lifecycle states: waiting, ongoing, finished or cancelled before play.
 */
public enum GameStatus {
    WAITING,
    IN_PROGRESS,
    FINISHED,
    ABORTED
}
