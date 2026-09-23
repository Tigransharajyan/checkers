package com.checkers.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawOfferMessage {

    /**
     * null → offer draw; true → accept; false → decline.
     */
    private Boolean accept;
}
