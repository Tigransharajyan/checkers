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
 * i18n skeleton. Default language is always English — never Accept-Language.
 * Locale is switched via {@code ?lang=} (interceptor) and/or cookie / user preference.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final String LOCALE_COOKIE_NAME = "CHECKERS_LOCALE";

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME) {
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

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
