package com.checkers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.site")
public class SiteProperties {

    /** Public origin used for canonical / hreflang (must match sitemap.xml host). */
    private String baseUrl = "https://example.com";
}
