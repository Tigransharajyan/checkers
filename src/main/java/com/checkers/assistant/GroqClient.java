package com.checkers.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * Server-only Groq transport with sticky key failover, cooldowns and a bounded request budget.
 * Keys, request text and provider error bodies are never logged or returned to the browser.
 */
@Component
@Slf4j
public class GroqClient {
    private final GroqProperties properties;
    private final ObjectMapper mapper;
    private final SiteHelpKnowledge knowledge;
    private final RestClient http;
    private final Clock clock;
    private final List<String> keys;
    private final long[] unavailableUntil;
    private final Semaphore inFlight = new Semaphore(2);
    private int preferredKey;
    private long windowStart;
    private int windowRequests;

    /** Creates production HTTPS transport with fixed Groq origin and short connection/read deadlines. */
    @Autowired
    public GroqClient(GroqProperties properties, ObjectMapper mapper, SiteHelpKnowledge knowledge) {
        this(properties, mapper, knowledge, createHttpClient(), Clock.systemUTC());
    }

    /** Accepts a controllable clock and mockable transport for deterministic failover tests. */
    GroqClient(GroqProperties properties, ObjectMapper mapper, SiteHelpKnowledge knowledge,
               RestClient http, Clock clock) {
        this.properties = properties;
        this.mapper = mapper;
        this.knowledge = knowledge;
        this.http = http;
        this.clock = clock;
        this.keys = properties.getApiKeys().stream().filter(key -> key != null && !key.isBlank())
                .map(String::trim).distinct().limit(10).toList();
        this.unavailableUntil = new long[keys.size()];
    }

