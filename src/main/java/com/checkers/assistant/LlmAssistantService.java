package com.checkers.assistant;

/**
 * Skeleton for a future paid LLM provider.
 *
 * <p>Enable with {@code app.assistant.provider=llm} and implement
 * {@link #reply(String, String)} using {@code app.assistant.llm.api-key}.
 * Uncomment {@code @Service} / {@code @ConditionalOnProperty} when ready —
 * leave {@link IntentAssistantService} as the default otherwise.
 */
// @Service
// @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
//         name = "app.assistant.provider", havingValue = "llm")
// @org.springframework.context.annotation.Primary
public class LlmAssistantService implements AssistantReplyService {

    // Inject: @Value("${app.assistant.llm.api-key}") String apiKey;

    @Override
    public AssistantReply reply(String locale, String message) {
        throw new UnsupportedOperationException(
                "Implement external LLM call here. Configure app.assistant.llm.api-key "
                        + "(env ASSISTANT_LLM_API_KEY). Prefer falling back to IntentAssistantService on failure.");
    }
}
