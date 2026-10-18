package com.checkers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.site")
public class SiteProperties {
    /** Configured public origin; never inferred from the request Host header. */
    private String baseUrl = "http://localhost:8080";
    private boolean indexingEnabled = true;
    private String googleVerification = "";
    private String bingVerification = "";

    public void setBaseUrl(String value) {
        URI uri = URI.create(value.trim());
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException("app.site.base-url must be an HTTP(S) origin without a path, query or credentials");
        }
        baseUrl = value.trim().replaceAll("/+$", "");
    }
}
