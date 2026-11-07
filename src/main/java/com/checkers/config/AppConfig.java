package com.checkers.config;

import com.checkers.assistant.GroqProperties;
import com.checkers.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.checkers.engine.CheckersRulesEngine;

/**
 * Registers the rules engine and binds JWT and server-side assistant settings.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, GroqProperties.class})
public class AppConfig {

    /**
     * Registers the shared rules engine as a Spring bean for constructor injection. The engine
     * itself remains independent of HTTP, Spring and persistence.
     */
    @Bean
    public CheckersRulesEngine checkersRulesEngine() {
        return new CheckersRulesEngine();
    }
}
