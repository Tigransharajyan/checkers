# Independent Accessibility & Usability review — Chess.com fidelity revision

Read `/tmp/checkers-chesscom/plan.md`; personally viewed both actual reference images `play.png` and `computer.png` through view_image. This review concerns the concrete plan before implementation, not a claim that revised screens have been tested. No application code changed; no other reviewers' conclusions read. Earlier proven keyboard/grid/overlay/pending fixes remain mandatory as stated in the plan.

## Scores

| Criterion | /10 | Basis |
|---|---:|---|
| Visual quality | 8 | Board-led proportions and restrained warm charcoal provide a coherent game workspace. |
| Usability | 9 | The user's requested composition is concrete; labelled actions and mobile actions-before-preview preserve the short start path. |
| Consistency | 9 | Palette, typography and shared controls propagate to all routes, retaining existing interaction contracts. |
| Accessibility | 9 | Specified foreground/boundary pairs pass AA, text remains 16/14px, controls remain 44–48px and functional board accessibility is retained. |
| Originality | 6 | Deliberately close to the requested external reference; own draughts identity and actual functionality remain honest. |
| Product fit | 9 | Real draughts board, difficulty, invitations and daily puzzle belong naturally in this composition. |

## Three strengths

1. **Layout fidelity is separated from inaccessible details of the reference.** The reference uses white labels on a relatively light green and contains decorative assets. The plan explicitly substitutes dark text on green, retains readable descriptions and excludes gradients/glow/borrowed assets.
2. **Mobile start remains actionable.** Controls stack before the noninteractive preview, with difficulty and visible button labels retained. A large desktop board does not become a mandatory scroll obstacle before starting a game on a phone.
3. **Important accessible behavior survives the revision.** Real game grid semantics, stable focus, response announcements, safe overlays and error-linked forms are preserved. The board's changed field colors still give strong piece separation with existing contrasting rims.

## Computed color checks

Calculated WCAG relative luminance from the specified HEX values:

| Pair | Contrast |
|---|---:|
| #F7F6F5 on canvas #302E2B | 12.54:1 |
| Muted #C3C0BC on panel #3D3B38 | 6.16:1 |
| Boundary #8B8882 on panel #3D3B38 | 3.16:1 |
| Boundary #8B8882 on canvas #302E2B | 3.83:1 |
| Primary text #18220E on #81B64C | 6.85:1 |
| White #F7F6F5 on #81B64C (rejected variant) | 2.23:1 |
| White piece #FFFAEA on dark field #779556 | 3.23:1 |
| Dark piece/rim #17241F on dark field #779556 | 4.75:1 |

These support the planned choices. The input boundary has only moderate margin above 3:1 on panel: hover/disabled styling must not introduce a lighter adjacent surface without recalculation. Ordinary text must not inherit the reference's white-on-green combination.

## Critical issues

No unresolved blocker at plan stage. The user's selected board-plus-menu composition is compatible with the existing accessible behavior and stated constraints.

## Specific implementation and verification requirements

- **Make preview truly noninteractive semantically.** The lobby initial position should be one labelled `role=img`/figure (for example “Starting position of Russian draughts. Preview”), with child cells hidden from AT and no tab stops. Do not expose the 64-cell interactive grid or “your turn” status for a preview. The actual game retains its interactive grid. This is the operational meaning of the plan's explicit noninteractive-preview requirement.
- Put the primary heading and actions in sensible DOM reading order. Mobile actions-before-preview should match reading/focus order, not merely reverse them visually with CSS. On desktop a visually left preview can follow the action heading in DOM without harming layout.
- Player bars around the lobby preview must not imply a real matched opponent or active turn. Use honest initial-position context and actual account information only. During a real game, opponent/self bars stay linked to actual server players.
- The 170px sidebar must allow Armenian labels to wrap naturally. Check 320px, 390px, 900/901px and the content stacking boundary in all three languages, at enlarged text; no clipped labels or hidden menu options.
- Height-based board sizing must not clip player bars or reduce labels/controls. Short desktop windows should permit normal page scroll. Keep dual outline selected/focus/destination states on both new field colors and forced-colors mode.
- Repeat the existing keyboard move, ACTIVE puzzle live response, drawer/backdrop/focus return and confirmation-dialog tests after moving DOM around. Board placement changes must not detach focused nodes or introduce duplicate hooks.

## Verdict: APPROVED

The concrete plan can proceed. It follows the user's explicit requested composition while keeping contrast, labels, responsive priorities and established accessible interaction contracts. Approval is for the plan; final accessibility still requires real screenshots and scenario checks after implementation.
