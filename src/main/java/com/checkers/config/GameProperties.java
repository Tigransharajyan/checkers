package com.checkers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.game")
public class GameProperties {

    /** Reconnect grace period before technical defeat (seconds). */
    private long reconnectTimeoutSeconds = 60;

    /** In-memory matchmaking wait timeout (seconds). For multi-instance deploy, use Redis. */
    private long matchmakingTimeoutSeconds = 120;
}
