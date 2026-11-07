package com.checkers.puzzle;

import com.checkers.engine.*;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.exception.ResourceNotFoundException;
import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Loads and validates the puzzle catalogue, selects the shared UTC-day position and revalidates all
 * submitted progress. Uses the same rules engine as ordinary games.
 */
@Service
public class DailyPuzzleService {
    private final CheckersRulesEngine rules;
    private final PuzzleSolver solver;
    private final Clock clock;
    private final List<Definition> catalog;

    /**
     * Loads the puzzle catalogue and validates every position through bounded proof search. The
     * production constructor uses UTC; the Clock overload makes time deterministic in tests.
     */
    @Autowired
    public DailyPuzzleService(CheckersRulesEngine rules, ObjectMapper mapper) throws IOException {
        this(rules, mapper, Clock.systemUTC());
    }

    /**
     * Loads the puzzle catalogue and validates every position through bounded proof search. The
     * production constructor uses UTC; the Clock overload makes time deterministic in tests.
     */
    DailyPuzzleService(CheckersRulesEngine rules, ObjectMapper mapper, Clock clock) throws IOException {
        this.rules = rules;
        this.solver = new PuzzleSolver(rules);
        this.clock = clock;
        try (var input = getClass().getResourceAsStream("/puzzles/daily.json")) {
            catalog = List.copyOf(mapper.readValue(input, new TypeReference<List<Definition>>() {}));
        }
        if (catalog.isEmpty()) throw new IllegalStateException("Empty puzzle catalog");
        for (Definition definition : catalog) verify(definition);
    }

    /**
     * Selects the shared puzzle for the current UTC date and returns its initial state without
     * revealing a solution.
     */
    public View daily() {
        LocalDate date = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Definition definition = forDate(date);
        return view(date, definition, initialBoard(definition), definition.maxPlies(), List.of(), "ACTIVE", List.of());
    }

