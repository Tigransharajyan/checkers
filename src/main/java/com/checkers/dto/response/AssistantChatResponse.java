package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public assistant response separated from the internal retrieval result. Lombok generates
 * accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantChatResponse {

    private String answer;
    private boolean matched;
    private String faqPath;
}
