package com.checkers.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registered or guest account with language and statistics. Registered passwords use BCrypt hashes;
 * guests have no email or password. Lombok generates accessors and a builder.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_guest", columnList = "guest")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @Size(min = 3, max = 50)
    private String username;

    /** Absent for guest accounts because password-based login is not available to guests. */
    @Column(length = 255)
    private String email;

    /** Absent for guest accounts because password-based login is not available to guests. */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private boolean guest = false;

    /**
     * Registered-user interface language: en, ru or hy. Always null for guests, whose preference
     * lives in the browser cookie.
     */
    @Column(name = "preferred_locale", length = 2)
    @Size(min = 2, max = 2)
    private String preferredLocale;

    @Column(nullable = false)
    @Builder.Default
    private int wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private int losses = 0;

    @Column(nullable = false)
    @Builder.Default
    private int draws = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Initializes creation and modification timestamps before inserting a user.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Updates the modification timestamp when an existing user record is saved.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
