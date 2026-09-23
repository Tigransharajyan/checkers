package com.checkers.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production database configuration.
 *
 * <p>Handles PostgreSQL connections from Render.com, Heroku, Fly.io, etc.
 * Supports both URI formats:
 * <ul>
 *   <li>{@code postgresql://user:pass@host:port/dbname} (Render connectionString / DATABASE_URL)</li>
 *   <li>{@code postgres://user:pass@host:port/dbname}</li>
 *   <li>{@code jdbc:postgresql://host:port/dbname}</li>
 *   <li>Separate environment variables: {@code DB_HOST}, {@code DB_PORT}, {@code DB_NAME}, {@code DB_USER}, {@code DB_PASSWORD}</li>
 * </ul>
 */
@Configuration
@Profile("prod")
@Slf4j
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:}}")
    private String rawUrl;

    @Value("${DB_HOST:}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:checkers}")
    private String dbName;

    @Value("${DB_USER:${SPRING_DATASOURCE_USERNAME:checkers}}")
    private String defaultUser;

    @Value("${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:checkers}}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(20000);

        ParsedDbConfig parsed = parseConnection();
        config.setJdbcUrl(parsed.jdbcUrl());
        if (parsed.username() != null && !parsed.username().isBlank()) {
            config.setUsername(parsed.username());
        }
        if (parsed.password() != null && !parsed.password().isBlank()) {
            config.setPassword(parsed.password());
        }

        log.info("Initialized production PostgreSQL DataSource for URL: {}", sanitizeUrl(parsed.jdbcUrl()));
        return new HikariDataSource(config);
    }

    public ParsedDbConfig parseConnection() {
        String jdbcUrl = null;
        String username = defaultUser;
        String password = defaultPassword;

        if (rawUrl != null && !rawUrl.isBlank()) {
            String trimmed = rawUrl.trim();
            if (trimmed.startsWith("jdbc:")) {
                jdbcUrl = trimmed;
            } else if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
                try {
                    String httpUrl = trimmed.replaceFirst("^(postgres|postgresql)://", "http://");
                    URI uri = URI.create(httpUrl);
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        if (parts.length > 1) {
                            password = parts[1];
                        }
                    }
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    if (path == null || path.isBlank() || "/".equals(path)) {
                        path = "/" + dbName;
                    }
                    jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        jdbcUrl += "?" + uri.getQuery();
                    }
                } catch (Exception e) {
                    log.error("Failed to parse PostgreSQL URL: {}", rawUrl, e);
                    throw new IllegalArgumentException("Invalid database URL format: " + rawUrl, e);
                }
            }
        }

        if (jdbcUrl == null) {
            String host = (dbHost != null && !dbHost.isBlank()) ? dbHost.trim() : "localhost";
            jdbcUrl = "jdbc:postgresql://" + host + ":" + dbPort + "/" + dbName;
        }

        return new ParsedDbConfig(jdbcUrl, username, password);
    }

    private String sanitizeUrl(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^/@]+@", ":***@");
    }

    public record ParsedDbConfig(String jdbcUrl, String username, String password) {}
}
