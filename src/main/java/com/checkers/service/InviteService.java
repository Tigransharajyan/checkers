package com.checkers.service;

import com.checkers.dto.response.GameResponse;
import com.checkers.engine.CheckersRulesEngine;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.mapper.DtoMapper;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.GameInvite;
import com.checkers.model.entity.User;
import com.checkers.model.enums.EventType;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.InviteStatus;
import com.checkers.model.enums.PieceColor;
import com.checkers.repository.GameInviteRepository;
import com.checkers.repository.GameRepository;
import com.checkers.repository.MoveRepository;
import com.checkers.security.CustomUserDetailsService;
import com.checkers.websocket.GameEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameInviteRepository gameInviteRepository;
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserService userService;
    private final GameEventPublisher eventPublisher;
    private final CheckersRulesEngine rulesEngine = new CheckersRulesEngine();

    @Transactional
    public GameInvite createInvite(Game game, User creator) {
        GameInvite invite = GameInvite.builder()
                .code(generateUniqueCode())
                .game(game)
                .creator(creator)
                .status(InviteStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        return gameInviteRepository.save(invite);
    }

    @Transactional
    public GameResponse acceptInvite(String code) {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        User joiner = userService.requireEntity(userId);

        GameInvite invite = gameInviteRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE));

        if (invite.getStatus() == InviteStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.INVITE_ALREADY_USED);
        }
        if (invite.getStatus() == InviteStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_INVITE);
        }
        if (invite.getExpiresAt().isBefore(Instant.now()) || invite.getStatus() == InviteStatus.EXPIRED) {
            invite.setStatus(InviteStatus.EXPIRED);
            gameInviteRepository.save(invite);
            throw new BusinessException(ErrorCode.INVITE_EXPIRED);
        }
        if (invite.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.GAME_NOT_JOINABLE);
        }

        Game game = invite.getGame();
        if (game.getStatus() == GameStatus.IN_PROGRESS
                || game.getStatus() == GameStatus.FINISHED
                || game.getStatus() == GameStatus.ABORTED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_STARTED);
        }
        if (game.getStatus() != GameStatus.WAITING || game.getBlackPlayer() != null) {
            throw new BusinessException(ErrorCode.GAME_NOT_JOINABLE);
        }

        game.setBlackPlayer(joiner);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(Instant.now());
        if (game.getBoardState() == null) {
            game.setBoardState(rulesEngine.createInitialBoard().toJson());
            game.setCurrentTurn(PieceColor.WHITE);
        }
        gameRepository.save(game);

        invite.setStatus(InviteStatus.ACCEPTED);
        gameInviteRepository.save(invite);

        Map<String, Object> data = new HashMap<>();
        data.put("gameId", game.getId());
        data.put("inviteCode", invite.getCode());
        data.put("joinerId", joiner.getId());
        data.put("joinerUsername", joiner.getUsername());
        data.put("invitePath", "/play?code=" + invite.getCode());
        eventPublisher.publishToGame(game.getId(), EventType.INVITE_ACCEPTED, data);
        eventPublisher.publishToGame(game.getId(), EventType.GAME_STARTED, Map.of(
                "gameId", game.getId(),
                "whitePlayerId", game.getWhitePlayer().getId(),
                "blackPlayerId", joiner.getId()
        ));

        return DtoMapper.toGameResponse(game, invite.getCode(),
                moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId()));
    }

    @Transactional
    public void cancelInvite(String code) {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        GameInvite invite = gameInviteRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE));
        if (!invite.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        invite.setStatus(InviteStatus.CANCELLED);
        gameInviteRepository.save(invite);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (gameInviteRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Cannot generate unique invite code");
    }
}
