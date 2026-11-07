package com.checkers.config;

import com.checkers.security.CustomUserDetailsService;
import com.checkers.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Defines HTTP access rules, BCrypt password encoding and JWT filtering. Participant-specific game
 * access is also checked by the service layer.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configures public pages and authentication routes while requiring JWT authentication for
     * protected APIs. Disables server HTTP sessions, form login and HTTP Basic; the dedicated
     * filter validates Bearer tokens.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"errorCode\":\"UNAUTHORIZED\"}");
                }))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index",
                                "/home",
                                "/login",
                                "/register",
                                "/faq",
                                "/play",
                                "/tasks",
                                "/rating",
                                "/history",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/robots.txt",
                                "/sitemap.xml",
                                "/h2-console/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/guest",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/assistant/chat"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/i18n", "/api/site/stats", "/api/tasks/daily").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/site/presence", "/api/tasks/*/moves").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .userDetailsService(userDetailsService)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Creates the BCrypt encoder used to hash passwords and compare login input against stored
     * hashes. Plaintext passwords are not saved in the user table.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager supplied by Spring Security's configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
