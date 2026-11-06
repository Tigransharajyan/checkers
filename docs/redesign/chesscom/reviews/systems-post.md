# Independent actual-interface review — UI Systems Architect — Chess.com revision

## Evidence examined
Read plan.md and post.md. Viewed the actual reference play.png/computer.png during the plan review and the new screenshots home1440/390, registered game1440/390, home HY1440, tasks390, login390 and assistant1440. Read current index/play-menu/position/play templates and relevant app.css layout rules. Did not read other reviewers' conclusions or modify implementation.

I independently opened the actual local home page at390×844 in Chromium to inspect preview semantics and keyboard focus sequence. Read completed supplied logs: browser8/8 passed; matrix384 cases no failures/runtime errors; axe58 states no violations with documented incomplete contrast checks. These runs were inspected, not personally repeated in full.

## Scores
| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | The actual large green board, player bars, narrow neutral sidebar and action stack substantially match the requested reference composition. |
| Usability | 7 | Pointer/touch layout is clear, but mobile keyboard focus jumps to lower history before primary game actions. |
| Consistency | 7 | Shared components remain, but the new appended workspace override block repeats several existing base/responsive definitions. |
| Accessibility | 7 | Static preview semantics are correct and earlier controls remain labelled; the mobile DOM/visual order discrepancy is a concrete regression. |
| Originality | 7 | Deliberate fidelity follows the user's explicit choice while retaining own branding and actual draughts features. |
| Product fit | 9 | The interface now directly presents the board and game modes, rather than a generic product dashboard. |

## Three strongest aspects
1. The requested layout is realized, not merely recolored: actual player cards bracket the game board, the action/notation area sits separately to the right, and the main menu uses a board-led composition.
2. The preview does not pretend to be a game. I confirmed `.position-board` has role=img and zero tabbable cells/controls. Its initial-position caption and own product strings preserve honesty while allowing the user's desired visual reference.
3. Existing reusable mode controls and actual board hooks remain single instances. The three-language screenshots and completed behavioral logs support continued product functionality without imported chess assets, new dependencies or invented ratings/features.

## Blocking finding
### CS1 — Mobile visual order and keyboard order diverge (High)
Location: index.html `.dashboard-main` (preview, counts, recent links) preceding `.dashboard-aside` (game actions); app.css applies `.dashboard-aside { order:-1; }` below1219px.

The mobile screenshot presents actions first, but DOM focus traverses history before the action panel. Independently reproduced at390×844:
1. Focus `#main-content`, then press Tab.
2. First focused element: «Все партии», with scrollY1295.
3. Second: «Войдите, чтобы сохранять следующие партии», still scrollY1295.
4. Third: «Найти соперника», abruptly returning scrollY0.

This makes the primary keyboard path jump down the page and back up, despite the specification explicitly preserving actions-before-preview accessibility on narrow layouts. An axe pass does not detect this semantic order regression.

Required correction: place the action panel before the preview/recent section in DOM reading/focus order, and use explicit desktop grid areas/column placement to keep board-left/menu-right visually. Preserve active-game priority and single instances of every data hook/ID. Do not patch this with positive tabindex or duplicate desktop/mobile controls. Add a behavior regression: after the skip/main entry, the first primary content focus on home must be the mode action (unless active-game Resume is present), and it must not first scroll to the history section. Verify the same principle on /play menu and across the stacking breakpoint.

## Non-blocking maintainability recommendation
The new workspace styles are appended after the earlier foundations of app.css and repeat `.content`, `.sidebar`, `.dashboard-grid`, `.mode-tile`, `.board-frame`, `.game-context` and related responsive rules. There is still no third stylesheet, but this recreates some override-on-override debt the original audit identified. Consolidate the touched selectors into their original component/layout sections when practical, and keep one documented owner for each media/container contract. Preserve runtime board and overlay rules while doing so; no broad refactor is necessary for the focus-order fix.

## Other observations and limits
- Large-board and fixed-bottom-nav full-page screenshots were not treated as proof that covered content cannot be scrolled into view.
- I did not perform a real-device session, screen-reader session or production performance measurement.
- Current visual fidelity is materially closer to the user's selected reference. The blocking verdict is about a reproduced keyboard regression, not disagreement with the user's visual preference.

## Verdict
**CHANGES REQUIRED.** Correct the mobile DOM/focus sequence before final approval. The static preview, single-hook board layout and reference fit are otherwise sound within the inspected scope.
