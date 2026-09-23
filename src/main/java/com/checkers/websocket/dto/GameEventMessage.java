package com.checkers.websocket.dto;

import com.checkers.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Generic event envelope. Frontend resolves {@link EventType} to localized text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEventMessage {

    private EventType type;
    private Long gameId;
    private Instant timestamp;
    /** Substitution data only (usernames, squares, ids) — never localized copy. */
    private Map<String, Object> data;
}
