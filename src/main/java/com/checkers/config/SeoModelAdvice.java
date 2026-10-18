package com.checkers.config;

import com.checkers.controller.PageController;
import com.checkers.seo.SeoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

@ControllerAdvice(assignableTypes = PageController.class)
@RequiredArgsConstructor
public class SeoModelAdvice {
    private final SeoService seo;

    @ModelAttribute("languageLinks")
    public java.util.Map<String, String> languageLinks(HttpServletRequest request) {
        java.util.Map<String, String> links = new java.util.LinkedHashMap<>();
        SeoService.LANGUAGES.forEach(language -> links.put(language,
                org.springframework.web.util.UriComponentsBuilder.fromPath(request.getRequestURI())
                        .query(request.getQueryString()).replaceQueryParam("lang", language).build(true).toUriString()));
        return links;
    }

    @ModelAttribute("seo")
    public SeoService.Metadata metadata(HttpServletRequest request, HttpServletResponse response) {
        var metadata = seo.metadata(request.getRequestURI(), RequestContextUtils.getLocale(request));
        response.setHeader("Content-Language", metadata.language());
        return metadata;
    }
}
