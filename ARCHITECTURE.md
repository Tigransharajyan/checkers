# Checkers Online — Architecture (Stage 1)

Diploma web project skeleton: package layout, ER model, REST / WebSocket contracts, i18n scaffolding.
Business logic and UI copy are deferred to later stages.

---

## 1. Package structure

```
com.checkers
├── CheckersApplication
├── config          # Security, WebSocket, Locale (CookieLocaleResolver)
├── controller      # REST + Thymeleaf page controllers
├── service         # Auth, User, Game, Matchmaking, Invite, Bot (stubs)
├── repository      # Spring Data JPA
├── model
│   ├── entity      # User, Game, Move, GameInvite
│   └── enums       # GameMode, GameStatus, BotDifficulty, EventType, …
├── dto
│   ├── request
│   └── response
├── websocket
│   ├── controller  # STOMP @MessageMapping handlers
│   └── dto         # MoveMessage, GameEventMessage, MatchmakingMessage
├── security        # UserDetails / UserDetailsService
└── exception       # ErrorCode, BusinessException, GlobalExceptionHandler
```

Resources:

```
src/main/resources
├── application.yml          # profiles: dev (H2), prod (PostgreSQL)
├── i18n/
│   ├── messages.properties      # en (default)
│   ├── messages_ru.properties
│   └── messages_hy.properties
├── static/  (css, js, robots.txt, sitemap.xml)
└── templates/  (Thymeleaf stubs)
```

**Design notes**

- JWT access + refresh auth (stateless). Guests supported.
- Backend never returns human-language strings for game events / API errors — only `EventType` / `ErrorCode` / `MoveError` + substitution `data`.
- `BotDifficulty` is a Java enum persisted as `VARCHAR` on `games`, not a separate table (lookup values are fixed: EASY / MEDIUM / HARD).
- Rules engine lives in `com.checkers.engine` (no Spring).
---

## 2. ER diagram

```mermaid
erDiagram
    USER ||--o{ GAME : "white_player"
    USER ||--o{ GAME : "black_player"
    USER ||--o{ GAME : "winner"
    USER ||--o{ MOVE : "plays"
    USER ||--o{ GAME_INVITE : "creates"
    GAME ||--o{ MOVE : "contains"
    GAME ||--o| GAME_INVITE : "has"

    USER {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar preferred_locale "nullable, len=2"
        timestamptz created_at
        timestamptz updated_at
    }

    GAME {
        bigint id PK
        varchar mode "FRIEND|MATCHMAKING|BOT"
        varchar status "WAITING|IN_PROGRESS|FINISHED|ABORTED"
        bigint white_player_id FK
        bigint black_player_id FK
        varchar bot_difficulty "nullable: EASY|MEDIUM|HARD"
        bigint winner_id FK
        varchar current_turn "WHITE|BLACK"
        text board_state
        timestamptz started_at
        timestamptz finished_at
        timestamptz created_at
    }

    MOVE {
        bigint id PK
        bigint game_id FK
        int move_number
        varchar from_square
        varchar to_square
        varchar captured_squares
        boolean promoted
        bigint player_id FK
        timestamptz created_at
    }

    GAME_INVITE {
        bigint id PK
        varchar code UK
        bigint game_id FK UK
        bigint creator_id FK
        varchar status "PENDING|ACCEPTED|EXPIRED|CANCELLED"
        timestamptz expires_at
        timestamptz created_at
    }
```

### Fields & constraints

| Table | Key fields | Notes |
|-------|------------|-------|
| **users** | `preferred_locale VARCHAR(2) NULL` | `en` / `ru` / `hy`; null until user chooses |
| **games** | `mode`, `status`, players, `bot_difficulty` | `bot_difficulty` required only for `BOT` |
| **moves** | `(game_id, move_number)` unique | Ordered history per game |
| **game_invites** | `code` unique, `game_id` unique | Friend-mode invite link/code |

