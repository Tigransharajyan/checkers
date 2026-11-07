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
     * Accepts bounded prior user and assistant turns so follow-ups retain their meaning.
     * Implementations without conversational support keep their single-question behavior.
     */
    default AssistantReply reply(String locale, String message, List<AssistantTurn> conversation) {
        return reply(locale, message);
    }
}
