# Chess.com fidelity revision — Independent Product Review

## Materials and scope

Read `/tmp/checkers-chesscom/plan.md`. Actually viewed both `play.png` and `computer.png`, which show the real public Chess.com interface. Did not read other reviewers' conclusions or change application code. This is a review of the proposed board-and-menu revision, not a claim to have verified its implementation. The user's explicit choice of this composition is respected; the prior product/a11y/state contracts remain applicable.

## Scores

| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | The proposed proportions, neutral dark surfaces, large board and restrained action colour form a specific, coherent composition. Actual rendering remains to be reviewed. |
| Usability | 9 | Modes retain explicit actions and difficulty; active games stay before new starts, and actions precede the preview when space narrows. |
| Consistency | 9 | Home and /play share the same board-plus-actions model; actual games reuse the surrounding player bars while all routes keep one visual system. |
| Accessibility | 8 | Readable labels, controls, dark-on-green contrast and existing keyboard/overlay contracts are preserved. Green board states and narrow Armenian navigation still need real QA. |
| Originality | 6 | The user explicitly requested close visual fidelity. Own draughts pieces, product name, real content and limited feature set prevent misleading brand/function duplication, but the composition is deliberately borrowed. |
| Product fit | 9 | A board-led workspace suits a draughts product and matches the expressed preference without adding chess-only functions. |

## Three strengths

1. **The reference is translated into supported tasks.** Chess.com's board and adjacent menu provide the intended spatial model, while this product keeps only online, bot and friend modes. There is no invented coach, tournament, chess rating or live opponent data to make the copy look fuller.
2. **Action priority survives the larger board.** Active games precede new game choices; narrow layouts put working controls before the noninteractive preview. The board communicates the product without becoming a mandatory scroll obstacle to starting a game on mobile.
3. **The transition into a real game is more natural.** Opponent and self bars above/below the playable board establish ownership where users expect it. Status, actions and notation remain adjacent instead of competing with repeated player cards in a generic side panel.

## Critical issues

**No unresolved critical issue in the plan.** The proposal explicitly retains the previous interaction fixes and distinguishes the initial-position preview from a real ongoing game. It does not require removing routes or supported behaviour to mimic the source.

## Concrete implementation checks

- Make the preview's noninteractive status unambiguous at its title/near the board, not only in a tooltip. Do not label preview bars as if a real opponent had joined, show a turn prompt, fabricate usernames, or expose game-like click/focus affordances on it.
- In the right menu, keep online/bot/friend directly discoverable before the daily puzzle. The 320px column must allow visible difficulty labels and complete RU/HY button text. Natural wrapping is preferable to smaller type or clipped labels.
- Preserve one active-game entry per real game before modes and keep Resume distinct from confirmed resignation/cancellation. A long active list should remain compact; it must not become a stack of large imitation chess cards.
- At the stacking boundary, test the actual available content width, not just a nominal viewport. At desktop dimensions where the board and menu fit, both should be visible without an unnecessarily wide empty gap. Once stacked, do not leave a duplicate preview ahead of working controls.
- The green board changes the relationship between destinations, selected cells and last-move marks. Verify the existing shape-based distinctions, white/black piece rims and focus outlines against both new square colours; do not rely on green-on-green alone.
- Repeat the meaningful search-to-friend/bot/Join cancellation checks, modal focus checks and all-language responsive matrix after DOM repositioning. A single instance per data hook is essential to retain actual behaviour.

## Verdict

**APPROVED.** The plan adopts the specific board-and-menu layout the user requested while retaining the usability work already completed. It is suitable for implementation without another conceptual redesign. Final approval of the result still requires inspection of the actual board preview, responsive action ordering and preserved gameplay/state flows.
