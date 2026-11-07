package com.checkers.assistant;

import java.util.List;

/**
 * Shared assistant contract. Controllers depend on this interface while Spring selects the concrete
 * implementation.
 */
public interface AssistantReplyService {

    /**
     * Answers the user's question in the requested language. Returns reply text, a match flag and a
     * link to the FAQ section.
     */
    AssistantReply reply(String locale, String message);

    /**
     * Accepts a bounded list of earlier user questions for short follow-ups. Implementations
     * without conversational routing retain their existing single-question behavior.
     */
    default AssistantReply reply(String locale, String message, List<String> recentQuestions) {
        return reply(locale, message);
    }
}
