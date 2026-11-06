# Independent post-implementation review R2 — UI Systems Architect

**CHANGES REQUIRED.** The current implementation has credible page separation, route integration and recoverable data states. Remaining changes concern responsive component ownership and agreed Home information order, plus incomplete evidence for the states whose filenames promise desktop coverage. This judgment is based on the current code and current rendered artifacts, not earlier verdicts or peer reports.

## Scope and evidence

Read `pages-revision.md` and examined current Home/Play/Game/Tasks/Progress templates, navigation fragments, lobby/common/rating/daily-task logic, BoardUI keyboard/reduced-motion hooks, app/foundation CSS, PageController, SecurityConfig, SeoService, locale resources and relevant browser-test source. Viewed all 18 PNGs currently in `screenshots/actual-final-r2/`.

I did not run the application or tests in this review. Test source establishes intended assertions, not successful execution. Network transitions below are code-inferred unless a corresponding screenshot is explicitly named. Screenshots are Russian; EN/HY hooks and message values were source-inspected, not visually certified. No inaccessible authenticated Chess.com Home or archive screen is claimed; the plan explicitly acknowledges that limitation and it is not a reason for rejection.

**Evidence caveats:** `file` confirms that `home-active-1440x1000.png` and `home-session-error-1440x1000.png` are actually **844×390** PNGs. They show a short mobile landscape viewport, not desktop. `tasks-390x844.png` has the drawer open over a solved puzzle; it is not an unobstructed initial mobile Tasks capture. These images prove those rendered states exist but cannot establish the desktop/mobile compositions suggested by their names.

| Dimension | Score / 10 | Rationale |
|---|---:|---|
| Visual quality | 8 | Clear hierarchy, restrained surfaces and large usable boards; landscape player metadata is visibly cramped. |
| Usability | 7 | Direct game start and continuation exist; registered mobile ordering and zero-progress next action remain incomplete. |
| Consistency | 7 | Shared navigation and primitives work, but responsive component widths and CSS definitions diverge. |
| Accessibility | 7 | Real roving board focus, overlay ownership, reduced motion and status hooks; objective association and some responsive evidence remain incomplete. |
| Originality | 7 | Own draughts pieces/identity with reference-informed proportions rather than imported Chess.com branding. |
| Product fit | 8 | Russian draughts ruleset, genuine data, distinct home/launcher/workspace/archive, no invented leaderboard. |

## Three strengths

1. **Architecture follows page purpose while preserving the existing stack.** Home lacks a full-size initial board; Play has the reference-like board/rail desktop silhouette and action-first mobile layout; Game has a focused header and complete drawer; Progress and History use real account data. Existing server-rendered templates and `/play?id|code` selection remain appropriate.
2. **Recoverable session/data handling is concrete in the current source.** `refreshMe()` retains stored auth on transient failure and exposes account Retry. Refresh only clears credentials for identified invalid-refresh authorization responses. Active/recent loaders now clear stale rows, mark busy, distinguish loading/error/empty, and use guarded initialization. Daily preview error is a separate accessible region outside the decorative hidden board. The rendered session-error image supports the visible retention message; actual token retention/retry success is code-inferred here.
3. **Progress and puzzle state visuals are meaningful.** Guest Progress has explanation and Login/Register without fake metric; zero Progress has actual total/W/L/D zeros and no undefined percentage; populated Progress includes total. Wrong/correct puzzle feedback is rendered explicitly and uses text plus color, while desktop puzzle board is bounded near the specified size.

## Integration assessment

- `/home` is a public controller alias; SEO maps it to home text and canonical/hreflang at root, and it is absent from indexable sitemap paths. Locale-preserving navigation includes `/home`. Existing game query dispatch and API names are preserved.
- The same sidebar fragment owns navigation, account and language; Game presents it as a drawer with a matching toggle target. Overlay owns inertness, Escape and focus return. `common.js` sets `aria-current` on active links. These are source-confirmed contracts, not an interactive screen-reader certification.
- EN/RU/HY resources contain ruleset, launcher, account-check and zero-progress messages. Intl number/date formatting remains. The local fonts and Armenian sidebar-width adjustment remain. No current screenshot proves long Armenian text at each breakpoint.
- Foundation CSS honors reduced motion globally and BoardUI queries the same preference. Keyboard square selection retains roving tab stops and arrow/selection handlers. Current browser tests contain focus/overlay/session checks; I have not executed them.

## Required changes

### 1. P2 — Home mobile source order still puts history before the daily task

