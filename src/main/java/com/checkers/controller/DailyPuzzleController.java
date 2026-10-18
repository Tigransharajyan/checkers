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

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class DailyPuzzleController {
    private final DailyPuzzleService puzzles;

    @GetMapping("/daily")
    public ResponseEntity<DailyPuzzleService.View> daily() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(puzzles.daily());
    }

    @PostMapping("/{id}/moves")
    public ResponseEntity<DailyPuzzleService.View> submit(@PathVariable String id, @Valid @RequestBody Attempt request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(puzzles.submit(id, request.moves()));
    }

    public record Attempt(@NotNull @Size(min = 1, max = 4)
            List<@NotNull @Size(min = 2, max = 13) List<@NotNull @Pattern(regexp = "[a-h][1-8]") String>> moves) {}
}
