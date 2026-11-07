package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Common API failure DTO with machine-readable code, server timestamp and details. The browser
 * selects localized wording; Lombok generates accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private String errorCode;
    private Instant timestamp;
    /** Structured error parameters; the browser selects the localized message. */
    private Map<String, Object> details;
}
