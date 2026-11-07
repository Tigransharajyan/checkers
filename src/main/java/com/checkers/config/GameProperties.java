package com.checkers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings under app.game for reconnect grace, matchmaking timeout and the minimum move count
 * before a draw offer. Lombok generates accessors.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.game")
public class GameProperties {

    /** Reconnect grace period before technical defeat, in seconds. */
    private long reconnectTimeoutSeconds = 60;

    /** Maximum wait in the single-instance matchmaking queue, in seconds. */
    private long matchmakingTimeoutSeconds = 120;

    /** Minimum full moves before a draw offer; zero allows an agreement immediately. */
    private int drawOfferMinimumMoves = 0;
}
