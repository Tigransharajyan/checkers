package com.checkers.model.enums;

/**
 * Bot difficulty levels. Stored as STRING on {@code games.bot_difficulty}.
 * Not a separate DB table — reference values live in this enum.
 */
public enum BotDifficulty {
    EASY,
    MEDIUM,
    HARD
}