### Indexes

| Index | Columns |
|-------|---------|
| `uk_users_username` / `idx_users_username` | username |
| `uk_users_email` / `idx_users_email` | email |
| `idx_games_status` | status |
| `idx_games_mode` | mode |
| `idx_games_white_player` / `idx_games_black_player` | player FKs |
| `idx_games_created_at` | created_at |
| `uk_moves_game_move_number` | (game_id, move_number) |
| `idx_moves_game_id` | game_id |
| `uk_game_invites_code` / `idx_game_invites_code` | code |
| `idx_game_invites_status` | status |
| `idx_game_invites_expires_at` | expires_at |

### Cardinality

- User 1—N Games (as white / black / winner)
- Game 1—N Moves
- Game 1—0..1 GameInvite (friend mode only)
- User 1—N GameInvites (as creator)

---

## 3. REST API contract

Auth is **JWT Bearer** (stateless). Errors: `ApiErrorResponse { errorCode, timestamp, details }` — no localized messages.

| Method | Path | Purpose | Request DTO | Response DTO |
|--------|------|---------|-------------|--------------|
| `POST` | `/api/auth/register` | Register | `RegisterRequest` `{username, email, password}` | `201 AuthResponse` `{user, accessToken, refreshToken, tokenType}` |
| `POST` | `/api/auth/login` | Login | `LoginRequest` `{usernameOrEmail, password}` | `200 AuthResponse` |
| `POST` | `/api/auth/guest` | Guest login | — | `201 AuthResponse` |
| `POST` | `/api/auth/refresh` | Refresh tokens | `RefreshTokenRequest` `{refreshToken}` | `200 AuthResponse` |
| `POST` | `/api/auth/logout` | Revoke refresh (optional body) | `RefreshTokenRequest?` | `204` |
| `GET` | `/api/auth/me` | Current user | — | `200 UserResponse` |
| `POST` | `/api/games` | Create game (all 3 modes) | `CreateGameRequest` `{mode, botDifficulty?}` | `201 GameResponse` |
| `GET` | `/api/games/{id}` | Game details | — | `200 GameResponse` |
| `GET` | `/api/games/active` | Active games of user | — | `200 List<GameSummaryResponse>` |
| `GET` | `/api/games/history` | Finished games (pageable; empty for guests) | `?page&size` | `200 Page<GameSummaryResponse>` |
| `POST` | `/api/games/invite/join` | Join by invite code | `JoinInviteRequest` `{code}` | `200 GameResponse` |
| `POST` | `/api/games/matchmaking/join` | Enter matchmaking queue | — | `202` |
| `DELETE` | `/api/games/matchmaking/leave` | Leave queue | — | `204` |
| `PATCH` | `/api/users/me/locale` | Change language (registered only) | `LocaleUpdateRequest` `{locale: en\|ru\|hy}` | `200 UserResponse` + set locale cookie |

### Create game by mode

| `mode` | Behaviour (Stage 2) |
|--------|---------------------|
| `FRIEND` | Create `Game` + `GameInvite` with code; status `WAITING`; response includes `inviteCode` |
| `MATCHMAKING` | Prefer REST enqueue or create after WS match; typically pair via `MatchmakingService` |
| `BOT` | Require `botDifficulty`; create game with bot as opponent; status `IN_PROGRESS` |

`UserResponse`: `{id, username, email, preferredLocale}`  
`GameResponse`: full game + optional `inviteCode` + `moves`  
`GameSummaryResponse`: list/history card fields

---

## 4. WebSocket (STOMP / SockJS) contract

**Endpoint:** `/ws` (SockJS)  
**App prefix:** `/app`  
**Broker:** `/topic`, `/queue`  
**User prefix:** `/user`

### Client → server

