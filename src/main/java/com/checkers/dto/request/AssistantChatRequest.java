package com.checkers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatRequest {

    @NotBlank
    @Size(max = 500)
    private String message;

    /** ISO language: en | ru | hy (from current UI locale). */
    @NotBlank
    @Size(min = 2, max = 8)
    private String locale;
}
