package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming assistant-question DTO. Bean Validation checks field constraints; Lombok generates
 * accessors and utility constructors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatRequest {

    @NotBlank
    @Size(max = 500)
    private String message;

    /** Requested reply language from the interface: en, ru or hy. */
    @NotBlank
    @Size(min = 2, max = 8)
    private String locale;
}
