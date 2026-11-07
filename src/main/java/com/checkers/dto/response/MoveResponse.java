package com.checkers.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One complete ply, including intermediate jumps, captures, promotion, author and time. Lombok
 * generates accessors and a builder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveResponse {

    private Long id;
    private Integer moveNumber;
    private String fromSquare;
    private String toSquare;
    private String capturedSquares;
    private String path;
    private boolean promoted;
    private Long playerId;
    private Instant createdAt;
}
