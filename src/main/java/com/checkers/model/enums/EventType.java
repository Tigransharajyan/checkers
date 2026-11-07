package com.checkers.model.enums;

/**
 * Real-time event types for moves, game lifecycle, draw offers, matchmaking and presence. The
 * client localizes event wording.
 */
public enum EventType {
    GAME_STARTED,
    MOVE_MADE,
    OPPONENT_DISCONNECTED,
    OPPONENT_RECONNECTED,
    TECHNICAL_DEFEAT,
    DRAW_OFFERED,
    DRAW_ACCEPTED,
    DRAW_DECLINED,
    GAME_RESIGNED,
    GAME_CANCELLED,
    GAME_FINISHED,
    MATCHMAKING_QUEUED,
    MATCHMAKING_MATCHED,
    MATCHMAKING_CANCELLED,
    MATCHMAKING_TIMEOUT,
    INVITE_ACCEPTED,
    ERROR
}
