package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /** Earlier user questions only; clients cannot submit system roles or trusted instructions. */
    @Size(max = 4)
    private List<@NotBlank @Size(max = 500) String> recentQuestions;
}
