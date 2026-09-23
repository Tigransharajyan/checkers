package com.checkers.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentAssistantServiceTest {

    private IntentAssistantService service;

    @BeforeEach
    void setUp() {
        service = new IntentAssistantService(new FaqKnowledgeBase());
    }

    @Test
    void englishMatchesStartGame() {
        AssistantReply reply = service.reply("en", "How do I start a game?");
        assertTrue(reply.matched());
        assertTrue(reply.answer().toLowerCase().contains("lobby"));
    }

    @Test
    void russianMatchesDamka() {
        AssistantReply reply = service.reply("ru", "что такое дамка");
        assertTrue(reply.matched());
        assertTrue(reply.answer().toLowerCase().contains("дамк"));
    }

    @Test
    void armenianUsesKeywordPath() {
        AssistantReply reply = service.reply("hy", "ինչպես սկսել խաղը");
        assertTrue(reply.matched());
    }

    @Test
    void unknownFallsBackWithFaqLink() {
        AssistantReply reply = service.reply("en", "xyzzy unrelated quantum bananas");
        assertFalse(reply.matched());
        assertTrue(reply.faqPath().contains("/faq"));
    }
}
