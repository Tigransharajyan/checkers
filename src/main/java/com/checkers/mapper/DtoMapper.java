package com.checkers.mapper;

import com.checkers.dto.response.GameResponse;
import com.checkers.dto.response.GameSummaryResponse;
import com.checkers.dto.response.MoveResponse;
import com.checkers.dto.response.UserResponse;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.Move;
import com.checkers.model.entity.User;

import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .preferredLocale(user.getPreferredLocale())
                .guest(user.isGuest())
                .wins(user.getWins())
                .losses(user.getLosses())
                .draws(user.getDraws())
                .build();
    }

    public static MoveResponse toMoveResponse(Move move) {
        return MoveResponse.builder()
                .id(move.getId())
                .moveNumber(move.getMoveNumber())
                .fromSquare(move.getFromSquare())
                .toSquare(move.getToSquare())
                .capturedSquares(move.getCapturedSquares())
                .path(move.getPath())
                .promoted(move.isPromoted())
                .playerId(move.getPlayer() != null ? move.getPlayer().getId() : null)
                .createdAt(move.getCreatedAt())
                .build();
    }

    public static GameSummaryResponse toGameSummary(Game game) {
        return GameSummaryResponse.builder()
                .id(game.getId())
                .mode(game.getMode())
                .status(game.getStatus())
                .whiteUsername(game.getWhitePlayer() != null ? game.getWhitePlayer().getUsername() : null)
                .blackUsername(game.getBlackPlayer() != null ? game.getBlackPlayer().getUsername() : "BOT")
                .winnerUsername(game.getWinner() != null ? game.getWinner().getUsername() : null)
                .finishReason(game.getFinishReason())
                .startedAt(game.getStartedAt())
                .finishedAt(game.getFinishedAt())
                .createdAt(game.getCreatedAt())
                .build();
    }

    public static GameResponse toGameResponse(Game game, String inviteCode, List<Move> moves) {
        return GameResponse.builder()
                .id(game.getId())
                .mode(game.getMode())
                .status(game.getStatus())
                .whitePlayer(toUserResponse(game.getWhitePlayer()))
                .blackPlayer(toUserResponse(game.getBlackPlayer()))
                .botDifficulty(game.getBotDifficulty())
                .winnerId(game.getWinner() != null ? game.getWinner().getId() : null)
                .currentTurn(game.getCurrentTurn())
                .boardState(game.getBoardState())
                .inviteCode(inviteCode)
                .invitePath(inviteCode != null ? "/play?code=" + inviteCode : null)
                .finishReason(game.getFinishReason())
                .startedAt(game.getStartedAt())
                .finishedAt(game.getFinishedAt())
                .createdAt(game.getCreatedAt())
                .moves(moves == null ? List.of() : moves.stream().map(DtoMapper::toMoveResponse).toList())
                .build();
    }
}