Current `index.html` nests active/start/recent inside `.home-main-column`, followed by daily/stats inside `.home-side-column`. When the grid becomes one column, a registered user's recent history necessarily precedes daily. The approved order is active → quick start → daily → personal results, with matching DOM/reading/Tab order. Guest mobile screenshots conceal the discrepancy because recent is absent.

**Remedy:** make active, start, daily, personal summary and recent independently placeable regions in a coherent source tree. Use grid placement for desktop without changing meaningful keyboard/reading order; do not correct only the visual order with CSS `order`. Include a populated registered 390px capture and keyboard/source-order check. Active is already before start in the current source and should remain there.

### 2. P2 — Game landscape board and player bars use contradictory height constraints

In `game-landscape-844x390.png`, “Бот (Средний)” wraps into a narrow stack above the board even though horizontal space is available. Source explains it: the late landscape rule gives `.board-frame` a minimum360px target, while `.board-column > .player-card` and `.board-instructions` retain `calc(100svh - 270px)`, only120px at390px height. Thus related parts of the same board component no longer share its width. A width-only board assertion will not detect this.

**Remedy:** assign one computed width/custom property to board frame, both player bars and instruction text in the landscape context; allow ordinary vertical scrolling and wrap names normally within that shared width. Verify 844×390 and another short height with long EN/RU/HY names, both player bars, actions and notation. Keep the minimum playable board size without squeezing its metadata to an unrelated120px column.

### 3. P2 — Final evidence does not demonstrate the claimed active/error desktop composition

Both active/error Home files named1440×1000 are844×390, with important content below the viewport. Registered active desktop, active guest Home, registered populated mobile Home and normal unobstructed mobile puzzle are not all represented in this evidence set. The code supports active guest sessions by permitting `loadActiveGames()` for `user.guest`, but that is distinct from observing it rendered correctly.

**Remedy:** recapture the named states at their actual intended dimensions, include viewport dimensions/state metadata in a small manifest, and keep the current landscape images under accurate names. Include active guest and registered Home, account error then restored state, unobstructed mobile puzzle and drawer-open puzzle separately. Do not describe source inspection or a guest-created browser test as visual proof of a registered desktop state.

## Maintenance risks and smaller fixes

- **CSS has several owners for the same selectors.** Legacy dashboard/game/mobile rules coexist with later replacements and another late landscape fix. Comments still imply a1200px game drawer threshold while JS uses the drawer at all game widths. Consolidate each component's definitions and responsive rules; update comments. Play preview hides at1199px, while the plan says below1180px: choose one boundary and align plan, CSS and tests at1179/1180/1199/1200.
- **Zero Progress lacks the promised Play next step.** Its rendered message tells the user to finish a game, but the only local action area contains Login/Register, both hidden for registered accounts. Add a contextual Play link in the zero state; keeping global navigation is useful but does not complete that local empty-state contract.
- **Home summary is games/wins/win-rate rather than total/W/L/D.** The zero-account image still shows an undefined rate as a dash. Prefer actual total/W/L/D and omit undefined rate, as specified. This is not a fabricated statistic, but it is an avoidable placeholder and inconsistent with the explicit Progress treatment.
- **Puzzle objective is not programmatically associated with board.** `tasks.html` still points `aria-describedby` only to keyboard instructions, while the objective text has no ID. Add that ID to the board description so entering the spatial control exposes the task as well as its controls.
- **Puzzle completion moves the board slightly.** Correct-state screenshot has a shorter objective/instruction block and the board rises relative to the unsolved/wrong capture. Reserve a reasonable instruction/status footprint or keep stable objective plus separate result text, following the approved no-board-shift behavior.
- **Repeated retry requests can race.** Active/recent Retry is not disabled or request-versioned; multiple responses can overwrite newer state. A per-region in-flight guard, cancellation or latest-request generation would make ownership explicit. Current artifacts do not demonstrate an actual race.
- **Cached personal metrics after account-check failure should be identified as last known.** Retaining a session is correct; if stored stats are shown despite failed `/me`, label them as saved/last confirmed or temporarily hide only that metrics section until verification succeeds. Continue allowing independent active/daily requests.

## Acceptance boundary

The current route, API, token and shared-shell approach is viable. Fix the two concrete responsive/order defects and produce accurately identified evidence for the required account/viewport states before final acceptance. EN/HY layout, reduced-motion execution, keyboard end-to-end behavior and retry/token retention must be reported as tested only when actually run; this review confirms their source hooks and relevant rendered Russian states, not those unperformed checks.
