package com.checkers.repository;

import com.checkers.model.entity.GameInvite;
import com.checkers.model.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

    Optional<GameInvite> findByCode(String code);

    Optional<GameInvite> findByCodeAndStatus(String code, InviteStatus status);

    Optional<GameInvite> findByGameId(Long gameId);
}
