package com.checkers.security.jwt;

import com.checkers.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Creates and validates access JWTs and generates opaque refresh values. Access tokens are verified
 * cryptographically; refresh tokens through their database records.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    /**
     * Creates a signed JWT containing the user id, username, guest flag, access type and expiry.
     * The signature protects claims against modification but does not encrypt their contents.
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtlMinutes() * 60);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("guest", user.isGuest())
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Creates an opaque random refresh-token value from two UUIDs. It is not a JWT; validity is
     * checked through its database record.
     */
    public String createRefreshTokenValue() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    /**
     * Calculates the new refresh token's expiry from the configured number of days.
     */
    public Instant refreshExpiry() {
        return Instant.now().plusSeconds(properties.getRefreshTokenTtlDays() * 24 * 3600);
    }

    /**
     * Verifies the JWT signature and expiry and returns its claims. An invalid token raises the JWT
     * library's exception.
     */
    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user identifier from the subject after parsing and validating the access token.
     */
    public Long extractUserId(String accessToken) {
        return Long.valueOf(parseAccessToken(accessToken).getSubject());
    }

    /**
     * Checks signature, expiry and typ=access without propagating validation exceptions. Used when
     * establishing an authenticated STOMP connection.
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseAccessToken(token);
            return "access".equals(claims.get("typ", String.class));
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Builds the HMAC key from the configured UTF-8 server secret. The same secret signs and
     * verifies tokens.
     */
    private SecretKey signingKey() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
