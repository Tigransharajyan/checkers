package com.checkers.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Adds the current language and shared translation dictionary to PageController views. Server
 * templates and browser code use the same message keys.
 */
@ControllerAdvice(assignableTypes = com.checkers.controller.PageController.class)
@Configuration
public class I18nModelAdvice {

    private final MessageSource messageSource;

    /**
     * Receives Spring's message source, shared by server-rendered templates and the browser
     * translation dictionary.
     */
    public I18nModelAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Builds the known translation dictionary in the current request language. Templates expose it
     * as window.i18n so JavaScript and Thymeleaf use the same messages.
     */
    @ModelAttribute("i18nMap")
    public Map<String, String> i18nMap(HttpServletRequest request) {
        Locale locale = resolveLocale(request);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : loadKeys()) {
            map.put(key, messageSource.getMessage(key, null, key, locale));
        }
        map.put("_locale", locale.getLanguage());
        return map;
    }

    /**
     * Returns the short language code used by the HTML lang attribute and language links.
     */
    @ModelAttribute("currentLocale")
    public String currentLocale(HttpServletRequest request) {
        return resolveLocale(request).getLanguage();
    }

    /**
     * Resolves the request language through the configured LocaleResolver. Uses English if the
     * resolver supplies no locale.
     */
    private Locale resolveLocale(HttpServletRequest request) {
        LocaleResolver resolver = RequestContextUtils.getLocaleResolver(request);
        if (resolver != null) {
            Locale locale = resolver.resolveLocale(request);
            if (locale != null) {
                return locale;
            }
        }
        return Locale.ENGLISH;
    }

    private static volatile Iterable<String> cachedKeys;

    /**
     * Loads and sorts the base message keys once. A volatile reference and synchronized
     * initialization safely publish the shared cache to concurrent requests.
     */
    public static Iterable<String> loadKeys() {
        Iterable<String> local = cachedKeys;
        if (local != null) {
            return local;
        }
        synchronized (I18nModelAdvice.class) {
            if (cachedKeys == null) {
                try (var in = I18nModelAdvice.class.getClassLoader()
                        .getResourceAsStream("i18n/messages.properties")) {
                    PropertyResourceBundle bundle = new PropertyResourceBundle(
                            new InputStreamReader(in, StandardCharsets.UTF_8));
                    cachedKeys = bundle.keySet().stream().sorted().toList();
                } catch (Exception e) {
                    cachedKeys = java.util.List.of("app.title");
                }
            }
            return cachedKeys;
        }
    }
}
