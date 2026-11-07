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
        // A known missing feature still has a precise answer, even with no remote AI available.
        AssistantReply direct = knowledge.directAnswer(language, message);
        if (direct != null) return direct;
        GroqClient.Route route = groq.route(language, message, recentQuestions);
        if (route == null) {
            AssistantReply fallback = fallback(language, message);
            return fallback != null ? fallback : knowledge.unavailable(language);
        }
        return switch (route.decision()) {
            case "answer" -> knowledge.answer(language, route.topics());
            case "refuse" -> knowledge.refuse(language);
            default -> {
                AssistantReply fallback = fallback(language, message);
                yield fallback != null ? fallback : knowledge.clarify(language);
            }
        };
    }

    /** Tries expanded local help before the original FAQ when AI routing cannot provide an answer. */
    private AssistantReply fallback(String locale, String message) {
        AssistantReply expanded = knowledge.fallbackAnswer(locale, message);
        if (expanded != null) return expanded;
        AssistantReply faq = local.reply(locale, message);
        return faq.matched() ? faq : null;
    }
}
