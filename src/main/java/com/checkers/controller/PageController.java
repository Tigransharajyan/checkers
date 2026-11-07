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

/**
 * MVC controller selecting Thymeleaf templates. Page rendering is separate from REST requests for
 * changing game data.
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final FaqKnowledgeBase faqKnowledgeBase;

    /**
     * Returns the main page's Thymeleaf template. The main URL and its supported alias render the
     * same screen.
     */
    @GetMapping({"/", "/index", "/home"})
    public String index() {
        return "index";
    }

    /**
     * Selects the login template. Credentials are submitted through a separate REST controller.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Selects the registration template without creating an account merely by opening the page.
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Chooses the game launcher or active-game template according to the game identifier or
     * invitation code in the request.
     */
    @GetMapping("/play")
    public String play(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) String code) {
        return id == null && (code == null || code.isBlank()) ? "play-menu" : "play";
    }

    /**
     * Selects the daily-puzzle template. Puzzle state and solution validation are obtained through
     * the API.
     */
    @GetMapping("/tasks")
    public String tasks() {
        return "tasks";
    }

    /**
     * Selects the personal-progress page backed by actual user statistics.
     */
    @GetMapping("/rating")
    public String rating() {
        return "rating";
    }

    /**
     * Selects the history template. The browser loads games through a separate authenticated
     * request.
     */
    @GetMapping("/history")
    public String history() {
        return "history";
    }

    /**
     * Adds localized knowledge-base entries to the model and renders the rules and FAQ page.
     */
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
