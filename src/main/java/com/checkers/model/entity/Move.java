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

@Entity
@Table(
        name = "moves",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_moves_game_move_number", columnNames = {"game_id", "move_number"})
        },
        indexes = {
                @Index(name = "idx_moves_game_id", columnList = "game_id"),
                @Index(name = "idx_moves_player_id", columnList = "player_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "move_number", nullable = false)
    private Integer moveNumber;

    @Column(name = "from_square", nullable = false, length = 8)
    private String fromSquare;

    @Column(name = "to_square", nullable = false, length = 8)
    private String toSquare;

    /** Comma-separated captured squares, if any (e.g. "c3,e5"). */
    @Column(name = "captured_squares", length = 64)
    private String capturedSquares;

    /** Full ply path for replay (e.g. "b2,d4,f6"). */
    @Column(name = "path", length = 255)
    private String path;

    @Column(nullable = false)
    @Builder.Default
    private boolean promoted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private User player;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