| Destination | Payload | Meaning |
|-------------|---------|---------|
| `/app/game/{gameId}/move` | `MoveMessage` `{fromSquare, toSquare}` or `{path:[…]}` | Submit move |
| `/app/game/{gameId}/resign` | — | Resign |
| `/app/game/{gameId}/draw` | `DrawOfferMessage` `{accept}` | Offer / respond to draw |
| `/app/game/{gameId}/presence` | — | Announce (re)connect |
| `/app/matchmaking/join` | — | Enter queue |
| `/app/matchmaking/leave` | — | Leave queue |

### Server → client

| Destination | Payload | When |
|-------------|---------|------|
| `/topic/game/{gameId}` | `GameEventMessage` | Moves, presence, draw, finish |
| `/user/queue/matchmaking` | `MatchmakingMessage` | Queue status / match found |
| `/user/queue/errors` | `GameEventMessage` (`type=ERROR`) | Private validation errors |

### Envelope (language-agnostic)

```json
{
  "type": "OPPONENT_DISCONNECTED",
  "gameId": 42,
  "timestamp": "2026-09-22T16:00:00Z",
  "data": {
    "userId": 7,
    "username": "alice"
  }
}
```

`EventType` values:  
`GAME_STARTED`, `MOVE_MADE`, `OPPONENT_DISCONNECTED`, `OPPONENT_RECONNECTED`, `DRAW_OFFERED`, `DRAW_ACCEPTED`, `DRAW_DECLINED`, `GAME_RESIGNED`, `GAME_FINISHED`, `MATCHMAKING_QUEUED`, `MATCHMAKING_MATCHED`, `MATCHMAKING_CANCELLED`, `INVITE_ACCEPTED`, `ERROR`

Example `MOVE_MADE` data: `{moveNumber, fromSquare, toSquare, capturedSquares, promoted, playerId, boardState, currentTurn}`  
Example `MATCHMAKING_MATCHED` data: `{gameId, opponentId, opponentUsername, yourColor}`  
Example `GAME_FINISHED` data: `{winnerId, reason}` where `reason` is a code (`RESIGN`, `NO_MOVES`, `DRAW`, …) — not a sentence.

Frontend (Stage 4) maps `type` → i18n key, e.g. `events.OPPONENT_DISCONNECTED`, and interpolates `data`.

---

## 5. i18n infrastructure

| Piece | Choice |
|-------|--------|
| `LocaleResolver` | `CookieLocaleResolver` (`CHECKERS_LOCALE`), **default `Locale.ENGLISH`** |
| Not used | `AcceptHeaderLocaleResolver` (TZ: default language must be English) |
| Switch | `LocaleChangeInterceptor` param `lang` (`?lang=ru`) |
| Messages | `spring.messages.basename=i18n/messages`, encoding UTF-8 |
| Files | `messages.properties`, `messages_ru.properties`, `messages_hy.properties` (stub key `app.title`) |
| Auth locale | `PATCH /api/users/me/locale` persists `preferred_locale` + refreshes cookie |

---

## 6. Profiles

| Profile | DB | DDL |
|---------|----|-----|
| `dev` (default) | H2 in-memory (PostgreSQL mode) | `update` |
| `prod` | PostgreSQL via env vars | `validate` |

---

## Next stages (out of scope here)

- Stage 4: Thymeleaf/JS UI, full i18n keys, AI help widget

---

## Stage 2 — contract changes vs Stage 1

Explicit deltas (auth model + move path + guest):

1. **Auth switched from session cookie to JWT** (stateless):
   - `AuthResponse` now: `{ user, accessToken, refreshToken, tokenType }`
   - New endpoints:
     - `POST /api/auth/guest` → temporary guest account (no password/email)
     - `POST /api/auth/refresh` body `{ refreshToken }` → new token pair
   - `POST /api/auth/logout` optionally body `{ refreshToken }` to revoke
   - REST: `Authorization: Bearer <accessToken>`
   - STOMP CONNECT: header `Authorization: Bearer …` (or `access_token`)

