package com.checkers.model.entity;

import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.PieceColor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Game entity in the games table containing participants, status, board JSON, persisted draw
 * history and result. A bot has no separate Black User record. Lombok generates constructors,
 * accessors and a builder.
 */
@Entity
@Table(
        name = "games",
        indexes = {
                @Index(name = "idx_games_status", columnList = "status"),
                @Index(name = "idx_games_mode", columnList = "mode"),
                @Index(name = "idx_games_white_player", columnList = "white_player_id"),
                @Index(name = "idx_games_black_player", columnList = "black_player_id"),
                @Index(name = "idx_games_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_difficulty", length = 20)
    private BotDifficulty botDifficulty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_turn", length = 10)
    private PieceColor currentTurn;

    /**
     * Current Board JSON snapshot containing turn, outcome and piece placement; updated by the
     * engine.
     */
    @Column(name = "board_state", columnDefinition = "TEXT")
    private String boardState;

    @Column(name = "draw_state", columnDefinition = "TEXT")
    private String drawState;

    /** Draw-offer author: null means no offer, -1 means bot, otherwise the proposing user's id. */
    @Column(name = "draw_offer_by_id")
    private Long drawOfferById;

    /** Machine-readable finish reason: resignation, draw, no legal moves or technical defeat. */
    @Column(name = "finish_reason", length = 40)
    private String finishReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Sets creation time before the first database insert and supplies WAITING when no status was
     * explicitly assigned.
     */
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = GameStatus.WAITING;
        }
    }
}
