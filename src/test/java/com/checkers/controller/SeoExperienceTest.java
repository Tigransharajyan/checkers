package com.checkers.controller;

import com.checkers.seo.SeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.site.base-url=https://checkers.example/", "app.site.indexing-enabled=true",
        "app.site.google-verification=google-test-token", "app.site.bing-verification=bing-test-token",
        "spring.datasource.url=jdbc:h2:mem:seo;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.web.resources.chain.strategy.content.enabled=true",
        "spring.web.resources.chain.strategy.content.paths=/css/**,/js/**,/images/**,/fonts/**",
        "spring.web.resources.cache.cachecontrol.max-age=365d", "logging.level.com.checkers=INFO"
})
@ActiveProfiles("seo-test")
@AutoConfigureMockMvc
class SeoExperienceTest {
    private static final String ORIGIN = "https://checkers.example";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void allPagesHaveUniqueLocalizedMetadataAndOnlyPublicContentIsIndexable() throws Exception {
        Set<String> titles = new HashSet<>(), descriptions = new HashSet<>();
        for (String language : SeoService.LANGUAGES) {
            for (String path : SeoService.PAGES.keySet()) {
                var response = mvc.perform(get(path).param("lang", language).param("utm_source", "test"))
                        .andExpect(status().isOk()).andExpect(header().string("Content-Language", language)).andReturn().getResponse();
                String html = response.getContentAsString(StandardCharsets.UTF_8);
                var title = Pattern.compile("<title>(.*?)</title>").matcher(html);
                assertTrue(title.find()); assertTrue(titles.add(title.group(1)), "Duplicate title " + path + language);
                String description = attribute(html, "meta", "name", "description", "content");
                assertTrue(description.length() > 40); assertTrue(descriptions.add(description), "Duplicate description");
                String canonical = ORIGIN + path + "?lang=" + language;
                assertEquals(canonical, attribute(html, "link", "rel", "canonical", "href"));
                assertEquals(canonical, attribute(html, "meta", "property", "og:url", "content"));
                assertNotNull(attribute(html, "meta", "name", "twitter:card", "content"));
                assertEquals("google-test-token", attribute(html, "meta", "name", "google-site-verification", "content"));
                assertEquals("bing-test-token", attribute(html, "meta", "name", "msvalidate.01", "content"));
                String robots = attribute(html, "meta", "name", "robots", "content");
                if (SeoService.INDEXABLE_PATHS.contains(path)) {
                    assertTrue(robots.startsWith("index,"));
                    assertNull(response.getHeader("X-Robots-Tag"));
                    for (String alternate : SeoService.LANGUAGES) {
                        assertEquals(ORIGIN + path + "?lang=" + alternate,
                                attribute(html, "link", "hreflang", alternate, "href"));
                    }
                    assertEquals(1, Pattern.compile("<h1\\b").matcher(html).results().count());
                } else {
                    assertEquals("noindex,follow", robots);
                    assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
                    assertFalse(html.contains("hreflang=\"x-default\""));
                }
                for (String alternate : SeoService.LANGUAGES) {
                    assertTrue(attribute(html, "a", "data-lang", alternate, "href").contains("lang=" + alternate));
                }
            }
        }
    }

