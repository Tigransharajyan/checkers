package com.checkers.assistant;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-only Groq settings. Lombok generates accessors but deliberately no toString method,
 * which could otherwise expose credentials in diagnostic output. Empty keys enable local help.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.assistant.groq")
public class GroqProperties {
    private List<String> apiKeys = new ArrayList<>();
    private String model = "openai/gpt-oss-20b";
    private int requestsPerMinute = 12;
}
