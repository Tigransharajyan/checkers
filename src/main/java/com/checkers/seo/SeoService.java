package com.checkers.seo;

import com.checkers.assistant.FaqKnowledgeBase;
import com.checkers.config.SiteProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

/**
 * Builds canonical links, language variants, metadata and JSON-LD from actual site content.
 * Excludes personal and game pages from indexing.
 */
@Service
@RequiredArgsConstructor
public class SeoService {
    public static final List<String> LANGUAGES = List.of("en", "ru", "hy");
    public static final List<String> INDEXABLE_PATHS = List.of("/", "/faq");
    public static final Map<String, String> PAGES = Map.of(
            "/", "home", "/home", "home", "/faq", "faq", "/play", "play", "/tasks", "tasks",
            "/rating", "progress", "/history", "history", "/login", "login", "/register", "register");
    private final SiteProperties site;
    private final MessageSource messages;
    private final ObjectMapper mapper;
    private final FaqKnowledgeBase faq;
    private final Environment environment;

    /**
     * Allows indexing only when configuration and the public deployment origin permit it. Excludes
     * development profiles, localhost and service addresses.
     */
    public boolean indexingEnabled() {
        String host = URI.create(site.getBaseUrl()).getHost().toLowerCase(Locale.ROOT);
        return site.isIndexingEnabled() && !environment.acceptsProfiles(Profiles.of("dev"))
                && !host.equals("localhost") && !host.endsWith(".localhost") && !host.equals("example.com")
                && !host.startsWith("127.") && !host.equals("[::1]") && !host.equals("0.0.0.0");
    }

    /**
     * Requires both global indexing permission and membership in the public indexable-path list.
     */
    public boolean isIndexable(String path) { return indexingEnabled() && INDEXABLE_PATHS.contains(path); }
    /**
     * Returns the prevalidated public origin from configuration rather than the incoming Host
     * header.
     */
    public String baseUrl() { return site.getBaseUrl(); }
    /**
     * Combines the public origin, page path and language parameter for canonical links and sitemap
     * entries.
     */
    public String localizedUrl(String path, String language) { return baseUrl() + path + "?lang=" + language; }

    /**
     * Builds localized titles, descriptions, canonical URLs, robots directives, language
     * alternatives and social metadata. The /home alias uses / as its canonical path.
     */
    public Metadata metadata(String path, Locale locale) {
        String language = LANGUAGES.contains(locale.getLanguage()) ? locale.getLanguage() : "en";
        Locale supportedLocale = Locale.forLanguageTag(language);
        String page = PAGES.getOrDefault(path, "error");
        String title = text("seo." + page + ".title", supportedLocale);
        String description = text("seo." + page + ".description", supportedLocale);
        String canonicalPath = path.equals("/home") ? "/" : (PAGES.containsKey(path) ? path : "/");
        String canonical = localizedUrl(canonicalPath, language);
        Map<String, String> alternates = new LinkedHashMap<>();
        if (INDEXABLE_PATHS.contains(canonicalPath)) {
            LANGUAGES.forEach(lang -> alternates.put(lang, localizedUrl(canonicalPath, lang)));
            alternates.put("x-default", localizedUrl(canonicalPath, "en"));
        }
        String robots = isIndexable(path) ? "index,follow,max-image-preview:large" : "noindex,follow";
        String image = baseUrl() + "/images/checkers-social.jpg";
        String json = INDEXABLE_PATHS.contains(path) ? structuredData(path, supportedLocale, title, description, canonical, image) : null;
        return new Metadata(title, description, canonical, robots, alternates, language,
                switch (language) { case "ru" -> "ru_RU"; case "hy" -> "hy_AM"; default -> "en_US"; },
                image, text("seo.image.alt", supportedLocale), json,
                Set.of("/", "/login", "/register").contains(path),
                site.getGoogleVerification(), site.getBingVerification());
    }

    /**
     * Builds JSON-LD from actual public site content and FAQ entries. Escapes angle brackets and
     * ampersands before embedding JSON in an HTML script element.
     */
    private String structuredData(String path, Locale locale, String title, String description, String canonical, String image) {
        String websiteId = baseUrl() + "/#website", gameId = baseUrl() + "/#game";
        List<Map<String, Object>> graph = new ArrayList<>();
        if (path.equals("/")) {
            graph.add(Map.of("@type", "WebSite", "@id", websiteId, "url", localizedUrl("/", "en"),
                    "name", text("app.title", Locale.ENGLISH),
                    "alternateName", List.of(text("app.title", Locale.forLanguageTag("ru")), text("app.title", Locale.forLanguageTag("hy"))),
                    "inLanguage", LANGUAGES));
            graph.add(Map.of("@type", "VideoGame", "@id", gameId, "name", text("app.title", locale),
                    "url", canonical, "description", description, "image", image, "inLanguage", LANGUAGES,
                    "gamePlatform", "Web browser", "genre", "Board game",
                    "playMode", List.of("https://schema.org/SinglePlayer", "https://schema.org/MultiPlayer")));
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("@type", path.equals("/faq") ? "FAQPage" : "WebPage");
        page.put("@id", canonical + "#webpage");
        page.put("url", canonical);
        page.put("name", title);
        page.put("description", description);
        page.put("inLanguage", locale.getLanguage());
        page.put("isPartOf", Map.of("@id", websiteId));
        page.put("about", Map.of("@id", gameId));
        if (path.equals("/faq")) {
            page.put("mainEntity", faq.entries(locale.getLanguage()).stream().map(entry -> Map.of(
                    "@type", "Question", "name", entry.question(),
                    "acceptedAnswer", Map.of("@type", "Answer", "text", entry.answer()))).toList());
        } else {
            page.put("mainEntity", Map.of("@id", gameId));
        }
        graph.add(page);
        try {
            return mapper.writeValueAsString(Map.of("@context", "https://schema.org", "@graph", graph))
                    .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize structured data", ex); }
    }

    /**
     * Resolves one localized SEO message through MessageSource.
     */
    private String text(String key, Locale locale) { return messages.getMessage(key, null, locale); }

    /**
     * Immutable per-page SEO model containing localized text, canonical URL, robots directives,
     * language variants and social metadata.
     */
    public record Metadata(String title, String description, String canonical, String robots,
                           Map<String, String> alternates, String language, String ogLocale,
                           String image, String imageAlt, String structuredData, boolean preloadHero,
                           String googleVerification, String bingVerification) {}
}
