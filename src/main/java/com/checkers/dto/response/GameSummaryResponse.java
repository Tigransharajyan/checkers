package com.checkers.dto.response;

import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSummaryResponse {

    private Long id;
    private GameMode mode;
    private GameStatus status;
    private String whiteUsername;
    private String blackUsername;
    private String winnerUsername;
    private String finishReason;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;
}
