# Independent product design review — page revision

Reviewer: Agent 1, Senior Product Designer. Date: 2026-11-06.

Scope: pre-implementation concept review of `docs/redesign/chesscom/pages-revision.md`, current route templates index/play-menu/play/tasks/rating/history/faq/login/register, and the supplied final-desktop.png and real Chess.com play.png. No other reviews consulted. Scores judge the proposed specification; they are not claims about a completed UI or user testing.

## Scores

| Dimension | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | Coherent restrained palette, clearer typographic hierarchy and purposeful surface types. Final composition is still specified in prose, so execution quality remains unproven. |
| Usability | 8 | Separates returning-home, starting-play, solving and playing; keeps existing real actions and improves empty states. |
| Consistency | 8 | Shared navigation, typography and state vocabulary hold the route-specific layouts together; game header is a justified contextual exception. |
| Accessibility | 8 | Explicit keyboard preservation, visible focus, target sizes, reduced motion and locale wrapping. Responsive board/action reachability must be verified in implementation. |
| Originality | 6 | Deliberately familiar Chess.com spatial and color language; appropriate adaptation to checkers rather than a distinctive new visual identity. |
| Product fit | 9 | Prioritizes the implemented checkers tasks and real available data while rejecting unsupported leaderboard, coach and progression features. |

## Three strongest aspects

1. **Corrects a visible task mismatch.** The current screenshot makes a very large initial-position board the home-page focal point and pushes the daily task below the first screen. The current index and play-menu templates share the position preview and mode selector. Removing that preview from home and giving Play an explicit launch role creates a meaningful destination difference, rather than merely changing decoration.
2. **Uses the reference selectively and honestly.** The real Play screenshot supports board-plus-mode-list as a launch composition, but its coach, tournaments and leaderboard are not automatically valid local features. The proposal takes its hierarchy and familiar game surfaces while retaining local bot difficulty, invitations, online cancellation and daily-task logic.
3. **Treats unavailable data as a product state.** Guest statistics/archive screens receive explanatory content and relevant actions without fabricated games, invented competitors or a synthetic chart. Separating loading, error, empty and guest states is materially more informative than a large panel containing dashes.

## Critical concerns

None that require rejecting this concept before implementation. The proposal identifies the major observed structural defects and sets credible constraints on the replacement. Accessibility and interaction claims remain implementation acceptance criteria, not established outcomes.

## Specific implementation refinements

- Give every home state a stable priority: actual active game first, then Start a game, then daily task and recent results. If active-game retrieval fails, show a compact retry/status instead of implying that no game exists. Keep the rest of home usable.
- On Play, make the selected mode and the target of the single primary CTA unmistakable. Online search must immediately expose searching/cancel; bot difficulty and invitation entry must remain attached to their corresponding modes. Avoid a generic primary action whose effect changes without a visible selection.
- Label a noninteractive board preview as an initial-position preview and keep it outside the keyboard interaction sequence. Its visual resemblance to the live board should not suggest that clicking pieces starts play.
- Keep the task condition visible before or immediately adjacent to the board on mobile. The current task template already separates context, board and answer controls: resizing should not move the instruction beneath a full-screen board or make the practice link visually outrank solving/reset feedback.
- Make the game header's Lobby destination text explicit and preserve locale and the real resume path. Leaving the game surface must not silently represent resignation; use existing resignation confirmation only for that action.
- Define zero-game statistics distinctly from unavailable statistics: zero completed games is real data, but win rate is undefined rather than 0%. Explain which completed games contribute to statistics using the actual source behavior.
- Preserve clear visible row-level replay actions in archive, including on narrow screens; an accessible horizontal table region is already present in the current template and should not be lost during visual densification.

## Verdict

**APPROVED** for implementation of concept A.

The selected direction objectively improves task hierarchy and route separation relative to the supplied current screenshot and templates. It retains predictable common navigation while allowing the board to dominate only where playing or solving requires it. The refinements above specify execution details within the selected direction; they do not require changing the concept. Approval is limited to this pre-implementation plan and does not certify final visual fidelity, keyboard behavior, responsive fit or usability testing results.
