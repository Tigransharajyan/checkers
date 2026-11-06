# Independent page architecture review — Accessibility & Usability

Decision: **CHANGES REQUIRED** before implementation. Direction A is suitable, but the specification leaves essential access contracts undefined and includes one measurable control-contrast problem. This is a plan review, not a claim that the proposed UI exists or has passed assistive-technology testing.

Reviewed only `docs/redesign/chesscom/pages-revision.md`, the two requested screenshots (`final-desktop.png`, real Chess.com `play.png`), current PageController/templates, shared navigation/overlay/board code and CSS. No other reviewer reports were read. Screenshots support visual comparison only; code inspection supports the interaction observations below.

| Criterion | Score / 10 |
|---|---:|
| Visual quality | 8 |
| Usability | 7 |
| Consistency | 8 |
| Accessibility | 6 |
| Originality | 7 |
| Product fit | 9 |

## Three strengths

1. Separating home, mode selection and active play reduces repeated board navigation and puts the user's next action first. Mobile actions preceding a decorative play preview is particularly helpful.
2. Honest guest/empty/loading/error states and real data improve comprehension. A dedicated archive and readable FAQ suit their content much better than forcing them into a board workspace.
3. The foundation explicitly retains visible focus, reduced motion and target sizing. Existing code provides a strong base: skip link, main landmark, roving board keyboard focus and square labels, overlay focus trapping/background inertness/focus restoration, task status announcements and reduced-motion handling in both CSS and board animation logic.

## Critical issues and required concrete fixes

### 1. Interactive boundary contrast is insufficient

The proposed `#68645f` border is only **1.90:1** against raised `#3d3b38` and **2.31:1** against page `#302e2b`. Where that border is what identifies an input/select or button boundary, it is insufficient for a 3:1 non-text contrast target. Decorative panel dividers need not meet this requirement, but the plan applies one border token broadly.

**Fix:** split decorative borders from control outlines and give the latter a verified 3:1 contrast against their adjacent surface in normal and error states. Specify a focus token and validate it against the board as well as neutral surfaces; retain the current two-tone square focus treatment. Text tokens are substantially stronger: dark green-button label/action is about 6.85:1, secondary/raised about 6.16:1. Do not replace the dark button label with white.

### 2. Removing the sidebar lacks an explicit complete navigation contract

The game plan guarantees a lobby return, but does not say how Rules, language/account controls and the rest of navigation remain reachable after replacing the global shell. Current mobile bottom navigation omits History and Rules; they are reachable through the drawer/top bar today. Dropping that drawer with the sidebar would silently remove paths. The document leaves “remove or collapse” unresolved.

**Fix:** choose a definite game header: persistent named Lobby link plus a named menu retaining every global destination, locale and account action. Preserve the skip link/main landmark and current-page indication in each shell. Define drawer Escape, initial focus, trapping, background inertness and focus return by retaining the current shared Overlay owner. Verify all destinations from both desktop and mobile game routes, including invite-code and waiting states.

### 3. Route-specific visual order could diverge from reading/focus order

The plan places mobile play actions before preview and task board before condition/controls, but does not specify DOM order. Current task DOM puts the condition before the board, which is useful for understanding what to solve. A CSS-only reorder can make keyboard focus jump around the screen or present a board before its instructions to a screen-reader user.

**Fix:** explicitly establish a logical source order for each template. For tasks retain a short condition/turn instruction before the interactive board and associate it using `aria-describedby`; place feedback/actions immediately after it. For Play make the preview noninteractive and hide decorative semantics; place the launcher before it in source order. Preserve one H1 and named main on each route. Test Tab and screen-reader reading order at every layout breakpoint.

### 4. State announcements are too vague to preserve working behavior

“Short status feedback” and keeping mouse/touch/keyboard working do not define what must survive the template split. Current game has `role=status`, tasks have a dedicated live region, history has status and retry, and overlays have a focus manager. New home active-session loading and separated launcher states introduce additional asynchronous content. A visually present draw offer or a changing notation panel alone does not guarantee announcement.

**Fix:** add an event/state matrix to the plan: queue entered/cancelled/failed, game ready, turn/opponent move, draw offer, result, task correct/incorrect/reset, archive loading/empty/error, and home active-session error. Specify a stable concise live region, busy state and reachable retry/cancel for each relevant transition. Announce opponent moves with coordinates/piece state as well as whose turn it is; do not read the entire move log on every update or steal board focus for routine changes. Associate form errors to fields and retain entered invite/auth values. Exercise resign/game-over dialogs with keyboard and focus restoration.

### 5. Board sizing, targets and zoom lack a reconciled acceptance rule

At 360px, an eight-column board cannot offer 44px squares once margins/coordinates are included (44 × 8 = 352px before either). “Mobile targets at least 44px” plus height-driven board sizing is therefore internally underspecified. The screenshot demonstrates how a large board can dominate the first screen; copying that composition is not evidence of mobile access. Fixed mobile navigation, browser chrome and an on-screen keyboard may also cover controls if sizing is only tested by width.

**Fix:** distinguish ordinary controls (44px target requirement) from the spatial board. Define board maximum usable width, a deliberate board-target exception/alternative if needed, and maintain single-tap selection plus keyboard operation without drag-only actions. Preserve legal-move/selected/last-move distinctions using shape/outline as well as color. Test 360/390px portrait, short landscape, 200% text scaling and 400% zoom/reflow, with menus/dialogs and invite keyboard open. Controls may scroll naturally; never shrink text or clip the board to force everything into one viewport. Reserve safe-area-aware space for fixed navigation and ensure focused controls scroll into view. Respect reduced motion for every new transition and keep the existing board animation preference check.

## Required gate evidence

Amend the specification with these contracts before approval. Implementation verification should then include keyboard-only route traversal, board moves and capture sequence, overlay return focus, screen-reader event checks, contrast measurements, reduced-motion behavior and narrow/zoomed reachability. Existing route/locale/width checks are useful but cannot substitute for these interaction checks.

**CHANGES REQUIRED:** approve direction A after the plan resolves the control contrast and explicitly preserves navigation, reading order, announcements and board access across the new shells. No code changes were made by this review.