    /** Builds the only allowed remote destination; redirects cannot forward credentials elsewhere. */
    private static RestClient createHttpClient() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(6));
        return RestClient.builder().baseUrl("https://api.groq.com/openai/v1")
                .requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, "CheckersOnline/1.0").build();
    }

    /**
     * Generates a contextual reply, trying each available key at most once within an eighteen-second
     * operation budget. Returns null for unavailable service so callers can use trusted local help.
     */
    public Completion complete(String locale, String question, List<AssistantTurn> conversation) {
        if (keys.isEmpty() || !inFlight.tryAcquire()) return null;
        try {
            if (!reserveRequest()) return null;
            Map<String, Object> payload = payload(locale, question, conversation);
            long deadline = clock.millis() + 18_000;
            Completion draft = request(payload, deadline);
            if (draft == null || "refuse".equals(draft.decision())) return draft;
            return request(reviewPayload(payload, draft), deadline);
        } finally {
            inFlight.release();
        }
    }

    /**
     * Asks the model to edit its draft against the same facts and original conversation. The review
     * is generic: it contains no special-case questions or canned answers. Only the reviewed text
     * is returned; an unavailable review never silently publishes an unchecked draft.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> reviewPayload(Map<String, Object> original, Completion draft) {
        Map<String, Object> reviewed = new java.util.LinkedHashMap<>(original);
        List<Map<String, String>> messages = new java.util.ArrayList<>(
                (List<Map<String, String>>) original.get("messages"));
        String question = messages.get(messages.size() - 1).get("content");
        messages.add(Map.of("role", "assistant", "content", draft.answer()));
        messages.add(Map.of("role", "user", "content", """
                Fact-check and edit the preceding draft for the latest user question quoted below.
                First check EVERY concrete claim against TRUSTED SITE TOPICS. In particular, every
                number, amount, frequency, time estimate, duration and comparison needs explicit
                support there. Remove any unsupported detail entirely, including approximate or
                typical quantities stated in words. A plausible estimate is not a supported fact.
                A request to a person does not establish approval, completion, timing or a promise.
                Then answer the quoted question directly in its conversation context and language.
                Use qualitative conditional explanations and a useful next step for uncertainty,
                rather than self-focused disclaimers. Do not repeat contacts already given unless
                requested again, or invent a support page. Keep the original site-only scope.
                Return only the corrected response in the required schema.
                """ + "\nLatest user question (untrusted JSON string): " + mapper.valueToTree(question)));
        reviewed.put("messages", messages);
        return reviewed;
    }

    /** Sends one generation or review request with shared key cooldowns and operation deadline. */
    private Completion request(Map<String, Object> payload, long deadline) {
        int first;
        synchronized (this) { first = preferredKey; }
        for (int offset = 0; offset < keys.size() && clock.millis() < deadline; offset++) {
            int index = (first + offset) % keys.size();
            synchronized (this) {
                if (unavailableUntil[index] > clock.millis()) continue;
            }
            try {
                String body = http.post().uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + keys.get(index))
                        .contentType(MediaType.APPLICATION_JSON).body(payload)
                        .retrieve().body(String.class);
                Completion completion = parse(body);
                if (completion != null) {
                    synchronized (this) { preferredKey = index; }
                    return completion;
                }
                // A malformed model result does not justify spending the other keys' quotas.
                return null;
            } catch (RestClientResponseException error) {
                int status = error.getStatusCode().value();
                if (!(status == 401 || status == 403 || status == 429 || status >= 500)) return null;
                long pause = status == 401 ? Long.MAX_VALUE : clock.millis() + cooldown(error);
                synchronized (this) { unavailableUntil[index] = pause; }
                log.debug("Groq key slot {} unavailable (HTTP {}); trying an available slot", index + 1, status);
            } catch (ResourceAccessException error) {
                synchronized (this) { unavailableUntil[index] = clock.millis() + 10_000; }
                log.debug("Groq transport unavailable for key slot {}", index + 1);
            } catch (RuntimeException error) {
                // Never propagate provider bodies, authorization headers or decoding failures.
                return null;
            }
        }
        return null;
    }

    /**
     * Limits model operations per application instance. This also bounds anonymous callers; it is
     * separate from Groq's organization-wide limits, which remain authoritative.
     */
    private synchronized boolean reserveRequest() {
        long now = clock.millis();
        if (now - windowStart >= 60_000) { windowStart = now; windowRequests = 0; }
        if (windowRequests >= Math.max(1, Math.min(60, properties.getRequestsPerMinute()))) return false;
        windowRequests++;
        return true;
    }

    /**
     * Keeps instructions separate from untrusted dialogue. Both sides of recent exchanges are
     * sent with their actual dialogue roles, allowing reference resolution while rejecting any
     * client-supplied privileged role. Only this server constructs the system message.
     */
    private Map<String, Object> payload(String locale, String question, List<AssistantTurn> conversation) {
        List<AssistantTurn> history = conversation == null ? List.of() : conversation.stream()
                .filter(turn -> turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role()))
                        && turn.content() != null && !turn.content().isBlank())
                .toList();
        history = history.subList(Math.max(0, history.size() - 8), history.size()).stream()
                .map(turn -> new AssistantTurn(turn.role(),
                        turn.content().substring(0, Math.min(2000, turn.content().length())))).toList();
        Map<String, Object> schema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "decision", Map.of("type", "string", "enum", List.of("answer", "refuse", "clarify")),
                        "answer", Map.of("type", "string")),
                "required", List.of("decision", "answer"));
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", knowledge.systemPrompt()
                + "\nReply language: " + knowledge.locale(locale)));
        for (AssistantTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", question));
        return Map.of("model", properties.getModel(), "temperature", 0.2,
                "reasoning_effort", "high", "max_completion_tokens", 1536,
                "messages", messages,
                "response_format", Map.of("type", "json_schema", "json_schema", Map.of(
                        "name", "site_help_reply", "strict", true, "schema", schema)));
    }

    /** Rejects incomplete envelopes, unexpected fields, invalid decisions and excessive reply text. */
    private Completion parse(String body) {
        if (body == null || body.length() > 65_536) return null;
        try {
            JsonNode choice = mapper.readTree(body).path("choices").path(0);
            if (!"stop".equals(choice.path("finish_reason").asText())) return null;
            String content = choice.path("message").path("content").asText("");
            if (content.length() > 16_384) return null;
            JsonNode result = mapper.readTree(content);
            if (result == null || !result.isObject() || result.size() != 2
                    || !result.path("decision").isTextual() || !result.path("answer").isTextual()) return null;
            String decision = result.path("decision").asText();
            String answer = result.path("answer").asText().strip();
            if (!Set.of("answer", "refuse", "clarify").contains(decision) || answer.length() > 2000
                    || (decision.equals("refuse") ? !answer.isEmpty() : answer.isEmpty())) return null;
            return new Completion(decision, answer);
        } catch (Exception error) {
            return null;
        }
    }

    /** Honors numeric or HTTP-date Retry-After values and applies conservative bounded cooldowns. */
    private long cooldown(RestClientResponseException error) {
        if (error.getStatusCode().value() == 403) return 300_000;
        if (error.getStatusCode().value() != 429) return 10_000;
        String retry = error.getResponseHeaders() == null ? null
                : error.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        long seconds = 60;
        if (retry != null) {
            try { seconds = Long.parseLong(retry); }
            catch (NumberFormatException ignored) {
                try { seconds = Duration.between(clock.instant(), ZonedDateTime.parse(retry,
                        DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds(); }
                catch (RuntimeException invalidDate) { seconds = 60; }
            }
        }
        return Math.max(1, Math.min(3600, seconds)) * 1000;
    }

    /** Validated model-generated reply and scope decision; no provider metadata reaches the UI. */
    public record Completion(String decision, String answer) {}
}
