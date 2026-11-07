package com.checkers.controller;

import com.checkers.puzzle.DailyPuzzleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public daily-puzzle API that returns positions and accepts proposed solutions for server
 * validation.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class DailyPuzzleController {
    private final DailyPuzzleService puzzles;

    /**
     * Returns the shared puzzle for the current UTC day and its initial position. Does not expose
     * the solution.
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyPuzzleService.View> daily() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(puzzles.daily());
    }

    /**
     * Accepts the player's proposed moves and delegates validation to the normal rules engine and
     * bounded proof search.
     */
    @PostMapping("/{id}/moves")
    public ResponseEntity<DailyPuzzleService.View> submit(@PathVariable String id, @Valid @RequestBody Attempt request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(puzzles.submit(id, request.moves()));
    }

    /**
     * Incoming puzzle plan containing complete player-move paths. Bean Validation restricts request
     * shape before engine validation.
     */
    public record Attempt(@NotNull @Size(min = 1, max = 4)
            List<@NotNull @Size(min = 2, max = 13) List<@NotNull @Pattern(regexp = "[a-h][1-8]") String>> moves) {}
}
