package com.checkers.model.enums;

/**
 * Structured WebSocket / domain event codes.
 * Backend never sends localized human-readable text — frontend maps
 * {@code EventType} + {@code data} to messages via i18n keys (Stage 4).
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
