# Independent Art Director review — Chess.com fidelity plan

Read plan.md completely. Actually viewed play.png and computer.png through view_image. Earlier audit and approved functional/accessibility contracts are retained context. This is review of the new composition before implementation; I did not modify code or read other reviewers' conclusions.

## Scores for the plan

| Criterion | Score /10 | Rationale |
|---|---:|---|
| Visual quality | 8 | Concrete board-led proportions, stronger typographic weight and warm charcoal palette directly address the selected reference. |
| Usability | 8 | Explicit actions/difficulty and prior continuation/pending safeguards remain; mobile puts actions ahead of the noninteractive preview. |
| Consistency | 8 | Shared type, surfaces, boundaries and controls are specified across routes. Actual implementation still needs visual verification. |
| Accessibility | 8 | Readable text and dark ink on green avoid the reference's white-on-green problem; established focus/keyboard/overlay contracts remain. |
| Originality | 6 | This is deliberately a close visual-reference revision requested by the user, not an originality exercise. Own draughts content and mark remain. |
| Product fit | 9 | A real 8×8 draughts position as the main visual is strongly suited to the product. No unsupported chess/coach/tournament features are invented. |

## Three strongest aspects

1. **The composition now actually follows the selected product UI.** The reference's defining move is a large board beside a narrow action column, not simply dark background plus green buttons. A170px sidebar, board with player bars,320px menu and bounded gutters provide a concrete basis for that relationship. This is materially different from the preceding text-heavy lobby.
2. **Reference fidelity is balanced with functional honesty.** Initial position is explicitly labelled a noninteractive preview; real active games and actual daily puzzle remain distinct. The plan does not populate the screen with imaginary ratings, chess variants, bot personalities or lessons merely to imitate the screenshot's density.
3. **The strongest reference characteristics can survive the user's visual bans.** Charcoal#302e2b/#262421, solid neutral menu surfaces, bold functional titles and green actions create the intended familiar game environment without gradients, coloured shadows or character art. Existing flat figures and contrast rings are appropriate substitutes for borrowed chess assets.

## Critical issues

**No unresolved critical issues in the specified plan. APPROVED for implementation.**

The geometric relationships are sufficiently concrete, and the plan explicitly preserves the functional corrections from prior reviews. This approval does not certify the eventual result or require visual originality contrary to the user's selected direction.

## Implementation checks and nonblocking clarifications

- **Treat board/menu proportions as the main acceptance criterion.** Do not retain the old full-width title/modes composition and only swap colours. At1440 the real initial draughts position must dominate the left, with a visibly narrower independent action panel to its right. Place the page title in that panel as specified.
- **Cap the board by height as well as width on desktop.** Player bars and labels must fit around it without pushing the board's bottom off the screen. The right panel may scroll with document content; do not shrink44–48px controls to force puzzle/invite/history above the fold.
- **Keep action semantics visible inside the narrow panel.** A menu entry should not appear to be a giant interactive card while containing a different small nested button unless the whole activation behavior is deliberately coherent. In particular, bot difficulty must remain a normal operable select and must not accidentally start a game when adjusted. This is already protected by the plan's visible controls/preserved UX contract.
- **Sidebar170px requires honest multiline layout.** Long RU/HY labels may wrap. Preserve legible type and icon alignment, rather than reducing font size or clipping. Do not copy reference navigation categories that the product does not implement.
- **Use the three icon hues sparingly.** Green play, amber task and blue rules are functional category cues; repeated large coloured icons in every panel would recreate the earlier multi-accent noise. No decorative badges or pseudo3D assets are needed.
- **Retain monochrome shape distinctions on the new board colours.** Existing white/black figures, crown, selected square, capture ring and premove dashes should remain identifiable on#ebecd0/#779556. Actual simultaneous-state screenshots are more useful than assuming prior brown-board QA automatically transfers.

Calculated solid colour pairs from the proposed values: dark#18220e on green#81b64c ≈6.85:1; muted#c3c0bc on canvas#302e2b ≈7.47:1; boundary#8b8882 against menu#3d3b38 ≈3.16:1. These support the plan, but are not a substitute for browser accessibility checks of the final UI.

## Verdict

**APPROVED — design plan only.** The specified revision is a credible interpretation of the explicitly selected Chess.com board-and-menu layout while retaining the product's draughts identity and prior UX safeguards. Post-implementation review should compare the actual board/action-column relationship, not merely colour similarity.
