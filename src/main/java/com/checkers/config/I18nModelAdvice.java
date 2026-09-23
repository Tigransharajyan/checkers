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
 * Exposes the full message bundle for the active locale so templates can
 * inline {@code window.i18n = {...}} without duplicating translations in JS.
 */
@ControllerAdvice
@Configuration
public class I18nModelAdvice {

    private final MessageSource messageSource;

    public I18nModelAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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

    @ModelAttribute("currentLocale")
    public String currentLocale(HttpServletRequest request) {
        return resolveLocale(request).getLanguage();
    }

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
