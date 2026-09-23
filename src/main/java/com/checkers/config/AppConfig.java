package com.checkers.config;

import com.checkers.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.checkers.engine.CheckersRulesEngine;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    @Bean
    public CheckersRulesEngine checkersRulesEngine() {
        return new CheckersRulesEngine();
    }
}
