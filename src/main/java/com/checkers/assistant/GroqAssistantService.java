package com.checkers.assistant;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main site-help service. Groq generates grounded conversational replies; local FAQ answers are
 * used only when the provider is unavailable. The model's scope refusal remains authoritative.
 */
@Service
@Primary
public class GroqAssistantService implements AssistantReplyService {
    private final GroqClient groq;
    private final SiteHelpKnowledge knowledge;

    /** Receives the generative client and authoritative content with explicit offline help. */
    public GroqAssistantService(GroqClient groq, SiteHelpKnowledge knowledge) {
        this.groq = groq;
        this.knowledge = knowledge;
    }

    /** Handles a single question using the same restricted pipeline as contextual requests. */
    @Override
    public AssistantReply reply(String locale, String message) {
        return reply(locale, message, List.of());
    }

    /**
     * Generates the answer from trusted site facts and untrusted recent dialogue. Explicit refusals
     * never fall through to keyword matching; canned help is reserved for an unavailable provider.
     */
    @Override
    public AssistantReply reply(String locale, String message, List<AssistantTurn> conversation) {
        String language = knowledge.locale(locale);
        if (message == null || message.isBlank()) return knowledge.clarify(language);
        GroqClient.Completion completion = groq.complete(language, message, conversation);
        if (completion == null) {
            // A contextual follow-up cannot safely be answered by isolated keyword matching.
            AssistantReply fallback = conversation == null || conversation.isEmpty() ? fallback(language, message) : null;
            return fallback != null ? fallback : knowledge.unavailable(language);
        }
        if ("refuse".equals(completion.decision())) return knowledge.refuse(language);
        return new AssistantReply(completion.answer(), "answer".equals(completion.decision()), "/faq?lang=" + language);
    }

    /** Tries expanded local help before the original FAQ when AI generation is unavailable. */
    private AssistantReply fallback(String locale, String message) {
        return knowledge.fallbackAnswer(locale, message);
    }
}
