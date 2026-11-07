package com.checkers.controller;

import com.checkers.assistant.AssistantReply;
import com.checkers.assistant.AssistantReplyService;
import com.checkers.dto.request.AssistantChatRequest;
import com.checkers.dto.response.AssistantChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for the assistant. Validates request shape and delegates answer retrieval to the
 * configured service implementation.
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantReplyService assistantReplyService;

    /**
     * Accepts the validated chat request, delegates answer retrieval and returns the public
     * response DTO. FAQ search belongs to the assistant service.
     */
    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        AssistantReply reply = assistantReplyService.reply(
                request.getLocale(), request.getMessage(), request.getRecentQuestions());
        return ResponseEntity.ok(AssistantChatResponse.builder()
                .answer(reply.answer())
                .matched(reply.matched())
                .faqPath(reply.faqPath())
                .build());
    }
}
