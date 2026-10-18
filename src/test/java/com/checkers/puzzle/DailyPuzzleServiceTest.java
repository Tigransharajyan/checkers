package com.checkers.puzzle;

import com.checkers.engine.*;
import com.checkers.exception.BusinessException;
import com.checkers.model.enums.PieceColor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DailyPuzzleServiceTest {
    private final CheckersRulesEngine rules = new CheckersRulesEngine();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T20:15:00Z"), ZoneOffset.UTC);

    @Test
    void everyPuzzleRequiresAUniqueQuietKeyAndThreeOrFourMovesAgainstAllDefences() throws Exception {
        List<DailyPuzzleService.Definition> catalog;
        try (var in = getClass().getResourceAsStream("/puzzles/daily.json")) {
            catalog = mapper.readValue(in, new TypeReference<>() {});
        }
        assertEquals(22, catalog.size());
        Set<String> ids = new HashSet<>();
        for (var puzzle : catalog) {
            assertTrue(ids.add(puzzle.id()));
            Board board = new Board(PieceColor.WHITE);
            puzzle.squares().forEach((square, code) -> board.set(Square.parse(square), Piece.fromCode(code)));
            PuzzleSolver solver = new PuzzleSolver(rules);
            assertEquals(puzzle.maxPlies(), solver.distance(board, puzzle.maxPlies()), puzzle.id());
            assertEquals(-1, solver.distance(board, puzzle.maxPlies() - 1), puzzle.id());
            List<EngineMove> legal = rules.legalMoves(board);
            assertTrue(legal.size() >= 3);
            assertTrue(legal.stream().noneMatch(EngineMove::isCapture));
            assertEquals(1, legal.stream().filter(move -> solver.distance(
                    rules.applyMove(board, move.pathAlgebraic()).board(), puzzle.maxPlies() - 1) >= 0).count());
        }
    }

    @Test
    void everyDailyPuzzleCanBeCompletedThroughThePublicServiceUsingNormalRules() throws Exception {
        DailyPuzzleService service = new DailyPuzzleService(rules, mapper, clock);
        PuzzleSolver solver = new PuzzleSolver(rules);
        LocalDate first = LocalDate.parse(service.daily().date());
        for (int offset = 0; offset < 22; offset++) {
            LocalDate date = first.plusDays(offset);
            DailyPuzzleService selected = new DailyPuzzleService(rules, mapper,
                    Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
            var view = selected.daily();
            int remaining = view.maxPlayerMoves() * 2 - 1;
            while (!view.status().equals("SOLVED")) {
                assertTrue(view.movesPlayed() < view.maxPlayerMoves());
                Board before = Board.fromJson(view.boardState());
                EngineMove key = rules.legalMoves(before).stream().filter(move ->
                        solver.distance(rules.applyMove(before, move.pathAlgebraic()).board(), viewRemaining(before, solver)) >= 0)
                        .findFirst().orElseThrow();
                List<List<String>> moves = new ArrayList<>(view.playerMoves());
                moves.add(key.pathAlgebraic());
                var next = service.submit(view.id(), moves);
                Board replay = before;
                for (var event : next.events()) {
                    MoveResult result = rules.applyMove(replay, event.path());
                    assertTrue(result.success());
                    assertEquals(event.boardState(), result.board().toJson());
                    assertEquals(event.capturedSquares(), result.move().captured().stream().map(Square::algebraic).toList());
                    assertEquals(event.promoted(), result.move().promotion());
                    replay = result.board();
                    remaining--;
                }
                assertEquals(replay.toJson(), next.boardState());
                assertEquals(moves, next.playerMoves());
                view = next;
            }
            assertEquals(GameOutcome.WHITE_WINS, Board.fromJson(view.boardState()).outcome());
            assertTrue(remaining >= 0);
            assertEquals(view.boardState(), service.submit(view.id(), view.playerMoves()).boardState(), "Reload is deterministic");
        }
    }

    private int viewRemaining(Board board, PuzzleSolver solver) {
        return solver.distance(board, 7) - 1;
    }

    @Test
    void wrongLegalMovesDoNotAdvanceOrInventAWinAndIllegalMovesAreRejected() throws Exception {
        DailyPuzzleService service = new DailyPuzzleService(rules, mapper, clock);
        var initial = service.daily();
        Board board = Board.fromJson(initial.boardState());
        PuzzleSolver solver = new PuzzleSolver(rules);
        var wrong = rules.legalMoves(board).stream().filter(move -> solver.distance(
                rules.applyMove(board, move.pathAlgebraic()).board(), initial.maxPlayerMoves() * 2 - 2) < 0)
                .findFirst().orElseThrow();
        var result = service.submit(initial.id(), List.of(wrong.pathAlgebraic()));
        assertEquals("INCORRECT", result.status());
        assertEquals(initial.boardState(), result.boardState());
        assertEquals(List.of(), result.playerMoves());
        assertEquals(List.of(), result.events());
        assertThrows(BusinessException.class, () -> service.submit(initial.id(), List.of(List.of("a1", "h8"))));
        assertThrows(BusinessException.class, () -> service.submit("bogus", List.of(List.of("a1", "b2"))));
    }

    @Test
    void dayAndIdAreSharedWorldwideAtUtcMidnight() throws Exception {
        DailyPuzzleService yerevanClock = new DailyPuzzleService(rules, mapper, clock.withZone(ZoneOffset.ofHours(4)));
        assertEquals("2026-10-18", yerevanClock.daily().date());
        DailyPuzzleService nextDay = new DailyPuzzleService(rules, mapper,
                Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.ofHours(-7)));
        assertNotEquals(yerevanClock.daily().id(), nextDay.daily().id());
    }
}
