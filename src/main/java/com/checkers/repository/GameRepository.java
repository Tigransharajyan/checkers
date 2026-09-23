package com.checkers.repository;

import com.checkers.model.entity.Game;
import com.checkers.model.enums.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("""
            SELECT g FROM Game g
            WHERE (g.whitePlayer.id = :userId OR g.blackPlayer.id = :userId)
              AND g.status IN :statuses
            ORDER BY g.createdAt DESC
            """)
    List<Game> findActiveByUserId(@Param("userId") Long userId,
                                  @Param("statuses") List<GameStatus> statuses);

    @Query("""
            SELECT g FROM Game g
            WHERE (g.whitePlayer.id = :userId OR g.blackPlayer.id = :userId)
              AND g.status = com.checkers.model.enums.GameStatus.FINISHED
            ORDER BY g.finishedAt DESC
            """)
    Page<Game> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);
}
