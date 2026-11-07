package com.checkers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * Selects language through the lang parameter and locale cookie, defaulting to English.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final String LOCALE_COOKIE_NAME = "CHECKERS_LOCALE";

    /**
     * Creates the CHECKERS_LOCALE cookie resolver with English as the default. Unsupported
     * languages also resolve to English.
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME) {
            /**
             * Restricts the cookie-resolved language to en, ru or hy. Reduces regional variants to
             * their supported base language.
             */
            @Override
            public Locale resolveLocale(jakarta.servlet.http.HttpServletRequest request) {
                Locale locale = super.resolveLocale(request);
                return com.checkers.seo.SeoService.LANGUAGES.contains(locale.getLanguage())
                        ? Locale.forLanguageTag(locale.getLanguage()) : Locale.ENGLISH;
            }
        };
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookieHttpOnly(false);
        resolver.setCookiePath("/");
        return resolver;
    }

    /**
     * Creates the Spring MVC interceptor that applies language changes from the lang request
     * parameter.
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    /**
     * Registers the language-change interceptor in the HTTP request processing chain.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
