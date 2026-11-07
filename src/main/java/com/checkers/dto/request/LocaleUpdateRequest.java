package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming profile-language DTO. The service also checks supported language values; Lombok
 * generates accessors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocaleUpdateRequest {

    /** Supported two-letter language code: en, ru or hy. */
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "en|ru|hy")
    private String locale;
}
