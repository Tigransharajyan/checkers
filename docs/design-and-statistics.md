# Interface and real statistics

The current interface uses a dark graphite and olive draughts workspace, with solid surfaces, local Inter/Noto Armenian, and flat playable pieces. Foundations live in `design-system.css`; layouts and runtime board states live in `app.css`. The former `theme.css` override layer is removed. See [the redesign report](redesign/REPORT.md) for the audit, references, independent reviews and current validation.

## Data definitions

- `GET /api/site/stats` is public and never cached. `completedGames` is `countByStatus(FINISHED)` in the shared game database, counting each game once. It includes bot games and guest games, excludes waiting, active and aborted games, and does not sum player counters (which could count a two-player game twice).
- `POST /api/site/presence` refreshes an HTTP-only visitor cookie. All visible screens send it every 15 seconds. A visitor expires after 45 seconds without a heartbeat. Tabs share a cookie; signed-in accounts are deduplicated across browsers by user ID. Anonymous visitors and guest accounts are included. Counts describe active visitors, not an estimated audience.
- Presence is held in memory on the single application instance, matching the existing matchmaking and STOMP deployment. A multi-instance deployment must move presence to shared storage before presenting a cluster-wide count. Completed-game totals already use the shared database.
- Database errors and network failures display `—`, never a substitute count. No community/site rating, fabricated audience, sample match history, estimated player rating or rank thresholds are shown.
- Personal progress uses the current `/api/auth/me` results. Win rate is `wins / (wins + losses + draws) × 100`, rounded to a whole percentage. Before the first completed game it displays `—`.
- The development H2 profile is in memory and resets on restart. Production totals persist in the configured PostgreSQL database.

## Asset provenance

The former `checkers-hero.webp` was generated with imagegen in an earlier revision. It and `login-board-scene.png` are no longer referenced or loaded by the UI. They remain unused historical files. The current interface uses no photographs or generated illustrations. The social JPEG is a deterministic draughts diagram rebuilt by `scripts/build-social.py`; the favicon is a flat SVG mark. Existing fonts and Lucide assets retain their licenses.

## Current verification

See [the redesign report](redesign/REPORT.md) and its evidence files. Earlier verification figures described the preceding revision and have been superseded.
