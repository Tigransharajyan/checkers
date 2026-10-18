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

@Service
public class DailyPuzzleService {
    private final CheckersRulesEngine rules;
    private final PuzzleSolver solver;
    private final Clock clock;
    private final List<Definition> catalog;

    @Autowired
    public DailyPuzzleService(CheckersRulesEngine rules, ObjectMapper mapper) throws IOException {
        this(rules, mapper, Clock.systemUTC());
    }

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

    public View daily() {
        LocalDate date = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Definition definition = forDate(date);
        return view(date, definition, initialBoard(definition), definition.maxPlies(), List.of(), "ACTIVE", List.of());
    }

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
                // Choose the defence that delays the proven win the longest.
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

    private BusinessException invalidMove(String reason) {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_MOVE);
        exception.setDetails(Map.of("moveError", reason));
        return exception;
    }

    private EngineMove strongestReply(Board board, int remaining) {
        return rules.legalMoves(board).stream()
                .sorted(Comparator.comparing(EngineMove::toString))
                .max(Comparator.comparingInt(move -> solver.distance(
                        rules.applyMove(board, move.pathAlgebraic()).board(), remaining - 1)))
                .orElseThrow(() -> new IllegalStateException("No reply in an active puzzle"));
    }

    private View view(LocalDate date, Definition definition, Board board, int remaining,
                      List<List<String>> moves, String status, List<Step> events) {
        return new View(id(date, definition), date.toString(), (definition.maxPlies() + 1) / 2,
                moves.size(), (remaining + 1) / 2, board.toJson(),
                rules.legalMoves(board).stream().map(EngineMove::pathAlgebraic).toList(),
                List.copyOf(moves), status, events);
    }

    private Step step(MoveResult result) {
        return new Step(result.board().toJson(), result.move().pathAlgebraic(),
                result.move().captured().stream().map(Square::algebraic).toList(), result.move().promotion());
    }

    private Definition forDate(LocalDate date) {
        return catalog.get(Math.floorMod(date.toEpochDay(), catalog.size()));
    }

    private String id(LocalDate date, Definition definition) { return date + "-" + definition.id(); }

    private Board initialBoard(Definition definition) {
        Board board = new Board(PieceColor.WHITE);
        definition.squares().forEach((square, piece) -> board.set(Square.parse(square), Piece.fromCode(piece)));
        return board;
    }

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

    record Definition(String id, int maxPlies, Map<String, String> squares) {}
    public record Step(String boardState, List<String> path, List<String> capturedSquares, boolean promoted) {}
    public record View(String id, String date, int maxPlayerMoves, int movesPlayed, int remainingPlayerMoves,
                       String boardState, List<List<String>> legalMoves, List<List<String>> playerMoves,
                       String status, List<Step> events) {}
}