2. **User model**:
   - `guest BOOLEAN`, `email` / `password_hash` nullable for guests
   - `preferredLocale` always null for guests (locale = cookie only)
   - New table `refresh_tokens`

3. **`UserResponse`**: added `guest` boolean

4. **History**: guests get empty page from `GET /api/games/history` (no persisted history UX)

5. **`MoveMessage`**: added optional `path: string[]` for multi-captures; `fromSquare`/`toSquare` still work for simple plies

6. **`DrawOfferMessage.accept`**: `null` = offer, `true` = accept, `false` = decline

7. **`moves.player_id`**: nullable (bot plies have `player_id = null`, WS `data.bot = true`)

8. **Engine package** `com.checkers.engine` — pure Java Russian draughts rules; errors via `MoveError` enum (never localized text)

9. **Error codes added**: `INVALID_REFRESH_TOKEN`, `GUEST_LOCALE_NOT_PERSISTED`, `GUEST_HISTORY_FORBIDDEN`, `INVALID_MOVE`, `BOT_DIFFICULTY_REQUIRED`

---

## Stage 4 — frontend & i18n

1. Full `messages.properties` / `_ru` / `_hy` for UI, MoveError, EventType, API errors
2. Thymeleaf pages: lobby, login/register, play, history; `window.i18n` from `I18nModelAdvice`
3. Language switcher: `?lang=` + `PATCH /api/users/me/locale` for registered users
4. SockJS/STOMP client; board highlights via `GET /api/games/{id}/legal-moves`
5. Fonts: Noto Sans / Noto Serif Armenian (Google Fonts)

---

## Stage 5 — SEO & AI assistant

1. `robots.txt` allows `/`, `/login`, `/register`, `/faq`; disallows `/api/`, `/ws/`, `/play`, `/history`
2. `sitemap.xml` with `xhtml:link` alternates en/ru/hy + x-default=en
3. Public pages: `<link rel="canonical">` + hreflang (via `fragments/seo.html`)
4. Intent assistant: `POST /api/assistant/chat` — TF-IDF (en/ru), keyword overlap (hy); FAQ fallback
5. Extension: implement `AssistantReplyService` / enable `LlmAssistantService` + `ASSISTANT_LLM_API_KEY`

---

## Stage 3 — gameplay modes, bot AI, presence, stats

1. **Friend invites**
   - `inviteCode` + `invitePath` (`/play?code=…`) in `GameResponse`
   - Error codes: `INVITE_ALREADY_USED`, `GAME_ALREADY_STARTED`, `INVITE_EXPIRED`, `INVALID_INVITE`
   - On join: WS `INVITE_ACCEPTED` + `GAME_STARTED`

2. **Matchmaking**
   - In-memory `ConcurrentLinkedQueue` (+ per-user timeout task)
   - **Scaling note:** for multi-instance deploy use Redis LIST/ZSET + pub-sub; current impl is single-node
   - Config: `app.game.matchmaking-timeout-seconds` (default 120)
   - New `EventType.MATCHMAKING_TIMEOUT`

3. **Bot**
   - Minimax + alpha-beta over `CheckersRulesEngine`
   - EASY depth 2 / top-5, MEDIUM depth 4 / top-3, HARD depth 6 / top-1
   - Async via `BotPlayService` (`@Async("botExecutor")`) → same `/topic/game/{id}` as human moves

4. **Disconnect / reconnect**
   - `PresenceService` listens to STOMP subscribe/disconnect on `/topic/game/{id}`
   - Config: `app.game.reconnect-timeout-seconds` (default 60)
   - Events: `OPPONENT_DISCONNECTED`, `OPPONENT_RECONNECTED`, `TECHNICAL_DEFEAT`

5. **History & stats**
   - Moves store `path` for replay; games store `finishReason`
   - Users: `wins` / `losses` / `draws` (not updated for guests)
   - `UserResponse` includes stats; `GameSummaryResponse.finishReason`
