package com.checkers.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Draw command: null means offer, true means acceptance and false means rejection. Lombok generates
 * accessors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawOfferMessage {

    /** null offers a draw; true accepts the other side's offer; false declines it. */
    private Boolean accept;
}
