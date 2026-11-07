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
        factory.setReadTimeout(Duration.ofSeconds(4));
        return RestClient.builder().baseUrl("https://api.groq.com/openai/v1")
                .requestFactory(factory).defaultHeader(HttpHeaders.USER_AGENT, "CheckersOnline/1.0").build();
    }

    /**
     * Requests a topic decision, trying each available key at most once within a twelve-second
     * operation budget. Returns null for unavailable service so callers can use trusted local help.
     */
    public Route route(String locale, String question, List<String> recentQuestions) {
        if (keys.isEmpty() || !inFlight.tryAcquire()) return null;
        try {
            if (!reserveRequest()) return null;
            Map<String, Object> payload = payload(locale, question, recentQuestions);
            long deadline = clock.millis() + 12_000;
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
                    Route route = parse(body);
                    if (route != null) {
                        synchronized (this) { preferredKey = index; }
                        return route;
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
        } finally {
            inFlight.release();
        }
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

    /** Builds trusted system instructions and an untrusted user-data payload with bounded history. */
    private Map<String, Object> payload(String locale, String question, List<String> recentQuestions) {
        List<String> history = recentQuestions == null ? List.of() : recentQuestions.stream()
                .filter(text -> text != null && !text.isBlank()).limit(4)
                .map(text -> text.substring(0, Math.min(500, text.length()))).toList();
        try {
            Map<String, Object> schema = Map.of(
                    "type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "decision", Map.of("type", "string", "enum", List.of("answer", "refuse", "clarify")),
                            "topics", Map.of("type", "array", "items", Map.of("type", "string", "enum", knowledge.topicIds()))),
                    "required", List.of("decision", "topics"));
            Map<String, Object> input = Map.of("language", locale,
                    "latest_question", question, "previous_questions", history);
            return Map.of("model", properties.getModel(), "temperature", 0.1,
                    "max_completion_tokens", 768,
                    "messages", List.of(Map.of("role", "system", "content", knowledge.systemPrompt()),
                            Map.of("role", "user", "content", mapper.writeValueAsString(input))),
                    "response_format", Map.of("type", "json_schema", "json_schema", Map.of(
                            "name", "site_help_route", "strict", true, "schema", schema)));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot encode assistant request");
        }
    }

    /**
     * Validates the full provider envelope and decision independently of constrained decoding.
     * No arbitrary answer field, unknown topic, truncated completion or extra property is accepted.
     */
    private Route parse(String body) {
        if (body == null || body.length() > 65_536) return null;
        try {
            JsonNode choice = mapper.readTree(body).path("choices").path(0);
            if (!"stop".equals(choice.path("finish_reason").asText())) return null;
            String content = choice.path("message").path("content").asText("");
            if (content.length() > 4096) return null;
            JsonNode result = mapper.readTree(content);
            if (result == null || !result.isObject() || result.size() != 2
                    || !result.path("decision").isTextual() || !result.path("topics").isArray()) return null;
            String decision = result.path("decision").asText();
            if (!Set.of("answer", "refuse", "clarify").contains(decision)) return null;
            JsonNode topics = result.path("topics");
            if (topics.size() > 3 || (decision.equals("answer") ? topics.isEmpty() : !topics.isEmpty())) return null;
            List<String> ids = new java.util.ArrayList<>();
            for (JsonNode topic : topics) {
                if (!topic.isTextual() || !knowledge.topicIds().contains(topic.asText())) return null;
                ids.add(topic.asText());
            }
            return new Route(decision, List.copyOf(ids));
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

    /** Validated topic-only provider decision. It contains neither generated prose nor credentials. */
    public record Route(String decision, List<String> topics) {}
}
