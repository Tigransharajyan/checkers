package com.checkers.assistant;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Local site-help assistant that retrieves existing FAQ answers using TF-IDF/cosine similarity for
 * English and Russian and keyword matching for Armenian. Does not generate text or call an external
 * AI provider.
 */
@Service
public class IntentAssistantService implements AssistantReplyService {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final double MIN_SCORE = 0.12;
    // Generic question words must not make an unknown request resemble "How do I start?".
    private static final Set<String> STOP_WORDS = Set.of(
            "how", "what", "where", "when", "why", "can", "could", "would", "do", "does",
            "the", "this", "that", "is", "are", "to", "of", "on", "in", "it", "my", "me",
            "you", "your", "please", "want", "site", "website",
            "как", "что", "где", "когда", "почему", "можно", "могу", "хочу", "мой", "моя",
            "мои", "мою", "свой", "свою", "мне", "меня", "это", "этот", "на", "не", "ли",
            "сайте", "сайт", "пожалуйста", "такое", "для", "или", "чтобы", "сделать", "нужно",
            "ինչպես", "ինչ", "որտեղ", "երբ", "ինչու", "իմ", "ես", "այս", "կայքում");

    private final FaqKnowledgeBase knowledgeBase;

    /**
     * Receives the FAQ knowledge base through constructor injection. Answers are retrieved locally;
     * questions are not sent to an external AI service.
     */
    public IntentAssistantService(FaqKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Normalizes the language and question, selects the search strategy and checks the confidence
     * threshold. Returns a fallback message with a FAQ link when no sufficiently close entry
     * exists.
     */
    @Override
    public AssistantReply reply(String locale, String message) {
        String lang = knowledgeBase.normalizeLocale(locale);
        String text = message == null ? "" : message.trim();
        List<FaqEntry> entries = knowledgeBase.entries(lang);
        String faqPath = "/faq?lang=" + lang;

        if (text.isBlank()) {
            return fallback(lang, faqPath);
        }

        FaqEntry best;
        double score;
        if ("hy".equals(lang)) {
            // Uses curated keyword matching for Armenian questions.
            // This project does not include a full morphological analyzer for Armenian.
            Scored scored = bestByKeywords(text, entries);
            best = scored.entry();
            score = scored.score();
        } else {
            Scored scored = bestByTfIdf(text, entries, lang);
            best = scored.entry();
            score = scored.score();
        }

        if (best == null || score < MIN_SCORE) {
            return fallback(lang, faqPath);
        }
        return new AssistantReply(best.answer(), true, faqPath);
    }

    /**
     * Compares the question with FAQ questions and keywords using cosine similarity between TF-IDF
     * vectors. Curated keyword matches increase the score; the result selects existing text rather
     * than generating an answer.
     */
    private Scored bestByTfIdf(String query, List<FaqEntry> entries, String lang) {
        List<List<String>> docs = new ArrayList<>();
        for (FaqEntry e : entries) {
            docs.add(tokenize(e.question() + " " + String.join(" ", e.keywords()), lang));
        }
        List<String> qTokens = tokenize(query, lang);
        Map<String, Double> idf = idf(docs);
        double[] qVec = tfIdfVector(qTokens, idf);

        FaqEntry best = null;
        double bestScore = -1;
        for (int i = 0; i < entries.size(); i++) {
            double[] dVec = tfIdfVector(docs.get(i), idf);
            double sim = cosine(qVec, dVec);
            // A curated keyword match boosts the corresponding FAQ candidate's score.
            for (String kw : entries.get(i).keywords()) {
                String stemmedKw = stem(kw.toLowerCase(Locale.ROOT), lang);
                if (qTokens.contains(stemmedKw) || qTokens.contains(kw.toLowerCase(Locale.ROOT))) {
                    sim += 0.20;
                }
            }
            if (sim > bestScore) {
                bestScore = sim;
                best = entries.get(i);
            }
        }
        return new Scored(best, bestScore);
    }

    /**
     * Finds the Armenian answer using the proportion and number of matching keywords. This strategy
     * does not require a full Armenian morphological analyzer.
     */
    private Scored bestByKeywords(String query, List<FaqEntry> entries) {
        Set<String> qTokens = new HashSet<>(tokenize(query));
        FaqEntry best = null;
        double bestScore = -1;
        for (FaqEntry e : entries) {
            Set<String> bag = new HashSet<>(tokenize(e.question()));
            bag.addAll(e.keywords().stream().map(k -> k.toLowerCase(Locale.ROOT)).toList());
            int hits = 0;
            for (String t : qTokens) {
                if (bag.contains(t)) {
                    hits++;
                }
            }
            double score = qTokens.isEmpty() ? 0 : (double) hits / qTokens.size();
            // Adds a small bonus for the absolute number of keyword matches.
            score += hits * 0.08;
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return new Scored(best, bestScore);
    }

    /**
     * Returns a localized message explaining that no reliable match was found. Includes a FAQ link
     * so the user can continue searching.
     */
    private AssistantReply fallback(String lang, String faqPath) {
        String answer = switch (lang) {
            case "ru" -> "Я пока знаю ответы на частые вопросы о сайте. Загляните в раздел FAQ: " + faqPath;
            case "hy" -> "Ես առայժմ պատասխանում եմ կայքի հաճախակի հարցերին։ Տեսեք FAQ էջը՝ " + faqPath;
            default -> "I can help with common site questions. Please see the FAQ: " + faqPath;
        };
        return new AssistantReply(answer, false, faqPath);
    }

    /**
     * Splits text into lowercase words and numbers and ignores single-character tokens. The
     * language-independent overload uses English; Russian tokens receive simplified suffix
     * normalization. Generic question words are excluded from similarity calculations.
     */
    private List<String> tokenize(String text) {
        return tokenize(text, "en");
    }

    /**
     * Splits text into lowercase words and numbers and ignores single-character tokens. The
     * language-independent overload uses English; Russian tokens receive simplified suffix
     * normalization. Removes generic question words before calculating topic similarity.
     */
    private List<String> tokenize(String text, String lang) {
        List<String> tokens = new ArrayList<>();
        var matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String t = matcher.group();
            if (t.length() > 1 && !STOP_WORDS.contains(t)) {
                tokens.add(stem(t, lang));
            }
        }
        return tokens;
    }

    /**
     * Removes selected common Russian suffixes so related word forms can match. This is a
     * regular-expression heuristic, not a complete linguistic stemmer.
     */
    private String stem(String word, String lang) {
        if (!"ru".equals(lang) || word.length() < 4) {
            return word;
        }
        return word.replaceAll("(ами|ями|ов|ев|ом|ем|ой|ей|ах|ях|ть|ти|ешь|ете|ит|ут|ют|ат|ят|ого|его|ому|ему|ым|им|ую|юю|а|я|у|ю|о|е|и|ы)$", "");
    }

    /**
     * Computes inverse document frequency: words appearing in fewer FAQ entries receive more
     * weight. Counts each word once per document and applies smoothing to avoid extreme weights.
     */
    private Map<String, Double> idf(List<List<String>> docs) {
        Map<String, Integer> df = new HashMap<>();
        for (List<String> doc : docs) {
            for (String term : new HashSet<>(doc)) {
                df.merge(term, 1, Integer::sum);
            }
        }
        int n = docs.size();
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idf.put(e.getKey(), Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0);
        }
        return idf;
    }

    /**
     * Multiplies relative token frequencies by their IDF weights. Sorts the shared vocabulary so
     * every compared vector uses the same coordinate order.
     */
    private double[] tfIdfVector(List<String> tokens, Map<String, Double> idf) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            tf.merge(t, 1, Integer::sum);
        }
        List<String> vocab = new ArrayList<>(idf.keySet());
        vocab.sort(String::compareTo);
        double[] vec = new double[vocab.size()];
        int len = Math.max(tokens.size(), 1);
        for (int i = 0; i < vocab.size(); i++) {
            String term = vocab.get(i);
            double termTf = tf.getOrDefault(term, 0) / (double) len;
            vec[i] = termTf * idf.getOrDefault(term, 0.0);
        }
        return vec;
    }

    /**
     * Computes cosine similarity from the dot product and vector lengths. Returns zero for a
     * zero-length vector to avoid division by zero.
     */
    private double cosine(double[] a, double[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Internal assistant-answer candidate paired with its numerical similarity score.
     */
    private record Scored(FaqEntry entry, double score) {
    }
}
