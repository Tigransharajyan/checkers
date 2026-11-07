package com.checkers.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Stored opaque refresh token with user, expiry and revocation state. Separate persistence supports
 * rotation and revocation. Lombok generates accessors.
 */
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_tokens_token", columnNames = "token")
        },
        indexes = {
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Sets server creation time before the refresh-token record is inserted.
     */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Compares the token expiry time with the current instant and reports whether it has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
