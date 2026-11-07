package com.checkers.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One untrusted conversation turn; clients may supply dialogue, never system instructions. */
public record AssistantTurn(
        @NotBlank @Pattern(regexp = "user|assistant") String role,
        @NotBlank @Size(max = 2000) String content) {}
