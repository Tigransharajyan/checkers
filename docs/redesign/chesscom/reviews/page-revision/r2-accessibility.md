# Round 2 — independent accessibility and usability review

**APPROVED for implementation of the revised plan.** No unresolved critical architectural accessibility issue remains. This approves the specification; runtime keyboard, screen-reader, responsive and contrast acceptance checks remain required before UI delivery.

Reviewed the updated `docs/redesign/chesscom/pages-revision.md`, current PageController, shared navigation/templates, board and overlay code, current local `final-desktop.png`, and supplied real Chess.com `play.png`. No other reviewer reports were read. Code remains unchanged; screenshots show the existing UI rather than the proposed route-specific implementation.

| Criterion | Score / 10 |
|---|---:|
| Visual quality | 8 |
| Usability | 9 |
| Consistency | 9 |
| Accessibility | 9 |
| Originality | 7 |
| Product fit | 9 |

## Three strengths

1. **Reachability is now an explicit shell contract.** The focused game retains named Lobby navigation and a complete menu with every section, locale and account action. Skip links, main landmarks, current-page indication, correct `aria-controls`, initial focus, Escape, inert background and return focus are specified. Reusing the existing shared Overlay manager is credible: the inspected implementation already supplies trapping, inertness and restoration.
2. **Reading order and meaningful feedback are defined per route.** Tasks require the objective before the interactive board in both DOM and visual order, with feedback/actions after it and described instructions. The launcher preview is one noninteractive labelled image; home actions precede its small daily preview. The announcement matrix specifies queue/retry, coordinates/turn, mandatory capture, draw/result, task feedback and API errors without stealing board focus or rereading notation.
3. **Responsive access has concrete safeguards.** Ordinary controls retain 44px targets while board squares are explicitly treated as a spatial exception with single-tap and keyboard selection. Natural page scroll takes priority over clipped controls or tiny height-constrained boards. The plan requires short landscape, keyboard-open mobile, safe areas, long Armenian/Russian strings, 200% text, 400% zoom and reduced-motion checks. This directly addresses weaknesses that cannot be assessed from the two desktop screenshots.

## Critical issues

**None in the revised architecture.** The previous boundary-contrast, navigation, source-order, announcement and target-sizing ambiguities are resolved by explicit requirements and acceptance gates.

Measured token checks support the proposed control fix: `#8b8882` against raised `#3d3b38` is approximately **3.16:1**, and against canvas `#302e2b` **3.83:1**. Dark `#18220e` labels on action `#81b64c` remain approximately **6.85:1**. The plan correctly reserves the weaker divider token for decoration and requires interactive adjacent-pair verification.

## Concrete implementation checks and refinements

These are verification details within the approved scope, not additional architecture blockers:

- Keep the **two-tone board focus indicator** from current CSS. `#abd573` alone against dark board square `#779556` is only approximately **2.01:1**; the plan's requirement of measured ≥3:1 focus and shape/outline differentiation must be fulfilled by the actual composite treatment. Do not replace the current board treatment with a generic green ring. Check light squares, dark squares, selected squares and pieces separately.
- Build static launcher and daily previews without initializing `BoardUI.create` on them: the current renderer assigns grid semantics and roving square tab stops. Their specified `role=img`/single label must remain true after scripts run.
- Verify both orientations of board arrow navigation, Enter/Space selection, multi-capture, premove and rerender focus retention. Current board code supports roving grid cells and coordinate/state labels; keeping the data hook alone does not prove these behaviors survive the layout refactor.
- At short heights and zoom, ensure the height-based preferred board size yields to usable squares and document scroll, as the plan directs. Open drawers/dialogs and focus the final action with the mobile keyboard visible; the last control must remain reachable above safe areas and fixed navigation.
- Exercise error recovery independently in home active/recent/task regions, progress/history, queue and invite forms. Preserve values and retry focus; never interpret a failed request as an empty result. Keep live-region nodes stable through those transitions.

## Reason for approval

Direction A now preserves the familiar navigation system while giving home, launcher, game, puzzle, archive and reading pages clear roles. The exact specification provides enough access behavior to implement and review without inventing missing interaction rules. The supplied Chess.com screenshot remains a composition reference, not accessibility evidence. Final accessibility approval of the running UI depends on the stated interaction and responsive gates; pre-code approval is **APPROVED**.
