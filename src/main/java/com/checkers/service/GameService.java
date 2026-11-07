package com.checkers.service;

import com.checkers.dto.request.CreateGameRequest;
import com.checkers.dto.response.GameResponse;
import com.checkers.dto.response.GameSummaryResponse;
import com.checkers.engine.Board;
import com.checkers.engine.DrawState;
import com.checkers.config.GameProperties;
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

/**
 * Transactional game lifecycle coordinator. Checks participants and turn ownership, invokes the
 * shared engine, persists moves and draw counters, finishes games and updates statistics.
 */
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
    private final GameProperties gameProperties;

    /**
     * Receives repositories, the rules engine and collaborating services through constructor
     * injection. Lazy presence injection resolves mutual service dependencies.
     */
    public GameService(GameRepository gameRepository,
                       MoveRepository moveRepository,
                       GameInviteRepository gameInviteRepository,
                       UserRepository userRepository,
                       UserService userService,
                       InviteService inviteService,
                       MatchmakingService matchmakingService,
                       BotService botService,
                       CheckersRulesEngine rulesEngine,
                       @Lazy PresenceService presenceService,
                       GameProperties gameProperties) {
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
        this.gameProperties = gameProperties;
    }

    /**
     * Dispatches creation to a friend invitation, bot game or matchmaking entry according to the
     * requested mode. Creation changes run within a transaction.
     */
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

    /**
     * Loads the game, verifies the current user's participation and builds its complete response
     * with ordered move history.
     */
    @Transactional(readOnly = true)
    public GameResponse getGame(Long gameId) {
        Game game = requireGame(gameId);
        assertParticipant(game);
        return toResponse(game);
    }

    /**
     * Returns compact DTOs for waiting and ongoing games that the user can resume from the lobby.
     */
    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getActiveGames() {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        return gameRepository
                .findActiveByUserId(userId, List.of(GameStatus.WAITING, GameStatus.IN_PROGRESS))
                .stream()
                .map(DtoMapper::toGameSummary)
                .toList();
    }

    /**
     * Returns a page of completed games for a registered user. Intentionally returns no history for
     * a guest.
     */
    @Transactional(readOnly = true)
    public Page<GameSummaryResponse> getHistory(Pageable pageable) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        if (principal.isGuest()) {
            return Page.empty(pageable);
        }
        return gameRepository.findHistoryByUserId(principal.getId(), pageable)
                .map(DtoMapper::toGameSummary);
    }

    /**
     * Delegates joining to InviteService, which checks invitation expiry, status and the available
     * Black seat.
     */
    @Transactional
    public GameResponse joinByInviteCode(String code) {
        return inviteService.acceptInvite(code);
    }

    /**
     * Checks access and returns complete legal paths from the server-side engine. Returns an empty
     * list when the game is not in progress.
     */
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

    /**
     * Locks the game row, checks status, participation and turn ownership, then validates the full
     * path. Persists the move and draw history, updates the outcome and builds the event atomically
     * within the transaction.
     */
    @Transactional
    public GameEventMessage applyMove(Long gameId, MoveMessage moveMessage) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User player = userService.requireEntity(principal.getId());
        // A row lock serializes concurrent changes to this game until the transaction completes.
        Game game = requireGameForUpdate(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw moveException(ErrorCode.INVALID_MOVE, Map.of("moveError", "GAME_OVER"));
        }
        assertParticipant(game);
        PieceColor playerColor = colorOf(game, player.getId());
        if (playerColor == null || playerColor != game.getCurrentTurn()) {
            throw moveException(ErrorCode.INVALID_MOVE, Map.of("moveError", "NOT_YOUR_TURN"));
        }

        // Validate against the persisted server position, not the browser's displayed arrangement.
        Board board = Board.fromJson(game.getBoardState());
        MoveResult result = rulesEngine.applyMove(board, moveMessage.resolvedPath());
        if (!result.success()) {
            throw moveException(ErrorCode.INVALID_MOVE,
                    Map.of("moveError", result.error().name()));
        }

        if (game.getDrawOfferById() != null && !game.getDrawOfferById().equals(player.getId())) game.setDrawOfferById(null);
        // Update draw counters and move history in the same transaction as the game position.
        recordDrawState(game, board, result);
        persistMove(game, player, result.move());
        gameRepository.save(game);

        return buildMoveEvent(game, player, result.move(), false);
    }

    /**
     * Rechecks under the row lock that the bot game is active and Black is to move. An obsolete
     * background task performs no action.
     */
    @Transactional
    public GameEventMessage applyBotMoveIfNeeded(Long gameId) {
        Game game = requireGameForUpdate(gameId);
        if (game.getMode() != GameMode.BOT
                || game.getStatus() != GameStatus.IN_PROGRESS
                || game.getCurrentTurn() != PieceColor.BLACK) {
            return null;
        }
        return applyBotMove(game);
    }

    /**
     * Ends an active game by the current participant's resignation and assigns the opponent as
     * winner. Statistics handle bot games separately because Black has no User record.
     */
    @Transactional
    public GameEventMessage resign(Long gameId) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        User player = userService.requireEntity(principal.getId());
        Game game = requireGameForUpdate(gameId);
        assertParticipant(game);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }

        User winner = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(player.getId())
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        // The bot has no User record: a human resignation leaves winner null; statistics also use
        // the reason.
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
     * Handles ending a game from the active list. Resigns a started game or cancels a waiting game
     * together with its invitation.
     */
    @Transactional
    public GameEventMessage endActiveGame(Long gameId) {
        Game game = requireGameForUpdate(gameId);
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

    /**
     * Checks configured timing restrictions and records the participant's offer. Asks the bot for
     * consent in bot games; an opposing offer already pending is treated as mutual agreement.
     */
    @Transactional
    public GameEventMessage offerDraw(Long gameId) {
        Game game = requireGameForUpdate(gameId);
        assertParticipant(game);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        // Each log entry is one ply, while the regulation counts full moves.
        int plies = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId).size();
        if (plies / 2 < gameProperties.getDrawOfferMinimumMoves()) throw new BusinessException(ErrorCode.DRAW_TOO_EARLY);
        if (game.getDrawOfferById() != null) {
            if (!game.getDrawOfferById().equals(principal.getId())) return respondToDraw(gameId, true);
            throw new BusinessException(ErrorCode.DRAW_OFFER_PENDING);
        }
        if (game.getMode() == GameMode.BOT) {
            // The human has already offered; the bot's positive response establishes mutual
            // agreement.
            boolean accepts = botService.considersDraw(Board.fromJson(game.getBoardState()));
            if (accepts) finishGame(game, null, "DRAW");
            return GameEventMessage.builder().type(accepts ? EventType.DRAW_ACCEPTED : EventType.DRAW_DECLINED)
                    .gameId(gameId).timestamp(Instant.now()).data(Map.of("userId", -1L, "status", game.getStatus().name())).build();
        }
        game.setDrawOfferById(principal.getId());
        gameRepository.save(game);
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

    /**
     * Accepts or declines an existing offer made by the other participant. Acceptance finishes the
     * game; rejection clears the offer and preserves active play.
     */
    @Transactional
    public GameEventMessage respondToDraw(Long gameId, boolean accept) {
        Game game = requireGameForUpdate(gameId);
        assertParticipant(game);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_MOVE);
        }
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        if (game.getDrawOfferById() == null || game.getDrawOfferById().equals(principal.getId())) {
            throw new BusinessException(ErrorCode.DRAW_OFFER_NOT_FOUND);
        }
        game.setDrawOfferById(null);
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
        gameRepository.save(game);
        return GameEventMessage.builder()
                .type(EventType.DRAW_DECLINED)
                .gameId(gameId)
                .timestamp(Instant.now())
                .data(Map.of("userId", principal.getId()))
                .build();
    }

    /**
     * Marks the participant as present again and builds a reconnection event when needed.
     */
    @Transactional
    public GameEventMessage presence(Long gameId) {
        SecurityUser principal = CustomUserDetailsService.requireCurrent();
        requireGame(gameId);
        boolean wasDisconnected = presenceService.markPresent(gameId, principal.getId());
        if (!wasDisconnected) {
            return null;
        }
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
     * Checks that the game is still active and the disconnected account is a participant after the
     * grace period. Assigns technical defeat and builds the final event.
     */
    @Transactional
    public GameEventMessage technicalDefeat(Long gameId, Long disconnectedUserId) {
        Game game = requireGameForUpdate(gameId);
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

    /**
     * Selects and validates the bot's move, records draw counters and persists the ply with
     * player=null. Returns the same event shape used for human moves.
     */
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
        if (game.getDrawOfferById() != null && game.getDrawOfferById() != -1L) game.setDrawOfferById(null);
        recordDrawState(game, board, result);
        persistMove(game, null, result.move());
        gameRepository.save(game);
        return buildMoveEvent(game, null, result.move(), true);
    }

    /**
     * Persists a WAITING game in the initial position and creates an invitation through which
     * another player takes Black.
     */
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

    /**
     * Creates an active game with the current user as White and the bot as Black without a separate
     * User record. Requires a bot difficulty.
     */
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

    /**
     * Assigns the next ply number and persists the complete path, captured squares, promotion and
     * author. A multi-capture chain is stored as one move.
     */
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

    /**
     * Loads persisted draw counters or reconstructs an older game's history by replaying its move
     * log. Starts from the known current position when replay does not match it.
     */
    private DrawState drawHistory(Game game, Board current) {
        if (game.getDrawState() != null) return DrawState.fromJson(game.getDrawState());
        // Upgrade older games by replaying their durable log when no DrawState has been stored yet.
        Board replay = Board.initial();
        DrawState history = DrawState.start(replay);
        for (Move move : moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId())) {
            List<String> path = move.getPath() == null ? List.of(move.getFromSquare(), move.getToSquare())
                    : List.of(move.getPath().split(","));
            MoveResult result = rulesEngine.applyMove(replay, path);
            if (!result.success()) return DrawState.start(current);
            history.record(replay, result.move(), result.board());
            replay = result.board();
        }
        return DrawState.positionKey(replay).equals(DrawState.positionKey(current)) ? history : DrawState.start(current);
    }

    /**
     * Records the confirmed move, checks objective draw rules and synchronizes the game with its
     * board. A stable bot ending may also create a draw offer subject to the offer interval.
     */
    private void recordDrawState(Game game, Board before, MoveResult result) {
        DrawState state = drawHistory(game, before);
        state.record(before, result.move(), result.board());
        String drawReason = state.reason(result.board(), rulesEngine);
        // Only an objective rule can set an automatic draw outcome.
        if (drawReason != null) result.board().setOutcome(GameOutcome.DRAW);
        game.setDrawState(state.toJson());
        syncGameFromBoard(game, result.board(), drawReason);
        if (game.getStatus() == GameStatus.IN_PROGRESS && game.getMode() == GameMode.BOT
                && before.turn() == PieceColor.BLACK && state.mayOfferBotDraw()
                && botService.considersDraw(result.board())) {
            // The -1 author denotes the bot. Play remains active until the human agrees.
            game.setDrawOfferById(-1L);
            state.setLastBotOfferPly(state.getPlies());
            game.setDrawState(state.toJson());
        }
    }

    /**
     * Copies board and turn into Game. If the engine or draw history establishes an outcome,
     * selects winner/reason and calls the shared finishing method.
     */
    private void syncGameFromBoard(Game game, Board board, String drawReason) {
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
            reason = drawReason == null ? "DRAW" : drawReason;
        }
        finishGame(game, winner, reason);
    }

    /**
     * Sets FINISHED, winner, reason and finish time once, then clears turn and draw offer. An
     * already finished game does not update statistics again.
     */
    private void finishGame(Game game, User winner, String reason) {
        // A repeated completion event must not count the same game's result twice.
        if (game.getStatus() == GameStatus.FINISHED) {
            return;
        }
        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishReason(reason);
        game.setFinishedAt(Instant.now());
        game.setCurrentTurn(null);
        game.setDrawOfferById(null);
        gameRepository.save(game);
        updateStats(game, winner, reason);
    }

    /**
     * Updates user result counters. All DRAW_* reasons count as draws, and the missing Black
     * account in bot mode must not turn a draw into a human loss.
     */
    private void updateStats(Game game, User winner, String reason) {
        User white = game.getWhitePlayer();
        User black = game.getBlackPlayer();

        if (DrawState.isDrawReason(reason)) {
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
            // Additional handling for the representation of an opponent without a User record.
            if (black == null && white != null && !white.getId().equals(winner.getId())) {
                bumpLoss(white);
            }
            // The human's victory over a bot was already recorded by bumpWin above.
            return;
        }
        // A bot win has no User winner; after the separate draw check, record a human loss.
        if (game.getMode() == GameMode.BOT && white != null) {
            bumpLoss(white);
        }
    }

    /**
     * Increments and persists wins for a registered user. Does nothing for a missing user or guest.
     */
    private void bumpWin(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setWins(user.getWins() + 1);
        userRepository.save(user);
    }

    /**
     * Increments losses only for a registered account; guest statistics are not accumulated.
     */
    private void bumpLoss(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setLosses(user.getLosses() + 1);
        userRepository.save(user);
    }

    /**
     * Increments draws for a registered account and persists the user.
     */
    private void bumpDraw(User user) {
        if (user == null || user.isGuest()) {
            return;
        }
        user.setDraws(user.getDraws() + 1);
        userRepository.save(user);
    }

    /**
     * Builds the shared event with path, board, turn and draw offer. Includes the result and uses
     * GAME_FINISHED instead of MOVE_MADE for completed games.
     */
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
        data.put("drawOfferById", game.getDrawOfferById());
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

    /**
     * Loads a game without a write lock or raises GAME_NOT_FOUND.
     */
    private Game requireGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GAME_NOT_FOUND));
    }

    /**
     * Loads the game row with a database write lock for safe mutation within a transaction.
     */
    private Game requireGameForUpdate(Long gameId) {
        return gameRepository.findForUpdate(gameId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GAME_NOT_FOUND));
    }

    /**
     * Rejects access unless the current user is the game's White or Black participant.
     */
    private void assertParticipant(Game game) {
        Long userId = CustomUserDetailsService.requireCurrentUserId();
        if (!isParticipant(game, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * Matches the user id against either participant, allowing for the bot's absent Black account.
     */
    private boolean isParticipant(Game game, Long userId) {
        boolean white = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId);
        boolean black = game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId);
        return white || black;
    }

    /**
     * Returns the participant's color by user id, or null for a nonparticipant.
     */
    private PieceColor colorOf(Game game, Long userId) {
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(userId)) {
            return PieceColor.WHITE;
        }
        if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(userId)) {
            return PieceColor.BLACK;
        }
        return null;
    }

    /**
     * Loads the associated invitation and ordered move history and passes them to the full-game DTO
     * mapper.
     */
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

    /**
     * Creates a domain failure with structured details such as moveError for a localized client
     * message.
     */
    private BusinessException moveException(ErrorCode code, Map<String, Object> details) {
        BusinessException ex = new BusinessException(code);
        ex.setDetails(details);
        return ex;
    }
}
