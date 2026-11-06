# Round 2 — Independent product design review

Reviewer: Agent 1, Senior Product Designer. Date: 2026-11-06.

Evidence: revised `docs/redesign/chesscom/pages-revision.md`; current route templates reviewed in round 1, with launcher fragment and cited lobby matchmaking code checked again; current `final-desktop.png` and actual Chess.com `play.png` inspected again. No other reviewers' reports read. This is a pre-code review, not a usability-test report or certification of rendered results.

## Scores

| Dimension | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | Explicit page compositions, dimensions and reading measures now make the intended hierarchy assessable; rendered execution remains unverified. |
| Usability | 9 | Home starts online play directly, Play exposes concrete mode actions, and returning users receive resume priority. |
| Consistency | 9 | Shared identity, navigation and component ownership coexist with clearly justified page layouts and game shell. |
| Accessibility | 9 | Source order, static preview semantics, focus behavior, announcements, contrast and zoom requirements are substantially more concrete. |
| Originality | 6 | Intentionally familiar reference-driven styling, with appropriate checkers identity rather than copied chess features. |
| Product fit | 9 | Existing local actions and real data govern the design, including guest, new-account and failure states. |

## Three strongest aspects

1. **Home and Play now have reviewable first-screen contracts.** Home places real continuation first when available and otherwise offers an immediate online start beside a small actual daily task. Bot/friend links lead to their corresponding launcher rows. Play has working separated mode rows and a noninteractive board only on wide screens. This materially improves the current screenshot, whose oversized home board pushes meaningful secondary tasks down the page, without adding an unnecessary step to online play.
2. **Async behavior is treated as part of the interface.** Active sessions, recent results and daily task fail independently; failure does not masquerade as absence. Queue connecting/searching/cancelling, local submit busy states and field-specific joining errors have explicit presentation and accessibility contracts. Existing matchmaking source has mode-change/cancel race handling, and preserving it is an appropriate constraint.
3. **Truthful data presentation now extends to exact edge cases.** Guests do not receive empty statistic grids or fabricated archive rows; zero-game accounts show actual zeros and no invented 0% win rate; unavailable data gets retry. The proposal discloses the lack of authenticated Chess.com screenshots and avoids claiming a pixel match for unobserved private states.

## Resolution of my prior refinements

- Home priority, independent active-game errors and retry: resolved explicitly.
- Unambiguous Play actions: resolved by separate named mode actions rather than a mutable generic CTA.
- Static preview mistaken for live play: resolved with initial-position label, image semantics and no Tab stops.
- Puzzle condition placement: resolved in both DOM and visual order.
- Lobby navigation versus resignation: resolved; complete game navigation and locale/account access are specified.
- Undefined zero-game win rate and source explanation: resolved.
- Accessible mobile replay action: resolved through labelled wrapped rows and visible replay access.

## Critical concerns

None remaining at the concept/specification gate. The updated document is sufficiently concrete to implement and verify. Approval does not imply that the current code already implements its state matrix.

## Specific execution notes / fixes

These are bounded implementation checks, not blockers to concept approval:

- On online start, move keyboard focus from the disappearing Find opponent button to reachable Cancel or an equivalent stable control; after cancellation restore it to Find opponent. A live announcement alone does not solve focus left on a hidden button. Keep this local to the queue and preserve existing match/cancel race behavior.
- Home Bot/Friend links should preserve locale and target stable launcher row anchors. Where browser anchor navigation does not focus a meaningful element, use a focusable row heading so the arrival context is clear without automatically starting a game.
- Reconcile the desktop board/rail coordinate targets with the stated height constraint by treating the coordinates as the 1440×1000 reference case. At 1280×720 shrink the board without introducing an unexplained fixed empty gap merely to keep the rail at x948.
- An invitation successfully created should expose the real link, Copy and Open game, as the current fragment supports. Prefer focus on that new actionable region over a generic return to its original submit. Preserve the entered code/difficulty on failures.
- Render a concise explicit zero-game explanation consistently on both Home and Progress; do not let “stats only when available” hide confirmed zero statistics or turn an unavailable account response into a guest claim.

## Verdict

**APPROVED** for implementation of revised concept A.

The revision resolves my previous concerns and strengthens first-screen task flow and error honesty. It preserves a recognizable shared system while separating overview, launcher, live game, puzzle, personal progress, archive and reading/form tasks. No new critical product gap warrants another pre-code redesign round. Final approval of execution still depends on the specified rendered-state, responsive and keyboard verification, with no user-testing claims inferred.
