package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private String errorCode;
    private Instant timestamp;
    /** Machine-readable details for frontend substitution — never localized text. */
    private Map<String, Object> details;
}
