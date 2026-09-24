package com.checkers.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret = "change-me-to-a-long-enough-secret-key-for-hs256-checkers";
    private long accessTokenTtlMinutes = 30;
    private long refreshTokenTtlDays = 3;
}
