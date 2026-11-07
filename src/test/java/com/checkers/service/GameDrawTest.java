package com.checkers.service;

import com.checkers.config.GameProperties;
import com.checkers.engine.*;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.Move;
import com.checkers.model.entity.User;
import com.checkers.model.enums.*;
import com.checkers.repository.*;
import com.checkers.security.SecurityUser;
import com.checkers.websocket.dto.GameEventMessage;
import com.checkers.websocket.dto.MoveMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit checks for GameDraw. Each test specifies a behavior or regression boundary; helper methods
 * build the required test data.
 */
class GameDrawTest {
    private GameRepository games;
    private MoveRepository moves;
    private BotService bot;
    private GameService service;
    private GameProperties properties;
    private Game game;
    private final List<Move> log = new ArrayList<>();
    private final CheckersRulesEngine rules = new CheckersRulesEngine();
    private final User white = User.builder().id(1L).username("white").build();
    private final User black = User.builder().id(2L).username("black").build();

    /**
     * Creates the service with mocked repositories, move history and an authenticated user.
     * Isolates draw lifecycle behavior from the network transport.
     */
    @BeforeEach
    void setup() {
        games = mock(GameRepository.class); moves = mock(MoveRepository.class); bot = mock(BotService.class);
        UserService users = mock(UserService.class);
        when(users.requireEntity(anyLong())).thenAnswer(call -> call.<Long>getArgument(0) == 1L ? white : black);
        when(games.findForUpdate(1L)).thenAnswer(call -> Optional.of(game));
        when(games.findById(1L)).thenAnswer(call -> Optional.of(game));
        when(moves.findByGameIdOrderByMoveNumberAsc(1L)).thenAnswer(call -> new ArrayList<>(log));
        when(moves.save(any())).thenAnswer(call -> { Move move = call.getArgument(0); log.add(move); return move; });
        properties = new GameProperties();
        service = new GameService(games, moves, mock(GameInviteRepository.class), mock(UserRepository.class),
                users, mock(InviteService.class), mock(MatchmakingService.class), bot, rules, mock(PresenceService.class), properties);
        authenticate(white);
    }

