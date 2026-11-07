package com.checkers.assistant;

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
}
