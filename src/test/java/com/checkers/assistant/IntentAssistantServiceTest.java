package com.checkers.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit checks for IntentAssistantService. Each test specifies a behavior or regression boundary;
 * helper methods build the required test data.
 */
class IntentAssistantServiceTest {

    private IntentAssistantService service;

    /**
     * Creates the local assistant with its FAQ knowledge base before each independent search test.
     */
    @BeforeEach
    void setUp() {
        service = new IntentAssistantService(new FaqKnowledgeBase());
    }

    /**
     * Checks that an English question about starting a game retrieves the appropriate
     * knowledge-base answer.
     */
    @Test
    void englishMatchesStartGame() {
        AssistantReply reply = service.reply("en", "How do I start a game?");
        assertTrue(reply.matched());
        assertTrue(reply.answer().toLowerCase().contains("lobby"));
    }

    /**
     * Checks retrieval of a king-related answer from the Russian FAQ.
     */
    @Test
    void russianMatchesDamka() {
        AssistantReply reply = service.reply("ru", "что такое дамка");
        assertTrue(reply.matched());
        assertTrue(reply.answer().toLowerCase().contains("дамк"));
    }

    /**
     * Checks the Armenian keyword-search branch and its localized answer.
     */
    @Test
    void armenianUsesKeywordPath() {
        AssistantReply reply = service.reply("hy", "ինչպես սկսել խաղը");
        assertTrue(reply.matched());
    }

    /**
     * Checks the fallback message and FAQ link when no confident match exists.
     */
    @Test
    void unknownFallsBackWithFaqLink() {
        AssistantReply reply = service.reply("en", "xyzzy unrelated quantum bananas");
        assertFalse(reply.matched());
        assertTrue(reply.faqPath().contains("/faq"));
    }

    /** Generic question words alone cannot select an unrelated FAQ answer during an outage. */
    @Test
    void doesNotMatchOnlyBecauseAQuestionContainsHowOrWhere() {
        assertFalse(service.reply("ru", "как сделать квантовый телепорт на сайте").matched());
        assertFalse(service.reply("en", "How do I calculate unrelated quantum bananas on this site?").matched());
    }
}
