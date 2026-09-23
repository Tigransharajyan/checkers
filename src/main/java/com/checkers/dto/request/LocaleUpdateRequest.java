package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocaleUpdateRequest {

    /** ISO 639-1 language code: en, ru, hy. */
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "en|ru|hy")
    private String locale;
}
