# AGENT 6 — Anti-AI Red Team
## Independent pre-implementation review — Chess.com fidelity revision

Read `/tmp/checkers-chesscom/plan.md`. Actually viewed both public browser screenshots through view_image: `play.png` and `computer.png`. No other reviewers’ conclusions read, no application code changed. This assesses the proposed revision, not an implemented result. The user's explicit choice of the board + menu composition is accepted; I am not re-litigating the previously rejected direction.

| Criterion | Score /10 |
|---|---:|
| Visual quality | 8 |
| Usability | 8 |
| Consistency | 8 |
| Accessibility | 8 |
| Originality | 6 |
| Product fit | 9 |

## Three strongest aspects
1. **The requested visual relationship is concrete.** The defining reference is the large board on the left, narrow action column on the right and compact persistent sidebar—not merely green buttons on a generic dashboard. The revision now reproduces that composition using a real draughts position, so it addresses the user's rejection directly.
2. **It avoids importing the reference's unsuitable baggage.** No invented coaches, tournaments, rating ladders, advertisements, themed bot portraits or chess assets. No gradient/shadow effects are required to achieve the neutral dark surface hierarchy. This prevents a plausible-looking but fictitious product from replacing the working checkers application.
3. **The menu remains functional rather than becoming screenshot decoration.** Online/bot/friend retain explicit controls, difficulty stays visible, active games precede starting another, actual counts/history are conditional, and mobile puts actions before the large preview. A labelled noninteractive initial position distinguishes the lobby board from a live game.

## Critical issues
None found in the specification. The direct visual influence reduces originality, but this is an explicitly requested fidelity revision and is not itself a defect. Existing access, localization and interaction contracts are preserved in the plan. They must be verified in the actual implementation rather than assumed from this approval.

## Concrete boundaries for implementation and post-review
- **Preview honesty:** keep the noninteractive label visible and clear in all three languages. Do not add fake opponent names, timers, selection highlights, live status or ratings around the lobby's static initial position. Reserve opponent/self bars with actual identities and turn state for a real game. The preview should not acquire a misleading keyboard grid role or dead interactive targets.
- **Avoid a substitute feature-card wall:** three real mode choices are justified. Do not turn the 320px column into six or more uniform tall cards copied from the reference. Keep bot difficulty/CTA and invitation state grouped with the relevant action; the daily puzzle is the only additional real board preview.
- **Visual bans remain intact:** the reference screenshots contain shaded/gradient buttons and, in computer.png, themed imagery. Do not reproduce these details. Flat neutral surfaces, actual boundaries and controlled green accents are sufficient. Do not import Chess.com branding, illustrations or logo.
- **No artificial board decoration:** preserve flat rings/crowns and useful state shapes. The new green board does not justify glossy pieces, wood texture, glow, pulsing or new decorative SVGs.
- **170px sidebar:** verify long Armenian/Russian labels at their actual 18px size. Let text wrap and sidebar scroll; do not solve the narrow rail by tiny typography, ellipsis or missing labels. This is a post-render geometry check, not a reason to block the specified composition.
- **Vertical fit:** when capping the board by desktop height, include the real player bars, coordinate labels and keyboard instruction in the available-height calculation. Do not compress these to microtext or shrink the board excessively on short laptop windows. Preserve normal scrolling where needed.
- **State contrast:** green primary text is deliberately dark for AA. Check actual focus, neutral raised buttons, error/warning and disabled states on the new backgrounds. Do not retain old cream/inverse/toast colors by accident from prior revisions.

## Final verdict
**APPROVED — implementation plan.** The revision clearly follows the user's selected board-led reference while remaining an honest, functional draughts product and respecting the visual restrictions. No unresolved critical issues at the plan stage. Final visual acceptance requires real rendered desktop/mobile/game screenshots and preserved interaction checks; this is not advance approval of unseen code.
