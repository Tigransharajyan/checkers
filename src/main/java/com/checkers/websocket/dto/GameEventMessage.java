package com.checkers.websocket.dto;

import com.checkers.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Real-time event envelope with type, game id, timestamp and machine-readable data. Localization
 * belongs to the client. Lombok generates accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEventMessage {

    private EventType type;
    private Long gameId;
    private Instant timestamp;
    /**
     * Event parameters such as names, squares and ids; localized wording is chosen by the
     * recipient's browser.
     */
    private Map<String, Object> data;
}
