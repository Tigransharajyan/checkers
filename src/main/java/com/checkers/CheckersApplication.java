package com.checkers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point. Its annotations enable automatic configuration, component
 * discovery and embedded-server startup.
 */
@SpringBootApplication
public class CheckersApplication {

    /**
     * Starts Spring Boot, creates the dependency container, configures persistence and opens the
     * embedded HTTP/WebSocket server. Passes command-line arguments to Spring unchanged.
     */
    public static void main(String[] args) {
        SpringApplication.run(CheckersApplication.class, args);
    }
}
