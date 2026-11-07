package com.checkers.assistant;

import com.checkers.config.GameProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Authoritative assistant content shared by AI routing and server-side answer rendering. Groq
 * may select topic identifiers, but only these curated local texts can reach the user.
 */
@Component
public class SiteHelpKnowledge {
    private final FaqKnowledgeBase faq;
    private final Map<String, Map<String, String>> answers = new LinkedHashMap<>();
    private final List<TopicMatch> directMatches = new ArrayList<>();
    private final List<TopicMatch> fallbackMatches = new ArrayList<>();
    private final String prompt;

    /**
     * Loads runtime instructions and supplementary help, reuses the existing FAQ and substitutes
     * actual configured game deadlines. Invalid or incomplete content fails startup explicitly.
     */
    public SiteHelpKnowledge(FaqKnowledgeBase faq, ObjectMapper mapper, GameProperties game) {
        this.faq = faq;
        for (String locale : List.of("en", "ru", "hy")) {
            for (FaqEntry entry : faq.entries(locale)) {
                answers.computeIfAbsent(entry.id(), ignored -> new LinkedHashMap<>())
                        .put(locale, entry.answer());
            }
        }
        try (var input = new ClassPathResource("assistant/site-help.json").getInputStream()) {
            Map<String, Map<String, String>> extra = mapper.readValue(input, new TypeReference<>() {});
            for (var entry : extra.entrySet()) {
                if (answers.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                    throw new IllegalStateException("Duplicate assistant topic: " + entry.getKey());
                }
                registerMatch(directMatches, entry.getKey(), entry.getValue().get("directMatch"));
                registerMatch(fallbackMatches, entry.getKey(), entry.getValue().get("fallbackMatch"));
            }
            for (Map<String, String> translations : answers.values()) {
                for (String locale : List.of("en", "ru", "hy")) {
                    String text = Objects.requireNonNull(translations.get(locale), "Missing help translation");
                    if (text.isBlank()) throw new IllegalStateException("Empty assistant help topic");
                    translations.put(locale, text
                            .replace("{reconnectSeconds}", String.valueOf(game.getReconnectTimeoutSeconds()))
                            .replace("{matchmakingSeconds}", String.valueOf(game.getMatchmakingTimeoutSeconds()))
                            .replace("{drawMinimumMoves}", String.valueOf(game.getDrawOfferMinimumMoves())));
                }
            }
            String rules = new ClassPathResource("assistant/system-prompt.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            List<Map<String, String>> catalogue = new ArrayList<>();
            answers.forEach((id, translations) -> catalogue.add(Map.of("id", id, "facts", translations.get("en"))));
            prompt = rules + "\n\nTRUSTED SITE TOPICS (JSON):\n" + mapper.writeValueAsString(catalogue);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load site assistant knowledge", error);
        }
    }

    /** Returns the immutable list of identifiers allowed by the provider's output schema. */
    public List<String> topicIds() {
        return List.copyOf(answers.keySet());
    }

    /** Returns trusted instructions and catalogue; credentials and account data are never included. */
    public String systemPrompt() {
        return prompt;
    }

    /** Normalizes an interface locale through the same rules used by the public FAQ. */
    public String locale(String locale) {
        return faq.normalizeLocale(locale);
    }

    /** Compiles curated multilingual intent patterns once; malformed rules fail startup. */
    private void registerMatch(List<TopicMatch> rules, String topic, String expression) {
        if (expression != null && !expression.isBlank()) {
            rules.add(new TopicMatch(topic, Pattern.compile(expression,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS)));
        }
    }

    /**
     * Answers explicit requests for known unsupported actions without depending on model quotas.
     * Patterns require the action and relevant object; deleting an account is not deleting a game.
     */
    public AssistantReply directAnswer(String locale, String message) {
        return match(locale, message, directMatches);
    }

    /** Returns a specific local answer when Groq is unavailable or unnecessarily asks to clarify. */
    public AssistantReply fallbackAnswer(String locale, String message) {
        AssistantReply direct = directAnswer(locale, message);
        return direct != null ? direct : match(locale, message, fallbackMatches);
    }

    /** Normalizes user spelling and returns only curated topics selected by trusted patterns. */
    private AssistantReply match(String locale, String message, List<TopicMatch> rules) {
        if (message == null || message.isBlank()) return null;
        String text = Normalizer.normalize(message, Normalizer.Form.NFKC);
        List<String> topics = rules.stream().filter(rule -> rule.expression().matcher(text).find())
                .map(TopicMatch::topic).distinct().limit(3).toList();
        return topics.isEmpty() ? null : answer(locale, topics);
    }

    /** Distinguishes a provider outage from an ambiguous question instead of blaming the user. */
    public AssistantReply unavailable(String locale) {
        String language = locale(locale);
        String text = switch (language) {
            case "ru" -> "Сейчас ИИ-помощник временно недоступен, а в локальной справке не нашёлся точный ответ. Попробуйте ещё раз немного позже. Это не означает, что ваш вопрос не относится к сайту.";
            case "hy" -> "Այժմ ԱԻ օգնականը ժամանակավորապես հասանելի չէ, իսկ տեղային տեղեկատուում ճշգրիտ պատասխան չի գտնվել։ Մի փոքր ուշ կրկին փորձեք։ Սա չի նշանակում, որ ձեր հարցը կայքին չի վերաբերում։";
            default -> "The AI assistant is temporarily unavailable and local help has no exact answer. Please try again shortly. This does not mean your question is unrelated to the site.";
        };
        return new AssistantReply(text, false, "/faq?lang=" + language);
    }

    /**
     * Renders up to three known topics in the interface language. Rejects unknown identifiers as
     * a whole rather than exposing any model-produced text or partial untrusted response.
     */
    public AssistantReply answer(String locale, List<String> topics) {
        String language = locale(locale);
        if (topics == null || topics.isEmpty() || topics.size() > 3
                || topics.stream().anyMatch(id -> !answers.containsKey(id))) return clarify(language);
        String text = String.join("\n\n", topics.stream().distinct()
                .map(id -> answers.get(id).get(language)).toList());
        return new AssistantReply(text, true, "/faq?lang=" + language);
    }

    /** Returns the fixed localized refusal used for requests unrelated to this website. */
    public AssistantReply refuse(String locale) {
        String language = locale(locale);
        String text = switch (language) {
            case "ru" -> "Я отвечаю только о Checkers Online: работе сайта, аккаунте, партиях, правилах шашек и задачах. На посторонние темы я не отвечаю. Что вы хотите узнать о сайте?";
            case "hy" -> "Ես պատասխանում եմ միայն Checkers Online կայքի, հաշվի, խաղերի, շաշկու կանոնների և խնդիրների մասին։ Այլ թեմաներով չեմ պատասխանում։ Ի՞նչ կցանկանաք իմանալ կայքի մասին։";
            default -> "I only answer questions about Checkers Online: this website, accounts, games, draughts rules and puzzles. I do not answer unrelated questions. What would you like to know about the site?";
        };
        return new AssistantReply(text, false, "/faq?lang=" + language);
    }

    /** Asks for a concrete site question when the supplied information does not determine an answer. */
    public AssistantReply clarify(String locale) {
        String language = locale(locale);
        String text = switch (language) {
            case "ru" -> "Что именно вы хотите сделать на сайте или на каком шаге возникла проблема? Например: войти в аккаунт, пригласить друга или сделать ход.";
            case "hy" -> "Ի՞նչ եք ցանկանում անել կայքում, կամ ո՞ր քայլում է առաջացել խնդիրը։ Օրինակ՝ մուտք գործել, ընկեր հրավիրել կամ քայլ կատարել։";
            default -> "What are you trying to do on the site, or at which step does the problem occur? For example: sign in, invite a friend, or make a move.";
        };
        return new AssistantReply(text, false, "/faq?lang=" + language);
    }

    /** One trusted local intent rule pointing to existing localized site content. */
    private record TopicMatch(String topic, Pattern expression) {}
}
