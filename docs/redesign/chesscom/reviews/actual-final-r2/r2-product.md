# Independent actual implementation review — round 2

Senior Product Designer · Agent 1 · 2026-11-06

## Evidence and limits

Reviewed current `pages-revision.md`, current Home/Play/Progress/FAQ/auth templates, lobby and progress state code, and relevant game CSS. `audit.md`, `design-research.md`, and `concepts.md` do not exist at the specified paths. Inspected the actual-final-r2 screenshots for Home default/mobile/active/session-error/empty-account, Play desktop/mobile, Game desktop/landscape, Tasks initial/wrong/correct/mobile, Progress guest/zero/populated and populated mobile History. No other reviewer reports used and no browser interaction performed for this review.

Evidence warning: `home-active-1440x1000.png` and `home-session-error-1440x1000.png` actually depict an 844×390 landscape viewport, not the dimensions in their names. `tasks-390x844.png` depicts an open navigation drawer over a solved puzzle, rather than a normal initial puzzle view. These are useful evidence of those states, but do not prove the absent views. FAQ/auth have source evidence only in this round's directory. Private Chess.com Home/archive were unavailable, as the audit discloses; no private-page fidelity claim is made.

## Scores

| Dimension | /10 | Rationale |
|---|---:|---|
| Visual quality | 7 | Desktop launcher and puzzle are deliberate and readable; short-height game has visibly broken player-label wrapping. |
| Usability | 8 | Distinct routes and direct online start materially improve utility; continue is now first in source and rendered active state. Short landscape still wastes critical board space. |
| Consistency | 8 | Shared navigation/palette, compact game header and route-specific layouts form a coherent system. |
| Accessibility | 7 | Source provides loading/status semantics and separate error recovery; landscape label layout and incomplete rendered state coverage constrain confidence. Keyboard behavior not browser-tested here. |
| Originality | 6 | Familiar reference-driven game language with appropriate local checkers pieces and honest features. |
| Product fit | 8 | Russian 8×8 ruleset is visible before starting; real results and guest/zero/populated distinctions replace unsupported competitor data. |

## Three strengths

1. **Useful route separation is real.** Home prioritizes useful actions and a small daily puzzle; desktop Play has the reference-like full board/rail composition while mobile removes preview and exposes mode controls. Game removes the global sidebar; Tasks is a separate solving workspace; Progress and History use actual results rather than fabricated leaderboards.
2. **Continuation and account recovery are represented honestly.** Current Home template puts active games before new-game controls. `loadActiveGames()` includes authenticated guest accounts, exposes loading with aria-busy, distinguishes confirmed none from error, and offers retry. The active screenshot visibly puts Continue before the quick start, and the account-error screenshot preserves the saved-session explanation rather than claiming the account disappeared. This is source/screenshot evidence, not a tested resume action.
3. **Task and progress states communicate meaning.** Puzzle wrong/correct screenshots use explanatory text and distinct status styling. Practice is secondary before completion. Progress guest has no absent-value score; zero shows actual total/W/L/D and an explanation without invented 0%; populated shows a defined percentage and real counts. Mobile history has labelled fields and a clear open action.

## Blocking issue

### Short landscape game: player row width is incorrectly bound to viewport height

In `game-landscape-844x390.png`, “Бот (Средний)” wraps into several short fragments above the board despite almost the entire viewport width being free. The board begins around y289, leaving only its top portion in the first 390px view. This reduces utility at the exact short-height viewport the specification required inspecting.

Source corroboration: `.game-page .board-column > .player-card` uses `width: min(100%,728px,calc(100svh - 270px))`. At a 390px viewport height, the player row is only 120px wide; avatar and gap leave very little label space. Board sizing may depend on height, but opponent/player labels should not inherit that tiny width. This is a concrete layout defect, not merely the acceptable need to scroll.

**Fix:** give player rows a width based on available content/board-column width with a practical readable minimum; decouple their width from the board's height-derived square size. In short landscape compact the context/status spacing so the opponent label fits naturally and the board follows promptly. Keep document scrolling and normal text size. Capture the same 844×390 initial game after correction, plus own/opponent long names, showing readable labels and accessible board/actions through ordinary scroll.

## Other concrete refinements and verification gaps

- Home zero-account screenshot still shows Games=0, Wins=0 and Win rate=—. Replace the undefined rate row with “Complete a game to see win rate” or omit it, and favor the specified W/L/D summary. It is not fabricated data, but the presentation is less informative than the implemented Progress zero state.
- Correct puzzle screenshot moves the board upward relative to initial/wrong state because the instruction collapses to fewer lines. Reserve a consistent objective/status area if keeping the promised stable board position is important. The feedback rail itself is clear.
- Replace misleading screenshot filenames, then add genuine desktop active-home and guest-active-home captures and one clean mobile puzzle capture. Source supports guest continuation but no supplied screenshot conclusively demonstrates the guest-active state.
- Add normal FAQ/auth rendered captures to this round if this report is used as a whole-product visual gate. Current source preserves labelled search/native disclosures and standalone auth, but source alone cannot establish current rendered fit.
- Full queue start/cancel, retry recovery, modal/drawer keyboard operation, 200% text/400% zoom and mobile keyboard behavior remain unverified by this review. Code semantics are useful evidence, not a substitute for those observations.

## Reference assessment and verdict

Wide Play shows credible spatial similarity to the observed Chess.com Play: board left, mode rail right, familiar dark/green relationship, compact task choices. Local Home is a product interpretation informed by available functionality, not an observed copy of private Chess.com Home. Avoiding unrelated chess features and foreign leaderboard data is appropriate.

**CHANGES REQUIRED.** The product structure and state honesty are materially improved and no new architecture is needed. Fix the confirmed short-landscape player-row defect and provide correctly identified rendered evidence for the affected view before approving final execution. The remaining refinements are bounded; no user-research or browser-testing claims are inferred.
