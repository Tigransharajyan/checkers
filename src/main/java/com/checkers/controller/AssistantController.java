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

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantReplyService assistantReplyService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        AssistantReply reply = assistantReplyService.reply(request.getLocale(), request.getMessage());
        return ResponseEntity.ok(AssistantChatResponse.builder()
                .answer(reply.answer())
                .matched(reply.matched())
                .faqPath(reply.faqPath())
                .build());
    }
}
