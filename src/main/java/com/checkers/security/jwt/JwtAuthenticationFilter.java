package com.checkers.security.jwt;

import com.checkers.security.CustomUserDetailsService;
import com.checkers.security.SecurityUser;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseAccessToken(token);
            if ("access".equals(claims.get("typ", String.class))
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = Long.valueOf(claims.getSubject());
                SecurityUser principal = userDetailsService.loadById(userId);
                // Development databases can reuse an ID after a restart.
                if (!principal.getUsername().equals(claims.get("username", String.class))) {
                    throw new IllegalArgumentException("Token account no longer exists");
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        } catch (ResourceNotFoundException ex) {
            if (ex.getErrorCode() != ErrorCode.USER_NOT_FOUND) throw ex;
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
