package com.checkers.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigTest {

    @Test
    void parsesRenderPostgresqlUri() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "postgresql://checkers:jBa9ybB8YMfjekmTaidRzawmRZk8Ws35@dpg-dapuikvavr4c73c4f74g-a/checkers_fqfv");
        ReflectionTestUtils.setField(config, "defaultUser", "checkers");
        ReflectionTestUtils.setField(config, "defaultPassword", "checkers");

        DatabaseConfig.ParsedDbConfig parsed = config.parseConnection();

        assertEquals("jdbc:postgresql://dpg-dapuikvavr4c73c4f74g-a:5432/checkers_fqfv", parsed.jdbcUrl());
        assertEquals("checkers", parsed.username());
        assertEquals("jBa9ybB8YMfjekmTaidRzawmRZk8Ws35", parsed.password());
    }

    @Test
    void parsesPostgresUriWithPortAndQuery() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "postgres://myuser:mypass@db.render.com:5433/mydb?sslmode=require");
        ReflectionTestUtils.setField(config, "defaultUser", "checkers");
        ReflectionTestUtils.setField(config, "defaultPassword", "checkers");

        DatabaseConfig.ParsedDbConfig parsed = config.parseConnection();

        assertEquals("jdbc:postgresql://db.render.com:5433/mydb?sslmode=require", parsed.jdbcUrl());
        assertEquals("myuser", parsed.username());
        assertEquals("mypass", parsed.password());
    }

    @Test
    void preservesExistingJdbcUrl() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "jdbc:postgresql://localhost:5432/checkers");
        ReflectionTestUtils.setField(config, "defaultUser", "myuser");
        ReflectionTestUtils.setField(config, "defaultPassword", "mypass");

        DatabaseConfig.ParsedDbConfig parsed = config.parseConnection();

        assertEquals("jdbc:postgresql://localhost:5432/checkers", parsed.jdbcUrl());
        assertEquals("myuser", parsed.username());
        assertEquals("mypass", parsed.password());
    }

    @Test
    void fallsBackToIndividualHostProperties() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "");
        ReflectionTestUtils.setField(config, "dbHost", "pg.internal");
        ReflectionTestUtils.setField(config, "dbPort", "5432");
        ReflectionTestUtils.setField(config, "dbName", "checkers_db");
        ReflectionTestUtils.setField(config, "defaultUser", "appuser");
        ReflectionTestUtils.setField(config, "defaultPassword", "secret");

        DatabaseConfig.ParsedDbConfig parsed = config.parseConnection();

        assertEquals("jdbc:postgresql://pg.internal:5432/checkers_db", parsed.jdbcUrl());
        assertEquals("appuser", parsed.username());
        assertEquals("secret", parsed.password());
    }

    @Test
    void parsesUriWithoutUserInfoUsesDefaults() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "rawUrl", "postgresql://db.render.com:5432/checkers_db");
        ReflectionTestUtils.setField(config, "defaultUser", "default_usr");
        ReflectionTestUtils.setField(config, "defaultPassword", "default_pwd");

        DatabaseConfig.ParsedDbConfig parsed = config.parseConnection();

        assertEquals("jdbc:postgresql://db.render.com:5432/checkers_db", parsed.jdbcUrl());
        assertEquals("default_usr", parsed.username());
        assertEquals("default_pwd", parsed.password());
    }
}
