# Agent 6 — Anti-AI Design Critic / Red Team

**Verdict: CHANGES REQUIRED.** The direction is credible, but the specification is not yet concrete enough to authorize implementation under its own strict professional/page-specific gate. It prevents obvious fake content better than it specifies the actual composition of the most important empty and guest screens.

## Evidence and limits

Independently inspected `docs/redesign/chesscom/pages-revision.md`, product `screenshots/final-desktop.png`, reference `screenshots/play.png`, current `index.html`, `play-menu.html`, `rating.html`, `history.html`, `tasks.html`, `fragments/modes.html`, `rating.js`, `history.js`, and `PageController.java`. No other agent reviews were read. This is a plan review against supplied screenshots and source, not a claim that proposed layouts have been rendered or tested. I did not independently verify live Chess.com behavior or its authenticated home/archive.

## Scores (proposed specification, not completed UI)

| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 7 | Restrained palette and density are plausible; important page compositions remain prose rather than reviewable layouts. |
| Usability | 6 | Clear primary tasks, but the new home creates an extra step for guests and crucial responsive ordering is unresolved. |
| Consistency | 7 | Shared tokens help; optional game-shell alternatives leave navigation behavior unsettled. |
| Accessibility | 6 | Good baseline commitments, insufficient route/state-specific reading order and error recovery requirements. |
| Originality | 6 | Sensible adaptation to checkers, but graphite/green plus summary rail is still a generic dashboard recipe without stronger structural decisions. |
| Product fit | 8 | Existing bot, invite, daily puzzle and actual personal results are respected; borrowed chess-only functionality is explicitly rejected. |

## Three strengths

1. It identifies the actual central failure. Current `index.html` and `play-menu.html` use the same position preview and mode-selector fragment; the product screenshot confirms that this is a giant board with a narrow stack of controls. Removing that board from home gives the revision a meaningful purpose.
2. It refuses fictional leaderboard data, progress graphs, puzzle maps, social login and decorative imported artwork. This is unusually important for a small product whose screenshot contains only five completed games: visual richness must come from useful composition rather than invented activity.
3. It correctly allows the supplied reference's board-plus-mode-list composition on desktop Play. The reference itself repeats mode cards, so banning all repeated cards would misread the evidence. The proposal instead calls for task-appropriate lists, tables and reading widths.

## Critical problems and required fixes

### 1. The new home is still a dashboard template waiting for content (high)

“Dynamic greeting + main column + narrow right rail + actual modules” does not specify a convincing first screen for a guest with no active game, no personal statistics and no results. Removing the existing board could leave a greeting, Play link, daily puzzle and a large empty results panel. That is the exact incomplete dashboard trope the revision intends to remove. A singleton daily puzzle does not inherently justify a permanent right rail.

**Fix:** provide explicit desktop/mobile block diagrams for (a) guest, (b) signed-in zero-history user, (c) returning user with active game and results. Specify which sections disappear, where remaining content expands, and the primary action in each. For the guest, justify the extra home-to-Play click; define a useful compact Play entry and daily task arrangement that does not require personalized filler. Avoid an empty activity feed as the principal home content.

### 2. The launch page promises priority without choosing its interaction model (high)

It simultaneously describes “selection of a mode,” “one primary CTA,” “settings beside the selected mode,” and the reference's menu of independent mode cards. Those are different interfaces. The current product has immediate bot and online actions and inline invite entry, whereas the reference uses large navigation choices with fewer inline settings. A visual implementation could either retain the present tall form stack or invent a selected-mode dashboard; both could claim compliance.

**Fix:** choose one precise model. Define whether rows immediately launch, select and reveal a single action area, or navigate. Show online idle/search/cancel, bot difficulty/start, and friend create/join states. Define how invite entry remains discoverable. Give board/control width allocation and the exact breakpoint rule for hiding or moving the inert preview. The static preview must not appear keyboard-interactive or announce opponent/player status as if a game existed.

### 3. Accessibility promises do not settle the known ordering conflict (high)

The current task template's DOM places date, question and instructions before its board; the proposal says mobile should prioritize the board and then the condition/controls. Moving the question after an interactive board can make the user encounter the problem before knowing whose turn and what to achieve. CSS rearrangement could also create divergent visual and keyboard order. “44px targets” and “visible focus” do not solve this.

**Fix:** require puzzle title/side-to-move/objective before the board in both DOM and visual order, with feedback and actions immediately after. Preserve the existing keyboard instructions and live announcement. Define focus behavior for mode reveal, search cancellation, game return, and guest-to-auth navigation. Add explicit loading/error/retry states for personal statistics: current `rating.js` returns after a failed `/api/auth/me`, leaving placeholder values and the login action, so this is an existing failure mode, not a hypothetical enhancement.

### 4. Reference evidence is being used more broadly than it supports (medium)

The supplied Play screenshot supports an inert board, repeated clickable mode rows and a stable left navigation. It does not establish that the actual game must remove its sidebar or that the local puzzle needs a separate shell. Home and archive were inaccessible, as the plan commendably discloses. Broad statements about how Chess.com separates game navigation are therefore not sufficient evidence for the proposed shell change. Also, `/rating` is intentionally a different product feature from Chess.com's leaderboard; it should be defended by local data and purpose rather than visual similarity.

**Fix:** label shell choices and home/archive compositions as local design hypotheses. Choose the game header rather than leaving “remove or collapse” open, list its return destination and retained controls, and demonstrate board-area gain at the stated widths. Keep the useful page distinctions without claiming that distinct shells are intrinsically more professional.

## Gate to approval

A revision can address these points without adding features or changing code: append concrete route/state layout diagrams, choose the Play interaction and game-navigation models, and specify ordering and recovery acceptance criteria. Show the sparse guest states prominently. Distinct template names, content landmarks and 384 technical combinations cannot themselves demonstrate visual quality or useful page differences; review representative rendered compositions after implementation.

**CHANGES REQUIRED** because the unresolved first-screen states and interaction alternatives permit another polished-looking but generic, empty dashboard implementation while still satisfying most of the prose specification.
