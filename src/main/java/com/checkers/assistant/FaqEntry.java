package com.checkers.assistant;

import java.util.List;

/**
 * Immutable multilingual knowledge-base entry containing a question, answer and search keywords.
 */
public record FaqEntry(String id, String question, String answer, List<String> keywords) {
}
