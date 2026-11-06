# Independent UI Systems Architect review — Chess.com fidelity revision

Reviewed plan.md in full and the actual public UI screenshots play.png and computer.png. Read the current index/play-menu template structure and board coordinate/state setup for compatibility. Other reviewers' reports were not read. No implementation was changed.

## Scores of the proposed specification
| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | The board-led composition and compact action panel are a clear reference-derived structure rather than another palette-only revision. |
| Usability | 8 | Visible modes, difficulty and continuation remain explicit; actions-before-preview stacking prevents the large board from burying start controls on narrow layouts. |
| Consistency | 9 | Existing components, data hooks, overlay and keyboard contracts remain reusable across the changed composition. |
| Accessibility | 8 | The plan retains meaningful focus/state fixes and specifies AA action text; preview semantics and height caps require careful implementation. |
| Originality | 7 | Deliberate fidelity follows the user's explicit preference; own branding and actual draughts behavior retain the appropriate product identity. |
| Product fit | 9 | The reference's board/menu pattern fits a game application, while unsupported chess features and fictional data are explicitly excluded. |

## Three strongest aspects
1. The revision is a layout change with bounded technical scope: a large board and player bars on the left, actions or real game controls on the right. It can be implemented within current Thymeleaf fragments and CSS without new state architecture or dependencies.
2. Shared behavior is explicitly retained: one hook instance per page, existing board/grid interactions, pending-request protections, guest/invitation flows and the common overlay owner. The new appearance does not require reintroducing the old unsafe drawer or matchmaking code.
3. The 320px action panel, height-aware board and explicit narrow-layout order address both reference fidelity and the application's actual constraints. Long Armenian labels are allowed to wrap inside the narrower170px sidebar rather than being clipped.

## Critical issues
None in this specification. The selected composition is implementable with the current architecture and prior preservation contracts.

## Concrete implementation checkpoints (not additional plan blockers)
- Treat the home/menu initial position as a genuinely noninteractive preview: label it as such, expose one useful accessible description and do not create64 focusable cells or attach drag/premove handlers. A shared board-look fragment/static rendering can reuse appearance without impersonating a live match. The actual game keeps its semantic grid and stable-square behavior.
- Keep the sidebar170 geometry and mobile matchMedia900/game1200 synchronized. The1050 threshold refers to available content width; express that through a container/intrinsic layout or an explicit derived viewport formula, rather than accidentally comparing it to viewport width. Verify both sides of the transition with long HY strings.
- A desktop board height cap must account for both player bars, coordinates and gutters. Let the document and action panel scroll naturally if controls exceed the viewport; do not crop either player's bar or create an unlabelled nested scroll trap to force screenshot fidelity.
- Move existing opponent/self DOM nodes to the above/below-board positions; do not duplicate them for desktop/mobile. Preserve selectors used by play.js and the one-instance coordinate-label contract in board.js.
- Apply the revised palette and typography through the existing foundation/app owners. Do not add a third override stylesheet. Verify ghost buttons, error text/fills, help/dialog surfaces and inactive nav contrast as independent semantic roles.
- Keep active games before new-game actions in the action reading/focus sequence. Server counts should remain clearly aggregate counters; preview player bars must not invent opponents, ratings or clocks.
- Reuse the actual all-language/boundary, overlay, bot/friend/puzzle and keyboard regression checks after changes. Recompute contrast for the new green board and action palette; previous graphite/olive or brown-board ratios are not current evidence.

## Verdict
**APPROVED.** This is a concrete, bounded composition change matching the user's stated preference and compatible with the existing application. No unresolved architectural blocker requires another plan round. Final approval of the actual interface must follow updated screenshots and runtime verification, especially preview semantics, player-bar geometry and narrow-panel behavior.
