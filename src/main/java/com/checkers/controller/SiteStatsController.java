package com.checkers.controller;

import com.checkers.security.SecurityUser;
import com.checkers.service.SiteStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/site")
@RequiredArgsConstructor
public class SiteStatsController {
    private final SiteStatsService stats;

    @PostMapping("/presence")
    public ResponseEntity<Void> heartbeat(
            @CookieValue(name = "checkers_visitor", required = false) String visitor,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request, HttpServletResponse response) {
        if (visitor == null || !visitor.matches("[a-f0-9-]{36}")) {
            visitor = UUID.randomUUID().toString();
            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("checkers_visitor", visitor)
                    .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/")
                    .maxAge(Duration.ofDays(365)).build().toString());
        }
        stats.heartbeat(visitor, user == null ? null : user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<SiteStatsService.Stats> stats() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stats.stats());
    }
}
