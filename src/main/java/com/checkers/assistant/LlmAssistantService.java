package com.checkers.assistant;

/**
 * Inactive extension point for a future external LLM provider. No external model is called by this
 * implementation.
 */
public class LlmAssistantService implements AssistantReplyService {

    // Future extension: inject an external provider client and its environment-supplied API key
    // here.

    /**
     * Throws UnsupportedOperationException because the external LLM integration has not been
     * implemented. The running application uses the local intent-based assistant instead.
     */
    @Override
    public AssistantReply reply(String locale, String message) {
        throw new UnsupportedOperationException(
                "Implement external LLM call here. Configure app.assistant.llm.api-key "
                        + "(env ASSISTANT_LLM_API_KEY). Prefer falling back to IntentAssistantService on failure.");
    }
}
