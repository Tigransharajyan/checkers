package com.checkers.assistant;

/**
 * Immutable internal assistant result containing reply text, a reliable-match flag and a FAQ path.
 */
public record AssistantReply(String answer, boolean matched, String faqPath) {
}