    /**
     * Replays the submitted plan from the puzzle start, validating legality and preserving the
     * proven win. Selects the strongest defense after each accepted player move and returns
     * progress plus the latest exchange events.
     */
    public View submit(String id, List<List<String>> playerMoves) {
        LocalDate date;
        try { date = LocalDate.parse(id.substring(0, 10)); }
        catch (RuntimeException ex) { throw new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND); }
        Definition definition = forDate(date);
        if (!id.equals(id(date, definition))) throw new ResourceNotFoundException(ErrorCode.TASK_NOT_FOUND);
        Board board = initialBoard(definition);
        int remaining = definition.maxPlies();
        List<List<String>> accepted = new ArrayList<>();
        List<Step> events = List.of();
        for (List<String> path : playerMoves) {
            if (board.outcome() != GameOutcome.IN_PROGRESS || remaining <= 0) {
                throw invalidMove("GAME_OVER");
            }
            MoveResult move = rules.applyMove(board, path);
            if (!move.success()) {
                throw invalidMove(move.error().name());
            }
            if (solver.distance(move.board(), remaining - 1) < 0) {
                return view(date, definition, board, remaining, accepted, "INCORRECT", List.of());
            }
            accepted.add(List.copyOf(path));
            List<Step> turnEvents = new ArrayList<>();
            turnEvents.add(step(move));
            board = move.board();
            remaining--;
            if (board.outcome() == GameOutcome.IN_PROGRESS) {
                // Black chooses the defense that delays the proven White win longest.
                EngineMove reply = strongestReply(board, remaining);
                MoveResult response = rules.applyMove(board, reply.pathAlgebraic());
                board = response.board();
                remaining--;
                turnEvents.add(step(response));
            }
            events = List.copyOf(turnEvents);
        }
        return view(date, definition, board, remaining, accepted,
                board.outcome() == GameOutcome.WHITE_WINS ? "SOLVED" : "ACTIVE", events);
    }

    /**
     * Creates an INVALID_MOVE domain failure with the engine's specific moveError detail.
     */
    private BusinessException invalidMove(String reason) {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_MOVE);
        exception.setDetails(Map.of("moveError", reason));
        return exception;
    }

    /**
     * Selects the legal Black defense that delays the proven White victory longest. Path sorting
     * makes equally scored choices reproducible.
     */
    private EngineMove strongestReply(Board board, int remaining) {
        return rules.legalMoves(board).stream()
                .sorted(Comparator.comparing(EngineMove::toString))
                .max(Comparator.comparingInt(move -> solver.distance(
                        rules.applyMove(board, move.pathAlgebraic()).board(), remaining - 1)))
                .orElseThrow(() -> new IllegalStateException("No reply in an active puzzle"));
    }

    /**
     * Builds public puzzle state with position, remaining moves, legal paths, accepted actions and
     * status. Does not expose the complete winning line.
     */
    private View view(LocalDate date, Definition definition, Board board, int remaining,
                      List<List<String>> moves, String status, List<Step> events) {
        return new View(id(date, definition), date.toString(), (definition.maxPlies() + 1) / 2,
                moves.size(), (remaining + 1) / 2, board.toJson(),
                rules.legalMoves(board).stream().map(EngineMove::pathAlgebraic).toList(),
                List.copyOf(moves), status, events);
    }

    /**
     * Converts one successful engine move into an event for sequential client playback.
     */
    private Step step(MoveResult result) {
        return new Step(result.board().toJson(), result.move().pathAlgebraic(),
                result.move().captured().stream().map(Square::algebraic).toList(), result.move().promotion());
    }

    /**
     * Selects a catalogue entry from the epoch day using floorMod. Every visitor receives the same
     * puzzle for a given date.
     */
    private Definition forDate(LocalDate date) {
        return catalog.get(Math.floorMod(date.toEpochDay(), catalog.size()));
    }

    /**
     * Combines the date and catalogue-position identifier so subsequent requests can be checked
     * against the selected puzzle.
     */
    private String id(LocalDate date, Definition definition) { return date + "-" + definition.id(); }

    /**
     * Builds the puzzle's initial board from square-to-piece codes. The human player starts as
     * White.
     */
    private Board initialBoard(Definition definition) {
        Board board = new Board(PieceColor.WHITE);
        definition.squares().forEach((square, piece) -> board.set(Square.parse(square), Piece.fromCode(piece)));
        return board;
    }

    /**
     * Validates square placement, piece types, search depth, a proven win and a unique first key
     * move. An invalid catalogue prevents service startup.
     */
    private void verify(Definition definition) {
        if (definition.maxPlies() < 5 || definition.maxPlies() > 7) throw new IllegalStateException("Puzzle depth out of bounds");
        definition.squares().forEach((square, code) -> {
            if (!Square.isValidAlgebraic(square) || !Square.parse(square).isDark()) throw new IllegalStateException("Invalid puzzle square");
            Piece piece = Piece.fromCode(code);
            int rank = Square.parse(square).rank();
            if (!piece.isKing() && (piece.color() == PieceColor.WHITE ? rank == 7 : rank == 0)) throw new IllegalStateException("Unpromoted piece on last rank");
        });
        Board board = initialBoard(definition);
        if (rules.evaluateOutcome(board) != GameOutcome.IN_PROGRESS || solver.distance(board, definition.maxPlies()) != definition.maxPlies()) {
            throw new IllegalStateException("Unproven puzzle: " + definition.id());
        }
        long winningChoices = rules.legalMoves(board).stream().filter(move ->
                solver.distance(rules.applyMove(board, move.pathAlgebraic()).board(), definition.maxPlies() - 1) >= 0).count();
        if (winningChoices != 1 || rules.legalMoves(board).size() < 3) throw new IllegalStateException("Puzzle must have one key move: " + definition.id());
    }

    /**
     * Puzzle-catalogue entry containing its identifier, allowed depth and initial piece placement.
     */
    record Definition(String id, int maxPlies, Map<String, String> squares) {}
    /**
     * Public puzzle-move event containing the resulting board, full path, captured squares and
     * promotion.
     */
    public record Step(String boardState, List<String> path, List<String> capturedSquares, boolean promoted) {}
    /**
     * Public puzzle progress without a complete solution: current board, legal paths, progress and
     * recent events.
     */
    public record View(String id, String date, int maxPlayerMoves, int movesPlayed, int remainingPlayerMoves,
                       String boardState, List<List<String>> legalMoves, List<List<String>> playerMoves,
                       String status, List<Step> events) {}
}
