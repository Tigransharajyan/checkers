package com.checkers.websocket.dto;

import com.checkers.model.enums.EventType;
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
public class MatchmakingMessage {

    private EventType type;
    private Instant timestamp;
    private Map<String, Object> data;
}