    @Test
    void sitemapContainsEveryIndexableLanguageAndRobotsAllowsReadingNoindexPages() throws Exception {
        byte[] xml = mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        var urls = document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url");
        assertEquals(6, urls.getLength());
        Set<String> locations = new HashSet<>();
        for (int i = 0; i < urls.getLength(); i++) {
            var url = (org.w3c.dom.Element) urls.item(i);
            String location = url.getElementsByTagNameNS("*", "loc").item(0).getTextContent();
            assertTrue(locations.add(location));
            assertTrue(SeoService.INDEXABLE_PATHS.stream().anyMatch(path -> location.startsWith(ORIGIN + path + "?lang=")));
            var alternates = url.getElementsByTagNameNS("http://www.w3.org/1999/xhtml", "link");
            assertEquals(4, alternates.getLength());
            for (int j = 0; j < alternates.getLength(); j++) {
                var alternate = (org.w3c.dom.Element) alternates.item(j);
                assertTrue(alternate.getAttribute("href").startsWith(ORIGIN));
                assertEquals("alternate", alternate.getAttribute("rel"));
            }
        }
        String sitemap = new String(xml, StandardCharsets.UTF_8);
        assertFalse(sitemap.contains("lastmod")); assertFalse(sitemap.contains("/login")); assertFalse(sitemap.contains("example.com"));
        String robots = mvc.perform(get("/robots.txt")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(robots.contains("Sitemap: " + ORIGIN + "/sitemap.xml"));
        assertFalse(robots.contains("Disallow: /login")); assertFalse(robots.contains("Disallow: /play"));
        assertTrue(robots.contains("Disallow: /api/"));
    }

    @Test
    void structuredDataIsParseableAndUsesOnlyRealVisibleContentWithoutRatingsOrOffers() throws Exception {
        for (String language : SeoService.LANGUAGES) {
            for (String path : SeoService.INDEXABLE_PATHS) {
                String html = mvc.perform(get(path).param("lang", language)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
                var script = Pattern.compile("<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>", Pattern.DOTALL).matcher(html);
                assertTrue(script.find());
                String json = script.group(1);
                var data = mapper.readTree(json);
                assertEquals("https://schema.org", data.get("@context").asText());
                assertFalse(json.contains("aggregateRating")); assertFalse(json.contains("review")); assertFalse(json.contains("offers"));
                assertFalse(json.contains("Organization")); assertFalse(json.contains("address"));
                for (var node : data.get("@graph")) {
                    assertTrue(node.has("@type"));
                    if (node.get("@type").asText().equals("FAQPage")) {
                        assertEquals(11, node.get("mainEntity").size());
                        for (var question : node.get("mainEntity")) {
                            assertEquals("Question", question.get("@type").asText());
                            assertEquals("Answer", question.get("acceptedAnswer").get("@type").asText());
                            String normalized = HtmlUtils.htmlUnescape(html);
                            assertTrue(normalized.contains(question.get("name").asText()));
                            assertTrue(normalized.contains(question.get("acceptedAnswer").get("text").asText()));
                        }
                    }
                }
            }
        }
    }

    @Test
    void normalizesAliasesAndLanguagesWithoutDroppingGameOrInviteParameters() throws Exception {
        mvc.perform(get("/index?lang=ru")).andExpect(status().isMovedPermanently()).andExpect(header().string("Location", "/?lang=ru"));
        mvc.perform(get("/faq/?lang=hy")).andExpect(status().isMovedPermanently()).andExpect(header().string("Location", "/faq?lang=hy"));
        mvc.perform(get("/play?id=42&code=INVITE&lang=de")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/play?id=42&code=INVITE&lang=en"));
        mvc.perform(get("/faq?lang=RU")).andExpect(status().isMovedPermanently()).andExpect(header().string("Location", "/faq?lang=ru"));
        mvc.perform(get("/does-not-exist")).andExpect(status().isNotFound()).andExpect(header().string("X-Robots-Tag", "noindex, follow"));
        String html = mvc.perform(get("/play?id=42&code=INVITE&lang=ru")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(ORIGIN + "/play?lang=ru", attribute(html, "link", "rel", "canonical", "href"));
        assertTrue(attribute(html, "a", "data-lang", "hy", "href").contains("code=INVITE"));
    }

    @Test
    void contentVersionedAssetsAreLocallyServedAndCacheable() throws Exception {
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("cdn.jsdelivr.net")); assertFalse(html.contains("fonts.googleapis.com"));
        var stylesheet = Pattern.compile("href=\"(/css/theme-[a-f0-9]{32}\\.css)\"").matcher(html);
        assertTrue(stylesheet.find(), "Fingerprinting must rewrite template links");
        mvc.perform(get(stylesheet.group(1))).andExpect(status().isOk()).andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=31536000")));
        String fonts = mvc.perform(get("/css/fonts.css")).andReturn().getResponse().getContentAsString();
        assertTrue(fonts.contains("/fonts/")); assertFalse(fonts.contains("https://fonts.gstatic"));
        mvc.perform(get("/images/checkers-social.jpg")).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
    }

    private static String attribute(String html, String tag, String selector, String value, String target) {
        var tags = Pattern.compile("<" + tag + "\\b[^>]*>").matcher(html);
        while (tags.find()) {
            String text = tags.group();
            if (!text.contains(selector + "=\"" + value + "\"")) continue;
            var attribute = Pattern.compile("(?:\\s)" + target + "=\"([^\"]*)\"").matcher(text);
            if (attribute.find()) return HtmlUtils.htmlUnescape(attribute.group(1));
        }
        return null;
    }
}
