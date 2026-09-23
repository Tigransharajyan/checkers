package com.checkers.controller;

import com.checkers.assistant.FaqKnowledgeBase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final FaqKnowledgeBase faqKnowledgeBase;

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/play")
    public String play(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) String code) {
        return id == null && (code == null || code.isBlank()) ? "play-menu" : "play";
    }

    @GetMapping("/tasks")
    public String tasks() {
        return "tasks";
    }

    @GetMapping("/rating")
    public String rating() {
        return "rating";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    @GetMapping("/faq")
    public String faq(Model model, HttpServletRequest request) {
        Locale locale = Locale.ENGLISH;
        LocaleResolver resolver = RequestContextUtils.getLocaleResolver(request);
        if (resolver != null && resolver.resolveLocale(request) != null) {
            locale = resolver.resolveLocale(request);
        }
        String lang = faqKnowledgeBase.normalizeLocale(locale.getLanguage());
        model.addAttribute("faqEntries", faqKnowledgeBase.entries(lang));
        model.addAttribute("faqLocale", lang);
        return "faq";
    }
}
