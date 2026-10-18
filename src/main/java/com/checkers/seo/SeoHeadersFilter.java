package com.checkers.seo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SeoHeadersFilter extends OncePerRequestFilter {
    private final SeoService seo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean asset = java.util.List.of("/images/", "/css/", "/js/", "/fonts/").stream().anyMatch(path::startsWith);
        if (!seo.indexingEnabled() || (!seo.isIndexable(path) && !asset)) response.setHeader("X-Robots-Tag", "noindex, follow");
        chain.doFilter(request, response);
    }
}
