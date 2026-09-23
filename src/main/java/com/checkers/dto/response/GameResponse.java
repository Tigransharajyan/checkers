package com.checkers.dto.response;

import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.PieceColor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResponse {

    private Long id;
    private GameMode mode;
    private GameStatus status;
    private UserResponse whitePlayer;
    private UserResponse blackPlayer;
    private BotDifficulty botDifficulty;
    private Long winnerId;
    private PieceColor currentTurn;
    private String boardState;
    private String inviteCode;
    /** Relative invite link hint, e.g. {@code /play?code=ABCD}. */
    private String invitePath;
    private String finishReason;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;
    private List<MoveResponse> moves;
}
