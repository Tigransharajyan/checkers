package com.checkers.repository;

import com.checkers.model.entity.Game;
import com.checkers.model.enums.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Game persistence queries for lists, history, statistics and row-locked updates.
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Loads a game with a pessimistic row lock held until the transaction ends. Concurrent moves,
     * resignation and draw agreement must observe one current game state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Game g WHERE g.id = :id")
    Optional<Game> findForUpdate(@Param("id") Long id);

    /**
     * Counts games with the supplied status directly in the database.
     */
    long countByStatus(GameStatus status);

    /**
     * Finds games where the user plays either color and the status belongs to the supplied set.
     * Returns newer games first.
     */
    @Query("""
            SELECT g FROM Game g
            WHERE (g.whitePlayer.id = :userId OR g.blackPlayer.id = :userId)
              AND g.status IN :statuses
            ORDER BY g.createdAt DESC
            """)
    List<Game> findActiveByUserId(@Param("userId") Long userId,
                                  @Param("statuses") List<GameStatus> statuses);

    /**
     * Returns a page of the user's completed games ordered by descending finish time.
     */
    @Query("""
            SELECT g FROM Game g
            WHERE (g.whitePlayer.id = :userId OR g.blackPlayer.id = :userId)
              AND g.status = com.checkers.model.enums.GameStatus.FINISHED
            ORDER BY g.finishedAt DESC
            """)
    Page<Game> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);
}
