package com.checkers.controller;

import com.checkers.config.I18nModelAdvice;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
@RequiredArgsConstructor
@Validated
public class I18nController {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    @GetMapping
    public Map<String, String> messages(
            @RequestParam @Pattern(regexp = "en|ru|hy") String lang,
            HttpServletRequest request,
            HttpServletResponse response) {
        Locale locale = Locale.forLanguageTag(lang);
        localeResolver.setLocale(request, response, locale);

        Map<String, String> map = new LinkedHashMap<>();
        for (String key : I18nModelAdvice.loadKeys()) {
            map.put(key, messageSource.getMessage(key, null, key, locale));
        }
        map.put("_locale", locale.getLanguage());
        return map;
    }
}
