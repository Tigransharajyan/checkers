package com.checkers.controller;

import com.checkers.dto.request.LocaleUpdateRequest;
import com.checkers.dto.response.UserResponse;
import com.checkers.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LocaleResolver localeResolver;

    @PatchMapping("/me/locale")
    public ResponseEntity<UserResponse> updateLocale(@Valid @RequestBody LocaleUpdateRequest request,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        UserResponse user = userService.updatePreferredLocale(request);
        localeResolver.setLocale(httpRequest, httpResponse, Locale.forLanguageTag(request.getLocale()));
        return ResponseEntity.ok(user);
    }
}
