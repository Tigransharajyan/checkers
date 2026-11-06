# Shared engine puzzles and smooth moves

## Daily puzzle contract

`GET /api/tasks/daily` returns the UTC day's position, its stable ID, legal move paths, the target (3–4 white moves), and the current board in the normal game's board-state format. The lobby reads the same endpoint for its preview. It does not receive a solution.

The 22 compositions in `src/main/resources/puzzles/daily.json` have a quiet opening move, at least three legal alternatives, a unique key move and a proven win in 5–7 plies. The catalog includes sacrifices, multiple captures and promotion. These are internally generated tactical compositions, not positions attributed to a player or an external puzzle database.

`DailyPuzzleService` injects the same `CheckersRulesEngine` used by `GameService`. `PuzzleSolver` proves a forced win within the target against **all** legal black replies, and the server chooses a defence that delays that win the longest. Every catalog entry is verified at application startup; an invalid or unproved entry prevents startup. The proof is memoized over the finite catalog tree.

`POST /api/tasks/{id}/moves` accepts only the player's move history. The server reconstructs the position from the catalog, validates each full move using the normal rules engine, and generates the black replies deterministically. A legal move that misses the target is reported as `INCORRECT` without advancing the board. Illegal paths return normal move errors. Accepted responses contain authoritative board snapshots for animating the player's move and the reply individually. Client state and a forged local-storage “solved” flag cannot create a solved response.

The puzzle uses the regular `Checkers.BoardUI` for selection, drag moves, full capture paths, flying kings, promotion, coordinates and animations. There is no separate JavaScript rules engine or midpoint-based capture handler. Puzzles do not create games or change site statistics. Progress is keyed by the date and position ID and is revalidated by the server on reload.

## Motion

Board cells and unchanged pieces retain their DOM nodes across selection and server updates. A single moving piece is composited with the Web Animations API along the complete move path, including captures that return to the starting square. An accepted local move continues its immediate visual preview instead of replaying it. Dragging hands the released screen position to that preview, avoiding a snap back to the source.

Authoritative board updates and game websocket events are processed in order. Duplicate board snapshots do not restart animations. Captures and promotion commit from the engine snapshot at the end of the motion. Rejected moves roll back the preview; an unanswered game move requests a fresh server snapshot after ten seconds. Reduced-motion and background-tab updates commit directly. Quiet long king moves are distinguished from captures using the actual board occupancy.

## Running checks

Start an isolated local development server with `./mvnw spring-boot:run -Dspring-boot.run.addResources=true`, then:

```sh
./mvnw test
npm ci
npx playwright install chromium
npm run test:browser
```

Browser tests default to `http://localhost:8080`. `BASE_URL` selects another test server, and `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` can select an existing Chromium binary. The gameplay smoke test creates a disposable registered test account and plays actual local test games.

The suite covers all 22 puzzle proofs, complete service-based solutions, illegal and incorrect moves, UTC rollover, public API validation, real game statistics, all translated templates, animation ordering and DOM continuity, capture loops, promotion, reduced motion, drag release, saved puzzle progress, responsive EN/RU/HY layouts, bot replies, guest invitations, premoves and a negotiated draw.

## Premove appearance and wooden audio

Premove selection no longer draws speculative destination/capture overlays across the board. Selection and the queued path remain visible; pieces retain their colors. Capture rings explicitly use a transparent background, so an overlay cannot fill a piece with black.

Board events now play local, softened wooden samples instead of synthesized tones. Three subtle move variations, a double-contact capture, and quiet start/end tap sequences share the same sound palette. Sound buffers load once per page, unlock on interaction, and late/hidden-tab sounds are skipped. CC0 provenance and the sample license are in `src/main/resources/static/sounds/board/`; rebuild with `python3 scripts/build-board-sounds.py` (ffmpeg required).

Verified with the real-game/premove regression, animation/drag/keyboard regression, and audio decoding/level/duration checks: 3 browser tests passed. Preview: `docs/redesign/chesscom-refinement/board-sounds-preview.wav` (move, capture, start, end).