    /**
     * Clears SecurityContextHolder after each test so another test cannot inherit its principal.
     */
    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    /**
     * Installs the supplied test account in Spring Security's context.
     */
    private void authenticate(User user) {
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Builds a compact test position from coordinates and piece codes.
     */
    private Board board(String... entries) {
        Board board = new Board(PieceColor.WHITE);
        for (int i = 0; i < entries.length; i += 2) board.set(Square.parse(entries[i]), Piece.fromCode(entries[i + 1]));
        return board;
    }

    /**
     * Creates an ongoing test game with the required mode, board and participants.
     */
    private void begin(GameMode mode, Board board) {
        game = Game.builder().id(1L).mode(mode).status(GameStatus.IN_PROGRESS).whitePlayer(white)
                .blackPlayer(mode == GameMode.BOT ? null : black).currentTurn(board.turn())
                .botDifficulty(BotDifficulty.EASY).boardState(board.toJson()).build();
    }

    /**
     * Submits the required human move or supplies the selected bot move and calls the real
     * GameService.
     */
    private GameEventMessage play(String from, String to) {
        Board board = Board.fromJson(game.getBoardState());
        if (game.getMode() == GameMode.BOT && board.turn() == PieceColor.BLACK) {
            EngineMove move = rules.legalMoves(board).stream().filter(m -> m.pathAlgebraic().equals(List.of(from, to))).findFirst().orElseThrow();
            when(bot.chooseMove(any(Board.class), any(BotDifficulty.class))).thenReturn(move);
            return service.applyBotMoveIfNeeded(1L);
        }
        authenticate(board.turn() == PieceColor.WHITE ? white : black);
        MoveMessage message = new MoveMessage(); message.setPath(List.of(from, to));
        return service.applyMove(1L, message);
    }

    /**
     * Checks repetition completion in BOT, FRIEND and MATCHMAKING, with exactly one draw update and
     * no accidental human loss.
     */
    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"BOT", "FRIEND", "MATCHMAKING"})
    void repetitionFinishesAllModesAndUpdatesDrawStatisticsExactlyOnce(GameMode mode) {
        begin(mode, board("a1", "WK", "h6", "BK"));
        GameEventMessage event = null;
        for (int cycle = 0; cycle < 2; cycle++) {
            play("a1", "b2"); play("h6", "g5"); play("b2", "a1"); event = play("g5", "h6");
        }
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals("DRAW_REPETITION", game.getFinishReason());
        assertEquals(GameOutcome.DRAW, Board.fromJson(game.getBoardState()).outcome());
        assertNull(game.getWinner()); assertNull(game.getCurrentTurn());
        assertEquals(EventType.GAME_FINISHED, event.getType());
        assertEquals("DRAW_REPETITION", event.getData().get("finishReason"));
        assertEquals(1, white.getDraws()); assertEquals(0, white.getLosses());
        if (mode != GameMode.BOT) assertEquals(1, black.getDraws());
        authenticate(white);
        MoveMessage message = new MoveMessage(); message.setPath(List.of("a1", "b2"));
        assertThrows(BusinessException.class, () -> service.applyMove(1L, message));
        assertEquals(1, white.getDraws()); assertEquals(8, log.size());
    }

    /**
     * Checks persisted offers, rejection of self-acceptance and expiry when the recipient makes a
     * move.
     */
    @Test
    void aDrawOfferIsDurableCannotBeAcceptedByItsAuthorAndExpiresOnOpponentMove() {
        begin(GameMode.FRIEND, Board.initial());
        service.offerDraw(1L);
        assertEquals(white.getId(), service.getGame(1L).getDrawOfferById());
        assertThrows(BusinessException.class, () -> service.respondToDraw(1L, true));
        play("a3", "b4");
        assertEquals(white.getId(), game.getDrawOfferById());
        play("b6", "a5");
        assertNull(game.getDrawOfferById());
        assertThrows(BusinessException.class, () -> service.respondToDraw(1L, true));
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    /**
     * Checks the configured minimum move count and normal two-player agreement.
     */
    @Test
    void drawAgreementHonoursConfiguredRegulation() {
        begin(GameMode.FRIEND, Board.initial());
        properties.setDrawOfferMinimumMoves(20);
        BusinessException early = assertThrows(BusinessException.class, () -> service.offerDraw(1L));
        assertEquals(ErrorCode.DRAW_TOO_EARLY, early.getErrorCode());
        properties.setDrawOfferMinimumMoves(0);
        service.offerDraw(1L); authenticate(black); service.respondToDraw(1L, true);
        assertEquals("DRAW", game.getFinishReason());
        assertEquals(1, white.getDraws()); assertEquals(1, black.getDraws());
    }

    /**
     * Checks that bot evaluation creates an offer only, and rejection preserves active play without
     * an immediate repeated offer.
     */
    @Test
    void approximateBotEvaluationOnlyOffersAndDecliningContinuesTheGame() {
        Board board = board("a1", "WK", "c1", "WK", "h6", "BK", "h8", "BK");
        board.setTurn(PieceColor.BLACK);
        begin(GameMode.BOT, board);
        DrawState state = DrawState.start(board);
        state.setWhiteStable(5); state.setBlackStable(4); state.setPlies(9);
        game.setDrawState(state.toJson());
        when(bot.considersDraw(any())).thenReturn(true);
        GameEventMessage event = play("h6", "f8");
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertNull(game.getFinishReason()); assertEquals(-1L, game.getDrawOfferById());
        assertEquals(-1L, event.getData().get("drawOfferById"));
        authenticate(white); service.respondToDraw(1L, false);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus()); assertNull(game.getDrawOfferById());
        assertFalse(DrawState.fromJson(game.getDrawState()).mayOfferBotDraw());
    }

    /**
     * Checks both agreement directions: accepting a bot offer and receiving the bot's acceptance or
     * rejection of a human offer.
     */
    @Test
    void aHumanCanAcceptABotOfferOrOfferADrawToTheBot() {
        begin(GameMode.BOT, board("a1", "WK", "h6", "BK"));
        game.setDrawOfferById(-1L); service.respondToDraw(1L, true);
        assertEquals("DRAW", game.getFinishReason()); assertEquals(1, white.getDraws());
        begin(GameMode.BOT, board("a1", "WK", "h6", "BK"));
        when(bot.considersDraw(any())).thenReturn(false);
        assertEquals(EventType.DRAW_DECLINED, service.offerDraw(1L).getType());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        when(bot.considersDraw(any())).thenReturn(true);
        assertEquals(EventType.DRAW_ACCEPTED, service.offerDraw(1L).getType());
        assertEquals("DRAW", game.getFinishReason());
    }
}
