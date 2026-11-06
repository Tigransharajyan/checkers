# Independent actual-interface Red Team Review — round 2

**CHANGES REQUIRED.** The captured pages are coherent and purpose-specific. I found no evidence of invented dashboard content. The remaining concerns are a reproducible short-landscape layout defect and an incomplete accessible description contract for the puzzle board. These are bounded corrections rather than a reason to redesign the system again.

## Scope and evidence discipline

Independently inspected the current `pages-revision.md`, all 18 captures in `screenshots/actual-final-r2/`, and current `rating.js`, `tasks.js`, `tasks.html`, relevant game CSS, game status/announcement markup and lobby state handling. Other reviewers' reports were not read. No code changes. Findings below are derived from these captures and source, not carried forward from previous review findings.

Only public Play is compared with the captured actual Chess.com `screenshots/play.png`. Private Chess.com Home/archive were inaccessible, so local Home/archive are judged on task fitness rather than a claimed visual match. Screenshots cannot establish live keyboard or screen-reader behavior; source observations are distinguished from visually observed facts.

## Scores

| Dimension | /10 | Rationale |
|---|---:|---|
| Visual quality | 8 | Restrained surfaces, readable typography and useful page silhouettes; landscape player wrapping undermines polish. |
| Usability | 8 | Main actions and result states are clear, with natural sparse content rather than filler. Short-height game hierarchy needs refinement. |
| Consistency | 8 | Shared identity, action language and surfaces coexist with distinct functional layouts. |
| Accessibility | 7 | Visible controls and explicit feedback are strong; puzzle board lacks its objective in the accessible description, and live operation is not proven by images. |
| Originality | 7 | Reference-derived palette/board framing is evident but the local functionality and page compositions avoid a generic dashboard clone. |
| Product fit | 9 | Russian draughts is named before play, real game modes dominate, and personal data is presented without fabricated competitors/charts. |

## Three strengths

1. **Honest states have useful hierarchy.** Guest Progress is a concise sign-in explanation with no fake score. Zero Progress explains the absence of a win rate while showing actual zeros; populated Progress includes completed total and W/L/D. Home session failure is visibly recoverable rather than silently converted into a guest screen.
2. **Public Play is a credible adaptation of the observed reference.** Desktop uses the large initial board on the left and a compact mode rail on the right, with the recognizable cream/green board and dark navigation. Mobile removes the inert board and retains working-mode controls. This is similarity of composition, not imported chess content or a claim of identical behavior.
3. **Pages serve different jobs.** Home is quick action plus daily task; Game is board/player/status; Tasks is objective/board/response; History is labelled result fields on mobile; Progress is outcomes and explanation. There is no universal grid of interchangeable cards. Wrong/correct task states use readable messages and restrained color rather than decoration.

## Screenshot evidence: remaining concern requiring correction

### Short-landscape game player label collapses into an extremely narrow column

`game-landscape-844x390.png` shows the opponent text broken into “Бот”, “(Средни”, “й)” beside the avatar. The upper screen is dominated by header, a separate “Партия” row and a full-width turn banner; the board begins around y289, leaving only its top visible in the first viewport. Ordinary document scrolling is allowed, so the lower board being outside this screenshot is not itself evidence of inaccessible clipping. The abnormal opponent wrapping is an actual visible defect.

**Specific remedy:** decouple player bars from the height-constrained board width in short landscape. Give the avatar/name region the board's useful width or a readable minimum, with a single-line or sensible two-line opponent label. Consolidate the redundant game title/turn area at short height so the board begins substantially earlier. Keep scroll access to the full board/actions and verify at844×390 and a narrow portrait width.

**Source corroboration:** `.game-page .board-column > .player-card` uses `width:min(100%,728px,calc(100svh - 270px))`. At390px height that last term is120px, insufficient for avatar plus normal name. The readable-width short-height override applies only at `min-width:901px`, excluding844px. This explains the screenshot rather than hypothesizing an unrelated device issue.

## Source evidence: remaining accessibility concern requiring correction

### Puzzle objective is visually before the board but is not part of its accessible description

Current `tasks.html` gives `[data-task-board]` only `aria-describedby="board-instructions"`, whose text is the keyboard instruction. The side-to-move/objective and dynamic `[data-task-instructions]` text have no referenced IDs. The approved plan explicitly requires the objective to describe the board. A screen-reader user jumping directly to the interactive grid receives generic board/keyboard context without the current puzzle goal.

**Specific remedy:** give the objective and dynamic task instruction stable IDs and include them, together with keyboard help, in the board's `aria-describedby`. Retain the visible order. Verify focus directly into the board exposes side-to-move and move-limit objective without replaying the full board or every notation entry.

`tasks.js` does supply a dedicated announcement string for feedback and accepted moves, and `play.html` supplies game status plus a move-announcement region. That is positive source evidence, but it does not replace the board's missing objective association.

## Nonblocking refinements and evidence limitations

- `home-registered-empty-1440x1000.png` shows a small win-rate em dash. It is not falsely zero, but hiding that row until total>0 would better match the plan and avoid an unnecessary placeholder.
- Task correct-state reduces the instruction from two lines to one, moving the board upward by roughly25px compared with the ready/wrong captures. Reserve instruction height if the intended “board never shifts” behavior is strict. It is a small stability issue, not a broken solution flow.
- `tasks-390x844.png` captures an open navigation drawer, obscuring most of the task. `home-active-1440x1000.png` and `home-session-error-1440x1000.png` are actually844×390 images despite their filenames. They are usable evidence of those visible states, but do not prove the named desktop/closed-drawer compositions. Capture accurately named counterparts to close that evidence gap.
- The supplied round includes Russian captures; it does not visually establish Armenian wrapping, English localization, every API-error state, or focus/scroll operation. I do not infer failures from that absence, and I do not certify those checks as passed.
- Generous empty space in guest Home/Progress is defensible for this small feature set. Adding synthetic activity or oversized marketing blocks would weaken honesty. The real content is grouped and actionable, so emptiness alone is not a blocker.

## Verdict

**CHANGES REQUIRED** for the short-landscape player layout and puzzle objective association. The visual language and page-specific hierarchy are otherwise ready; resolve these concrete defects and verify the affected viewport and direct-to-board accessible description. No new feature or extra decorative content is warranted.
