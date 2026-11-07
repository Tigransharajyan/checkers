package com.checkers.repository;

import com.checkers.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Account lookups by username/email and uniqueness checks used during registration.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds an account by its username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds an account using the repository query's ordinary email equality comparison.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds an account matching either the supplied username or email.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Looks up the login identifier as a username or case-insensitive email address.
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR LOWER(u.email) = LOWER(:identifier)")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    /**
     * Checks whether a username is already taken before registration.
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a record exists with the supplied email.
     */
    boolean existsByEmail(String email);

    /**
     * Checks email availability without case sensitivity so letter-case variants cannot create
     * duplicate registrations.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
}

