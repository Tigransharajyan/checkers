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
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Authoritative site facts for grounded AI generation and localized help during provider outages.
 * Catalogue wording is reference material, not a mandatory set of canned AI replies.
 */
@Component
public class SiteHelpKnowledge {
    /** Public support channels supplied by the site owner; never inferred from a model response. */
    private static final String OWNER_CONTACTS = "Telegram: https://t.me/shaatimi\nEmail: shatimi@proton.me";
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
                            .replace("{drawMinimumMoves}", String.valueOf(game.getDrawOfferMinimumMoves()))
                            .replace("{ownerContacts}", OWNER_CONTACTS));
                }
            }
            String rules = new ClassPathResource("assistant/system-prompt.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            List<Map<String, String>> catalogue = new ArrayList<>();
            answers.forEach((id, translations) -> {
                String facts = translations.getOrDefault("facts", translations.get("en"));
                catalogue.add(Map.of("id", id, "facts", facts.replace("\n\n" + OWNER_CONTACTS, "")));
            });
            prompt = "TRUSTED SITE TOPICS (JSON):\n" + mapper.writeValueAsString(catalogue) + "\n\n" + rules;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot load site assistant knowledge", error);
        }
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

    /** Returns a specific local answer when Groq is unavailable. */
    public AssistantReply fallbackAnswer(String locale, String message) {
        AssistantReply direct = directAnswer(locale, message);
        if (direct != null) return direct;
        AssistantReply expanded = match(locale, message, fallbackMatches);
        if (expanded != null) return expanded;
        // During an outage, an exact FAQ question is safe; a merely similar topic is not.
        String normalized = normalizeQuestion(message);
        return faq.entries(locale).stream()
                .filter(entry -> normalizeQuestion(entry.question()).equals(normalized))
                .findFirst().map(entry -> answer(locale, List.of(entry.id()))).orElse(null);
    }

    /** Ignores case and punctuation only; this is equality matching, not a fuzzy topic guess. */
    private String normalizeQuestion(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
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
            case "ru" -> "Сейчас не удалось загрузить ответ. Попробуйте ещё раз через минуту или свяжитесь с владельцем сайта.";
            case "hy" -> "Այժմ պատասխանը չհաջողվեց բեռնել։ Մեկ րոպեից կրկին փորձեք կամ կապվեք կայքի սեփականատիրոջ հետ։";
            default -> "The answer could not be loaded just now. Try again in a minute or contact the site owner.";
        };
        return new AssistantReply(text + "\n\n" + OWNER_CONTACTS, false, "/faq?lang=" + language);
    }

    /**
     * Renders up to three known topics in the interface language. Rejects unknown identifiers as
     * a whole so offline matching cannot accidentally return unrelated or partial content.
     */
    public AssistantReply answer(String locale, List<String> topics) {
        String language = locale(locale);
        if (topics == null || topics.isEmpty() || topics.size() > 3
                || topics.stream().anyMatch(id -> !answers.containsKey(id))) return clarify(language);
        List<String> sections = topics.stream().distinct().map(id -> answers.get(id).get(language)).toList();
        boolean contacts = sections.stream().anyMatch(section -> section.contains(OWNER_CONTACTS));
        // Several related topics may share an escalation path; show the public contacts only once.
        String text = String.join("\n\n", sections.stream()
                .map(section -> section.replace("\n\n" + OWNER_CONTACTS, "")).toList());
        if (contacts) text += "\n\n" + OWNER_CONTACTS;
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
