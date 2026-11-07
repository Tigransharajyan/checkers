package com.checkers.repository;

import com.checkers.model.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence access to a game's ordered ply log.
 */
public interface MoveRepository extends JpaRepository<Move, Long> {

    /**
     * Loads a game's move log in ascending ply order for replay and draw-history reconstruction.
     */
    List<Move> findByGameIdOrderByMoveNumberAsc(Long gameId);
}
