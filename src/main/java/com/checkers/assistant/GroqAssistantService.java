package com.checkers.assistant;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main site-help service. Groq interprets questions, while the server renders only approved site
 * answers. Missing keys, quota limits or network failures preserve the local FAQ fallback.
 */
@Service
@Primary
public class GroqAssistantService implements AssistantReplyService {
    private final GroqClient groq;
    private final SiteHelpKnowledge knowledge;
    private final IntentAssistantService local;

    /** Receives the remote router, authoritative content and existing offline FAQ matcher. */
    public GroqAssistantService(GroqClient groq, SiteHelpKnowledge knowledge, IntentAssistantService local) {
        this.groq = groq;
        this.knowledge = knowledge;
        this.local = local;
    }

    /** Handles a single question using the same restricted pipeline as contextual requests. */
    @Override
    public AssistantReply reply(String locale, String message) {
        return reply(locale, message, List.of());
    }

    /**
     * Uses only validated topic identifiers from Groq. The provider's arbitrary text never becomes
     * a public answer, even if a prompt injection or malformed completion bypasses its instructions.
     */
    @Override
    public AssistantReply reply(String locale, String message, List<String> recentQuestions) {
        String language = knowledge.locale(locale);
        if (message == null || message.isBlank()) return knowledge.clarify(language);
        GroqClient.Route route = groq.route(language, message, recentQuestions);
        if (route == null) {
            AssistantReply fallback = local.reply(language, message);
            return fallback.matched() ? fallback : knowledge.clarify(language);
        }
        return switch (route.decision()) {
            case "answer" -> knowledge.answer(language, route.topics());
            case "refuse" -> knowledge.refuse(language);
            default -> knowledge.clarify(language);
        };
    }
}
