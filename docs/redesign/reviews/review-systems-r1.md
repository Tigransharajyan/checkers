# Independent review — AGENT 3 / UI Systems Architect — round 1

Reviewed: design-review.md v1 in full; actual baseline screenshots home-1440.png, play-390.png, tasks-768.png. Read relevant source in lobby.js, common.js, tasks.js, head.html and board CSS selectors. No implementation was changed or tested by this reviewer. Other reviewers' findings were not consulted.

## Scores (proposed specification, not measured final UI)
| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | Coherent solid surfaces, bounded scale and appropriate board emphasis; final rendering is still unproven. |
| Usability | 8 | Explicit actions, simplified modes and earlier puzzle condition address concrete baseline weaknesses. |
| Consistency | 7 | Strong ownership proposal, but responsive contracts and preservation during CSS replacement need precision. |
| Accessibility | 8 | Explicit focus/error/dialog plan; component reflow and DOM order need a single implementation contract. |
| Originality | 7 | Appropriate distinct club character without introducing a separate asset system. |
| Product fit | 9 | Scope is genuinely draughts, preserving engine, invitations, guest sessions and three languages. |

## Three strengths
1. Consolidating tokens/foundations and page layouts while removing the third override layer directly addresses the observed maintenance failure. No new framework is necessary for this template/vanilla JS app.
2. One reusable mode fragment is appropriate: home and play-menu currently repeat nearly identical controls and singleton data hooks. Keeping one instance per page allows existing actions to survive without a backend rewrite.
3. Scope explicitly preserves board interaction features and real data; typography assets already support required languages. The proposed max widths prevent a 1920px board from becoming physically uncomfortable.

## Blocking plan issues — CHANGES REQUIRED

### S1 — Define responsive contracts using available component width, and synchronize behavior breakpoints (High)
The spec changes global drawer threshold from 768 to 900, retains game threshold1200 and opens lobby columns at viewport1040. The existing common.js still uses 768/1200. At viewport1040 with sidebar208 and content gutters64, the lobby has768px; subtract aside300/gap24 and the mode column is444px. Padding, icon, description, difficulty selector and a labelled action compete for substantially less room. A viewport-only <=600 rule does not cover this narrow desktop component. Armenian labels make this particularly plausible. The plan does not state how that intermediate case must reflow.

Required clarification before coding:
- Specify that mode controls wrap/stack by available row width (intrinsic grid/flex or container query) rather than relying only on the global600 breakpoint. State a no-truncation/no-hidden-label invariant and preserve description -> difficulty -> action reading/focus order.
- Define matching CSS and JS media-query contracts: ordinary900, game1200. JS should derive overlay state from the same documented matchMedia values; crossing to desktop must clear inert, traps, expanded and backdrop state.
- Include boundary QA at899/900/901,1039/1040/1041 and1199/1200/1201, in addition to the requested broad viewport matrix; use the longest HY strings.

### S2 — Add the runtime CSS/DOM preservation contract for the full stylesheet replacement (High)
The implementation plan replaces nearly all styling and removes theme.css, but the behavioral style rules are split across these files. For example theme.css ends with .piece.is-motion-source visibility and .board .piece.piece-motion positioning/compositor rules. Board JavaScript deliberately keeps source DOM nodes and adds motion/drag copies. Losing these rules can cause duplicate pieces or moving pieces outside the board despite unchanged JavaScript. Similarly tasks.js overwrites feedback.className with `task-feedback ok/bad` and relies on .hidden for initial/retry states. Merely preserving data-* hooks is insufficient.

Required clarification before coding:
- Make a compact inventory of retained behavioral selectors/classes and assign them to the board/overlay/state portion of app.css: .hidden; .piece-motion/.is-motion-source/.piece-ghost/.is-drag-source; .board-annotations; selected/last/premove/destination/capture/drag classes; feedback.ok/.bad; sidebar-open; modal visibility and assistant.is-open. Preserve geometry, positioning, z-index, pointer-events and DOM retention assumptions, not the forbidden visual effects.
- Explicitly state that each data-* singleton/ID appears once per rendered page, even when task condition/controls move. Use one task DOM reading order: condition/progress/feedback -> board -> actions, with desktop grid placement; do not duplicate sections for breakpoints.
- Existing game/puzzle browser tests are the right regression basis; add inspection of source/ghost visibility during movement and cleanup after resize/reduced-motion as meaningful checks, not screenshot pixel snapshots.

## Non-blocking recommendations
- A small shared overlay helper for focus/inert ownership is preferable to independent drawer, game-modal and assistant traps that can disable each other's trigger. If multiple overlays are allowed, document precedence; simplest is close assistant/drawer before opening game confirmation.
- Keep raw numeric spacing/type scales in foundations; component CSS should consume semantic colors and standard geometry rather than embedding local alternatives. A short ownership map in the saved design document prevents the deleted theme layer from reappearing later.
- Tables should have internal overflow only where data requires it. Do not use global overflow-x:hidden as a substitute for passing layout QA.

## Verdict
**CHANGES REQUIRED.** The direction itself is suitable and implementable in the current architecture. Two missing contracts—intermediate-width behavior and behavioral CSS preservation—are concrete regression risks of this particular consolidation. Both can be resolved in the specification before implementation; no build or mock implementation is being demanded before the approval gate.
