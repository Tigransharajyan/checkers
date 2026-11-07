package com.checkers.repository;

import com.checkers.model.entity.GameInvite;
import com.checkers.model.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data invitation repository. Spring generates derived lookup implementations from method
 * names.
 */
public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

    /**
     * Finds an invitation by its unique code. Optional explicitly represents a missing record.
     */
    Optional<GameInvite> findByCode(String code);

    /**
     * Finds an invitation matching both the supplied code and required status.
     */
    Optional<GameInvite> findByCodeAndStatus(String code, InviteStatus status);

    /**
     * Returns the invitation associated with the supplied game.
     */
    Optional<GameInvite> findByGameId(Long gameId);
}
