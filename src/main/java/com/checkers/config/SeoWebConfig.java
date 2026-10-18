package com.checkers.config;

import com.checkers.seo.SeoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

@Configuration
public class SeoWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String original = request.getRequestURI();
                String path = original.equals("/index") ? "/" : original;
                if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
                if (!SeoService.PAGES.containsKey(path)) return true;
                String[] languages = request.getParameterValues("lang");
                String language = languages == null ? null : languages[0].toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
                if (language != null && !SeoService.LANGUAGES.contains(language)) language = "en";
                boolean normalizeLanguage = languages != null && (languages.length != 1 || !languages[0].equals(language));
                if (!path.equals(original) || normalizeLanguage) {
                    var location = UriComponentsBuilder.fromPath(path).query(request.getQueryString());
                    if (normalizeLanguage) location.replaceQueryParam("lang", language);
                    response.setStatus(301);
                    response.setHeader("Location", location.build(true).toUriString());
                    return false;
                }
                return true;
            }
        }).order(-10);
    }
}
