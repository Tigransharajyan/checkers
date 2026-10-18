# Interface and real statistics

The shared theme is `src/main/resources/static/css/theme.css`. It follows the supplied navy / emerald / warm wood reference and applies to the lobby, game mode menu, board, daily task, progress, history, rules, authentication, dialogs and assistant. English, Russian and Armenian use matching message bundles. Daily tasks now use the [shared game engine and board](daily-puzzles-and-motion.md). Language selection reloads the current URL with its game ID and other parameters, so server-rendered text, FAQ answers, document titles and accessible labels switch together.

## Data definitions

- `GET /api/site/stats` is public and never cached. `completedGames` is `countByStatus(FINISHED)` in the shared game database, counting each game once. It includes bot games and guest games, excludes waiting, active and aborted games, and does not sum player counters (which could count a two-player game twice).
- `POST /api/site/presence` refreshes an HTTP-only visitor cookie. All visible screens send it every 15 seconds. A visitor expires after 45 seconds without a heartbeat. Tabs share a cookie; signed-in accounts are deduplicated across browsers by user ID. Anonymous visitors and guest accounts are included. Counts describe active visitors, not an estimated audience.
- Presence is held in memory on the single application instance, matching the existing matchmaking and STOMP deployment. A multi-instance deployment must move presence to shared storage before presenting a cluster-wide count. Completed-game totals already use the shared database.
- Database errors and network failures display `—`, never a substitute count. No community/site rating, fabricated audience, sample match history, estimated player rating or rank thresholds are shown.
- Personal progress uses the current `/api/auth/me` results. Win rate is `wins / (wins + losses + draws) × 100`, rounded to a whole percentage. Before the first completed game it displays `—`.
- The development H2 profile is in memory and resets on restart. Production totals persist in the configured PostgreSQL database.

## Illustration provenance

Workspace asset: `src/main/resources/static/images/checkers-hero.webp` (approximately 180 KB).
Generated with the built-in imagegen tool, then converted to WebP. No text, counters or UI are embedded in the image. The same image supports the hero, page headers, sidebar atmosphere and authentication backdrop.

Final generation prompt:

> Use case: stylized-concept. Asset type: wide website hero background, landscape 3:1 composition. Create a premium cinematic photorealistic 3D wooden Russian checkers board in a deep midnight navy and emerald scene, warm golden lamp light from upper right, soft dark forest leaves in background. Board at right two-thirds of image, angled perspective with luxurious walnut frame, precisely 8x8 alternating dark walnut and warm ivory squares and scattered black and ivory round draughts pieces (no chess pieces). Left third very dark deep teal negative space for website text. Similar mood to high-end game lobby: glossy dimensional pieces, subtle green atmosphere, warm wood, realistic beautiful detail. No text, no typography, no numbers, no logos, no UI, no watermark.

## Verification

- `./mvnw clean test`: 40 tests pass, including full application startup, 27 translated page renders, actual database totals, public API access, cookie flags, deduplication and presence expiry.
- Browser checks cover registration, switching all three languages, solving a daily task, legal moves against a bot, resigning, creating and joining an invite, matchmaking between two browser sessions, history and saved personal results.
- All nine page variants (including the active game board) were checked for horizontal overflow at 320, 390, 768, 1024, 1280, 1440 and 1774 pixels. Bot losses display as losses; bot labels are localized.
- The browser confirmed that an extra tab does not increase online presence, failed statistics requests show `—`, and successful requests restore the database count. No JavaScript runtime errors were observed.
