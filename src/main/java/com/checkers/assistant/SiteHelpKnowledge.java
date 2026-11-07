package com.checkers.assistant;

import com.checkers.config.GameProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Authoritative assistant content shared by AI routing and server-side answer rendering. Groq
 * may select topic identifiers, but only these curated local texts can reach the user.
 */
@Component
public class SiteHelpKnowledge {
    private final FaqKnowledgeBase faq;
    private final Map<String, Map<String, String>> answers = new LinkedHashMap<>();
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
            case "ru" -> "Уточните, пожалуйста, вопрос о Checkers Online: на какой вы странице, что хотите сделать и что происходит? Я помогу с игрой, приглашениями, аккаунтом, ничьими, премувами или задачами, но не буду придумывать неизвестные сведения.";
            case "hy" -> "Խնդրում եմ հստակեցնել Checkers Online-ի մասին հարցը․ ո՞ր էջում եք, ի՞նչ եք ցանկանում անել և ի՞նչ է տեղի ունենում։ Կօգնեմ խաղի, հրավերների, հաշվի, ոչ-ոքիի, նախնական քայլերի կամ խնդիրների հարցերով՝ առանց անհայտ տեղեկություն հորինելու։";
            default -> "Please clarify your Checkers Online question: which page are you on, what are you trying to do, and what happens? I can help with games, invitations, accounts, draws, premoves or puzzles, but will not invent missing facts.";
        };
        return new AssistantReply(text, false, "/faq?lang=" + language);
    }
}
