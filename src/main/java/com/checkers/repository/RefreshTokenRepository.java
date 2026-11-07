package com.checkers.repository;

import com.checkers.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

/**
 * Refresh-token persistence for verification, revocation and user-token cleanup.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds the stored refresh-token record for expiry and revocation checks.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes a user's refresh-token records through a Spring Data derived query.
     */
    @Modifying
    void deleteByUserId(Long userId);
}
