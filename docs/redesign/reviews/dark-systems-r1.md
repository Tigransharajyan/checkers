# Independent UI Systems Architect review — dark direction v3 — round 1

Read dark-direction.md in full, with the previously reviewed audit/v2 contracts as context. Viewed the actual user reference image at /home/tigran/.codex/attachments/1bab5691-b04c-4786-937e-cd136e001450/image-1.png. Read current shared overlay changes and checked hard-coded color sites in app.css. Did not read other reviewers' reports or change implementation.

This evaluates the proposed dark refinement before styling. The listed post-r1 corrections were inspected in source where relevant, but are not declared runtime-verified by this plan review.

## Scores
| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | Restrained graphite/olive foundations and the retained warm board have a clear hierarchy without the reference's decorative effects. |
| Usability | 9 | The proven task-first structure, resumption priority and explicit actions survive the palette change. |
| Consistency | 9 | The same tokens, fragments, component-width behavior and overlay contracts remain applicable across routes. |
| Accessibility | 8 | Text/focus values are concrete and state distinctions survive, but actual dark-state contrast still needs the planned instrumental check. |
| Originality | 8 | The reference informs density and atmosphere without importing chess figures, unsupported game types or fictional ratings. |
| Product fit | 9 | This is a draughts workspace suited to the user's explicit dark preference rather than a new landing-page aesthetic. |

## Three strengths
1. It changes foundations and density while retaining the existing functional architecture. It does not require a second theme implementation, runtime theme state, component duplication or new libraries.
2. The sidebar208/content1184/aside300 geometry and container-based selector contract remain stable. Moving real registered statistics to the aside is compatible with existing single-instance hooks and removes guest placeholders.
3. Distinct action/text roles are specified for accent, inverse, warning, success and danger. The board continues to use its own established state/geometry system, avoiding a broad regression to motion or keyboard behavior.

## Critical issues
None in the v3 specification. The direction is implementable within the current CSS ownership and template architecture.

## Concrete implementation requirements / non-blocking cautions
- Carry the palette through every component, not only :root. Current app.css contains a fixed light task-widget background (#eeeadf), fixed account/language/toast borders and overlay colors. Assign surface/component tokens or deliberate dark values; leaving a light puzzle surface with inherited light text would break the system.
- Separate danger foreground from filled danger action. Existing --danger is used both as text and as background. V3's quiet danger #FFB9AC and filled danger #8C302E cannot share the same semantic variable without one state becoming wrong. Use foreground/fill/hover/on-danger roles and verify input errors, text buttons, toast errors and confirmations independently.
- Online primary and its CTA should follow the final explicit rule: subdued surface-alt block with left accent and a green primary CTA. Reserve the pale inverse token for genuinely inverse contexts; do not carry the old filled pine block's inverse button treatment accidentally.
- Remove serif display overrides in both generic headings and task-widget/brand selectors. Keep the HY size overrides and local font coverage, while using the same sans-family system consistently.
- Preserve all runtime positioning, hidden/inert, stable-grid and ghost rules while changing visual declarations. No CSS selector rewrite is required for this refinement.
- Re-measure all foreground/background state pairs after applying the palette, including placeholder, readonly/disabled, keyboard focus, table rows, open FAQ and modal/assistant. Reuse the established language/breakpoint/scenario matrix; the old light contrast measurements do not validate the dark UI.

## Verdict
**APPROVED.** V3 is a bounded, coherent refinement that matches the user's explicit reference preference while preserving the approved system and useful UX changes. No unresolved specification blocker requires another plan round from this role. Final approval of the actual dark interface remains a separate post-implementation review with current screenshots and runtime verification.
