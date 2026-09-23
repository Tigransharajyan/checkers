package com.checkers.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SeoModelAdvice {

    private final SiteProperties siteProperties;

    public SeoModelAdvice(SiteProperties siteProperties) {
        this.siteProperties = siteProperties;
    }

    @ModelAttribute("siteBaseUrl")
    public String siteBaseUrl() {
        String base = siteProperties.getBaseUrl();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    @ModelAttribute("canonicalPath")
    public String canonicalPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank() || "/index".equals(uri)) {
            return "/";
        }
        return uri;
    }
}
