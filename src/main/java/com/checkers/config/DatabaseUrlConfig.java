package com.checkers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Converts Render's postgres:// DATABASE_URL to jdbc:postgresql:// format
 * that Spring Boot / HikariCP expects.
 *
 * <p>Render PostgreSQL provides {@code SPRING_DATASOURCE_URL} via Blueprint's
 * {@code fromDatabase.connectionString}, which returns a URI like:
 * {@code postgres://user:pass@host:5432/dbname}
 *
 * <p>Spring Boot's auto-configuration accepts {@code SPRING_DATASOURCE_URL}
 * directly as {@code spring.datasource.url}. We convert the scheme here
 * at startup so Hikari always receives a valid JDBC URL.
 */
@Configuration
@Profile("prod")
@Slf4j
public class DatabaseUrlConfig {

    @PostConstruct
    public void fixDatabaseUrl() {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null) {
            url = System.getProperty("spring.datasource.url");
        }
        if (url != null && url.startsWith("postgres://")) {
            String jdbcUrl = "jdbc:postgresql://" + url.substring("postgres://".length());
            System.setProperty("spring.datasource.url", jdbcUrl);
            log.info("Converted DATABASE_URL to JDBC format: jdbc:postgresql://***");
        }
    }
}
