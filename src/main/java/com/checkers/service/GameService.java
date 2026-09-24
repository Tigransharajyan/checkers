package com.checkers.service;

import com.checkers.dto.request.CreateGameRequest;
import com.checkers.dto.response.GameResponse;
import com.checkers.dto.response.GameSummaryResponse;
import com.checkers.engine.Board;
import com.checkers.engine.CheckersRulesEngine;
import com.checkers.engine.EngineMove;
import com.checkers.engine.GameOutcome;
import com.checkers.engine.MoveResult;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import com.checkers.mapper.DtoMapper;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.GameInvite;
import com.checkers.model.entity.Move;
import com.checkers.model.entity.User;
import com.checkers.model.enums.EventType;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.InviteStatus;
import com.checkers.model.enums.PieceColor;
import com.checkers.repository.GameInviteRepository;
import com.checkers.repository.GameRepository;
import com.checkers.repository.MoveRepository;
import com.checkers.repository.UserRepository;
import com.checkers.security.CustomUserDetailsService;
import com.checkers.security.SecurityUser;
import com.checkers.websocket.dto.GameEventMessage;
import com.checkers.websocket.dto.MoveMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final GameInviteRepository gameInviteRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final InviteService inviteService;
    private final MatchmakingService matchmakingService;
    private final BotService botService;
    private final CheckersRulesEngine rulesEngine;
    private final PresenceService presenceService;

    public GameService(GameRepository gameRepository,
                       MoveRepository moveRepository,
                       GameInviteRepository gameInviteRepository,
                       UserRepository userRepository,
                       UserService userService,
                       InviteService inviteService,
                       MatchmakingService matchmakingService,
                       BotService botService,
                       CheckersRulesEngine rulesEngine,
                       @Lazy PresenceService presenceService) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.gameInviteRepository = gameInviteRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.inviteService = inviteService;
        this.matchmakingService = matchmakingService;
        this.botService = botService;
        this.rulesEngine = rulesEngine;
        this.presenceService = presenceService;
    }

    @Transactional
    public GameResponse createGame(CreateGameRequest request) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User current = userService.requireEntity(principal.getId());

        return switch (request.getMode()) {
            case FRIEND -> createFriendGame(current);
            case BOT -> createBotGame(current, request);
            case MATCHMAKING -> {
                matchmakingService.enqueue(current.getId());
                Game matched = matchmakingService.tryMatch();
                if (matched != null) {
                    yield toResponse(matched);
                }
                yield GameResponse.builder()
                        .mode(GameMode.MATCHMAKING)
                        .status(GameStatus.WAITING)
                        .build();
            }
        };
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long gameId) {
        Game game = requireGame(gameId);
        assertParticipant(game);
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getActiveGames() {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        return gameRepository
                .findActiveByUserId(userId, List.of(GameStatus.WAITING, GameStatus.IN_PROGRESS))
                .stream()
                .map(DtoMapper::toGameSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<GameSummaryResponse> getHistory(Pageable pageable) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        if (principal.isGuest()) {
            return Page.empty(pageable);
        }
        return gameRepository.findHistoryByUserId(principal.getId(), pageable)
                .map(DtoMapper::toGameSummary);
    }

    @Transactional
    public GameResponse joinByInviteCode(String code) {
        return inviteService.acceptInvite(code);
    }

    @Transactional(readOnly = true)
    public List<List<String>> getLegalMovePaths(Long gameId) {
        Game game = requireGame(gameId);
        assertParticipant(game);
        if (game.getStatus() != GameStatus.IN_PROGRESS || game.getBoardState() == null) {
            return List.of();
        }
        Board board = Board.fromJson(game.getBoardState());
        return rulesEngine.legalMoves(board).stream()
                .map(EngineMove::pathAlgebraic)
                .toList();
    }

    @Transactional
    public GameEventMessage applyMove(Long gameId, MoveMessage moveMessage) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User player = userService.requireEntity(principal.getId());
        Game game = requireGame(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw moveException(ErrorCode.INVALID_MOVE, Map.of("moveError", "GAME_OVER"));
        }
        assertParticipant(game);
        PieceColor playerColor = colorOf(game, player.getId());
        if (playerColor == null || playerColor != game.getCurrentTurn()) {
            throw moveException(ErrorCode.INVALID_MOVE, Map.of("moveError", "NOT_YOUR_TURN"));
        }

        Board board = Board.fromJson(game.getBoardState());
        MoveResult result = rulesEngine.applyMove(board, moveMessage.resolvedPath());
        if (!result.success()) {
            throw moveException(ErrorCode.INVALID_MOVE,
                    Map.of("moveError", result.error().name()));
        }

        persistMove(game, player, result.move());
        syncGameFromBoard(game, result.board());
        gameRepository.save(game);

        return buildMoveEvent(game, player, result.move(), false);
    }

    /**
     * Applies bot ply if it is currently the bot's turn. Called asynchronously
     * from {@link BotPlayService} so the STOMP/HTTP thread is not blocked.
     */
    @Transactional
    public GameEventMessage applyBotMoveIfNeeded(Long gameId) {
        Game game = requireGame(gameId);
        if (game.getMode() != GameMode.BOT
                || game.getStatus() != GameStatus.IN_PROGRESS
                || game.getCurrentTurn() != PieceColor.BLACK) {
            return null;
        }
        return applyBotMove(game);
    }

    @Transactional
    public GameEventMessage resign(Long gameId) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User player = userService.requireEntity(principal.getId());
        Game game = requireGame(gameId);
        assertParticipant(game);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }

        User winner = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(player.getId())
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        // vs bot: black is null → human resigns, no human winner
        finishGame(game, winner, "RESIGN");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", player.getId());
        data.put("username", player.getUsername());
        data.put("winnerId", winner != null ? winner.getId() : null);
        data.put("reason", "RESIGN");
        data.put("status", game.getStatus().name());

        return GameEventMessage.builder()
                .type(EventType.GAME_RESIGNED)
                .gameId(game.getId())
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    /**
     * Ends a game from the active-games list. A started game is a resignation;
     * a waiting game is aborted and its pending invite becomes unusable.
     */
    @Transactional
    public GameEventMessage endActiveGame(Long gameId) {
        Game game = requireGame(gameId);
        assertParticipant(game);

        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            return resign(gameId);
        }
        if (game.getStatus() != GameStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }

        gameInviteRepository.findByGameId(gameId)
                .filter(invite -> invite.getStatus() == InviteStatus.PENDING)
                .ifPresent(invite -> {
                    invite.setStatus(InviteStatus.CANCELLED);
                    gameInviteRepository.save(invite);
                });
        game.setStatus(GameStatus.ABORTED);
        game.setFinishReason("CANCELLED");
        game.setFinishedAt(Instant.now());
        game.setCurrentTurn(null);
        gameRepository.save(game);

        return GameEventMessage.builder()
                .type(EventType.GAME_CANCELLED)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(Map.of(
                        "userId", CustomUserDetailsService.requireCurrentUserId(),
                        "status", game.getStatus().name(),
                        "reason", "CANCELLED"
                ))
                .build();
    }

    @Transactional
    public GameEventMessage offerDraw(Long gameId) {
        requireGame(gameId);
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        return GameEventMessage.builder()
                .type(EventType.DRAW_OFFERED)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(Map.of(
                        "userId", principal.getId(),
                        "username", principal.getUsername()
                ))
                .build();
    }

    @Transactional
    public GameEventMessage respondToDraw(Long gameId, boolean accept) {
        Game game = requireGame(gameId);
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        if (accept) {
            finishGame(game, null, "DRAW");
            return GameEventMessage.builder()
                    .type(EventType.DRAW_ACCEPTED)
                    .gameId(gameId)
                    .timestamp(Instant.now())
                    .data(Map.of(
                            "userId", principal.getId(),
                            "reason", "DRAW",
                            "status", game.getStatus().name()
                    ))
                    .build();
        }
        return GameEventMessage.builder()
                .type(EventType.DRAW_DECLINED)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(Map.of("userId", principal.getId()))
                .build();
    }

    @Transactional
    public GameEventMessage presence(Long gameId) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        requireGame(gameId);
        presenceService.markPresent(gameId, principal.getId());
        return GameEventMessage.builder()
                .type(EventType.OPPONENT_RECONNECTED)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(Map.of(
                        "userId", principal.getId(),
                        "username", principal.getUsername()
                ))
                .build();
    }

    /**
     * Awards the win to the opponent after reconnect timeout.
     */
    @Transactional
    public GameEventMessage technicalDefeat(Long gameId, Long disconnectedUserId) {
        Game game = requireGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return null;
        }
        if (!isParticipant(game, disconnectedUserId)) {
            return null;
        }

        User winner = null;
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(disconnectedUserId)) {
            winner = game.getBlackPlayer();
        } else if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(disconnectedUserId)) {
            winner = game.getWhitePlayer();
        }
        finishGame(game, winner, "TECHNICAL_DEFEAT");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", disconnectedUserId);
        data.put("winnerId", winner != null ? winner.getId() : null);
        data.put("reason", "TECHNICAL_DEFEAT");
        data.put("status", game.getStatus().name());
        data.put("boardState", game.getBoardState());

        return GameEventMessage.builder()
                .type(EventType.TECHNICAL_DEFEAT)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    private GameEventMessage applyBotMove(Game game) {
        Board board = Board.fromJson(game.getBoardState());
        EngineMove botMove = botService.chooseMove(board, game.getBotDifficulty());
        if (botMove == null) {
            return null;
        }
        MoveResult result = rulesEngine.applyMove(board, botMove.pathAlgebraic());
        if (!result.success()) {
            return null;
        }
        persistMove(game, null, result.move());
        syncGameFromBoard(game, result.board());
        gameRepository.save(game);
        return buildMoveEvent(game, null, result.move(), true);
    }

    private GameResponse createFriendGame(User current) {
        Board board = rulesEngine.createInitialBoard();
        Game game = Game.builder()
                .mode(GameMode.FRIEND)
                .status(GameStatus.WAITING)
                .whitePlayer(current)
                .currentTurn(PieceColor.WHITE)
                .boardState(board.toJson())
                .build();
        gameRepository.save(game);
        GameInvite invite = inviteService.createInvite(game, current);
        return DtoMapper.toGameResponse(game, invite.getCode(), List.of());
    }

    private GameResponse createBotGame(User current, CreateGameRequest request) {
        if (request.getBotDifficulty() == null) {
            throw new BusinessException(ErrorCode.BOT_DIFFICULTY_REQUIRED);
        }
        Board board = rulesEngine.createInitialBoard();
        Game game = Game.builder()
                .mode(GameMode.BOT)
                .status(GameStatus.IN_PROGRESS)
                .whitePlayer(current)
                .blackPlayer(null)
                .botDifficulty(request.getBotDifficulty())
                .currentTurn(PieceColor.WHITE)
                .boardState(board.toJson())
                .startedAt(Instant.now())
                .build();
        gameRepository.save(game);
        return DtoMapper.toGameResponse(game, null, List.of());
    }

    private void persistMove(Game game, User player, EngineMove engineMove) {
        int nextNumber = moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId()).size() + 1;
        Move move = Move.builder()
                .game(game)
                .moveNumber(nextNumber)
                .fromSquare(engineMove.from().algebraic())
                .toSquare(engineMove.to().algebraic())
                .capturedSquares(engineMove.capturedCsv())
                .path(String.join(",", engineMove.pathAlgebraic()))
                .promoted(engineMove.promotion())
                .player(player)
                .build();
        moveRepository.save(move);
    }

    private void syncGameFromBoard(Game game, Board board) {
        game.setBoardState(board.toJson());
        game.setCurrentTurn(board.turn());
        GameOutcome outcome = board.outcome();
        if (outcome == GameOutcome.IN_PROGRESS) {
            return;
        }
        User winner = null;
        String reason;
        if (outcome == GameOutcome.WHITE_WINS) {
            winner = game.getWhitePlayer();
            reason = "NO_MOVES";
        } else if (outcome == GameOutcome.BLACK_WINS) {
            winner = game.getBlackPlayer();
            reason = "NO_MOVES";
        } else {
            reason = "DRAW";
        }
        finishGame(game, winner, reason);
    }

    private void finishGame(Game game, User winner, String reason) {
        if (game.getStatus() == GameStatus.FINISHED) {
            return;
        }
        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishReason(reason);
        game.setFinishedAt(Instant.now());
        game.setCurrentTurn(null);
        gameRepository.save(game);
        updateStats(game, winner, reason);
    }

    private void updateStats(Game game, User winner, String reason) {
        User white = game.getWhitePlayer();
        User black = game.getBlackPlayer();

        if ("DRAW".equals(reason)) {
            bumpDraw(white);
            bumpDraw(black);
            return;
        }
        if (winner != null) {
            bumpWin(winner);
            if (white != null && !white.getId().equals(winner.getId())) {
                bumpLoss(white);
            }
            if (black != null && !black.getId().equals(winner.getId())) {
                bumpLoss(black);
            }
            // vs bot: human lost
            if (black == null && white != null && !white.getId().equals(winner.getId())) {
                bumpLoss(white);
            }
            // vs bot: human won (winner is white, black null) — already bumpWin(white)
            return;
        }
        // resign vs bot: winner null → human lost
        if (game.getMode() == GameMode.BOT && white != null) {
            bumpLoss(white);
        }
    }

    private void bumpWin(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setWins(user.getWins() + 1);
        userRepository.save(user);
    }

    private void bumpLoss(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setLosses(user.getLosses() + 1);
        userRepository.save(user);
    }

    private void bumpDraw(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setDraws(user.getDraws() + 1);
        userRepository.save(user);
    }

    private GameEventMessage buildMoveEvent(Game game, User player, EngineMove move, boolean bot) {
        Map<String, Object> data = new HashMap<>();
        data.put("fromSquare", move.from().algebraic());
        data.put("toSquare", move.to().algebraic());
        data.put("path", move.pathAlgebraic());
        data.put("capturedSquares", move.capturedCsv());
        data.put("promoted", move.promotion());
        data.put("playerId", player != null ? player.getId() : null);
        data.put("username", player != null ? player.getUsername() : "BOT");
        data.put("bot", bot);
        data.put("boardState", game.getBoardState());
        data.put("currentTurn", game.getCurrentTurn() != null ? game.getCurrentTurn().name() : null);
        data.put("status", game.getStatus().name());
        data.put("finishReason", game.getFinishReason());
        if (game.getStatus() == GameStatus.FINISHED) {
            data.put("winnerId", game.getWinner() != null ? game.getWinner().getId() : null);
            data.put("reason", game.getFinishReason());
        }

        EventType type = game.getStatus() == GameStatus.FINISHED
                ? EventType.GAME_FINISHED
                : EventType.MOVE_MADE;

        return GameEventMessage.builder()
                .type(type)
                .gameId(game.getId())
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    private Game requireGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GAME_NOT_FOUND));
    }

    private void assertParticipant(Game game) {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        if (!isParticipant(game, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean isParticipant(Game game, Long userId) {
        boolean white = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId);
        boolean black = game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId);
        return white || black;
    }

    private PieceColor colorOf(Game game, Long userId) {
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId)) {
            return PieceColor.WHITE;
        }
        if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId)) {
            return PieceColor.BLACK;
        }
        return null;
    }

    private GameResponse toResponse(Game game) {
        String inviteCode = gameInviteRepository.findByGameId(game.getId())
                .filter(i -> i.getStatus() == InviteStatus.PENDING)
                .map(GameInvite::getCode)
                .orElseGet(() -> gameInviteRepository.findByGameId(game.getId())
                        .map(GameInvite::getCode)
                        .orElse(null));
        List<Move> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId());
        return DtoMapper.toGameResponse(game, inviteCode, moves);
    }

    private BusinessException moveException(ErrorCode code, Map<String, Object> details) {
        BusinessException ex = new BusinessException(code);
        ex.setDetails(details);
        return ex;
    }
}
