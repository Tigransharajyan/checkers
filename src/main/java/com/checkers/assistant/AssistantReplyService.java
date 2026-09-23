package com.checkers.assistant;

/**
 * Extension point for the site help assistant.
 *
 * <p>Default bean: {@link IntentAssistantService} (offline FAQ matching).
 * To plug in a paid LLM later:
 * <ol>
 *   <li>Implement this interface (e.g. {@code LlmAssistantService})</li>
 *   <li>Mark it {@code @Primary} or enable via
 *       {@code app.assistant.provider=llm}</li>
 *   <li>Put the API key in {@code app.assistant.llm.api-key}
 *       (or env {@code ASSISTANT_LLM_API_KEY}) — never commit the key</li>
 * </ol>
 * The REST controller depends only on this interface, so no other code changes
 * are required when swapping providers.
 */
public interface AssistantReplyService {

    AssistantReply reply(String locale, String message);
}
