package com.checkers.config;

import com.checkers.assistant.FaqKnowledgeBase;
import com.checkers.seo.SeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class SeoSafetyTest {
    @Test
    void originMustNotContainCredentialsOrAnUntrustedPath() {
        SiteProperties site = new SiteProperties();
        for (String value : new String[]{"javascript:evil", "https://user:pass@host.test", "https://host.test/path", "https://host.test/?lang=ru", "https://host.test/#fragment"}) {
            assertThrows(IllegalArgumentException.class, () -> site.setBaseUrl(value));
        }
        site.setBaseUrl("https://checkers.example/");
        assertEquals("https://checkers.example", site.getBaseUrl());
    }

    @Test
    void developmentLocalhostAndDisabledStagingNeverBecomeIndexable() {
        SiteProperties site = new SiteProperties();
        MockEnvironment environment = new MockEnvironment();
        SeoService seo = new SeoService(site, new StaticMessageSource(), new ObjectMapper(), new FaqKnowledgeBase(), environment);
        assertFalse(seo.indexingEnabled());
        site.setBaseUrl("https://checkers.example"); assertTrue(seo.indexingEnabled());
        environment.setActiveProfiles("dev"); assertFalse(seo.indexingEnabled());
        environment.setActiveProfiles("prod"); site.setIndexingEnabled(false); assertFalse(seo.indexingEnabled());
    }
}
