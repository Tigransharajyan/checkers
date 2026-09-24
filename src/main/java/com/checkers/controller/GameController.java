package com.checkers.controller;

import com.checkers.dto.request.CreateGameRequest;
import com.checkers.dto.request.JoinInviteRequest;
import com.checkers.dto.response.GameResponse;
import com.checkers.dto.response.GameSummaryResponse;
import com.checkers.service.GameService;
import com.checkers.service.MatchmakingService;
import com.checkers.websocket.GameEventPublisher;
import com.checkers.websocket.dto.GameEventMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final MatchmakingService matchmakingService;
    private final GameEventPublisher gameEventPublisher;

    @PostMapping
    public ResponseEntity<GameResponse> create(@Valid @RequestBody CreateGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.createGame(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGame(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<GameSummaryResponse>> active() {
        return ResponseEntity.ok(gameService.getActiveGames());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<GameSummaryResponse>> history(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(gameService.getHistory(pageable));
    }

    @GetMapping("/{id}/legal-moves")
    public ResponseEntity<List<List<String>>> legalMoves(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getLegalMovePaths(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> endActiveGame(@PathVariable Long id) {
        GameEventMessage event = gameService.endActiveGame(id);
        gameEventPublisher.publishToGame(id, event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite/join")
    public ResponseEntity<GameResponse> joinInvite(@Valid @RequestBody JoinInviteRequest request) {
        return ResponseEntity.ok(gameService.joinByInviteCode(request.getCode()));
    }

    @PostMapping("/matchmaking/join")
    public ResponseEntity<Void> joinMatchmaking() {
        matchmakingService.enqueue();
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/matchmaking/leave")
    public ResponseEntity<Void> leaveMatchmaking() {
        matchmakingService.dequeue();
        return ResponseEntity.noContent().build();
    }
}
