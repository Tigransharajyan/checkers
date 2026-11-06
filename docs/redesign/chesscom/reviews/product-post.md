# Actual Chess.com-layout revision — Independent Product Post-review

## Materials and direct observations

Read `plan.md` and `post.md`. Actually viewed the current after/home-1440.png, home-390.png, game-registered-1440.png, game-registered-390.png, play-hy-390.png, tasks-390.png, login-390.png and assistant-1440.png. The public reference play.png/computer.png had been directly viewed during the preceding plan review. Did not read other reviewers' conclusions or edit application code.

Additional read-only verification at localhost: home at390×844. The online button is at y257–305; the static preview starts at y1162, so the usable action precedes it on mobile. The preview is role=img with the explicit localized label «Выберите режим игры, чтобы начать партию. На доске — начальная позиция.» and contains zero focusable cells/buttons/links. Document width is390. After a real scroll to the invitation action, Join is at y388–436, above the fixed navigation at y776. Saved and viewed `product-home-scroll-390.png`; the form is reachable and readable, not permanently hidden by the navigation.

Read completed logs rather than assuming ongoing jobs passed: tests.log reports8/8 browser tests passed; package.log BUILD SUCCESS; matrix.log384cases with no failures/runtime errors; scenarios.log reports real bot/history, invitation, puzzle, auth-error and independent-session matchmaking flows with no runtime errors. accessibility.log reports58states with no violations but retains incomplete contrast cases. These are provided QA results, not an assertion that I personally repeated every flow. No user study, real-device or screen-reader session was performed by this reviewer.

## Scores

| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | Actual proportions clearly establish a large board on the left and narrow mode menu on the right; neutral dark panels and green squares/actions now follow the requested reference. |
| Usability | 9 | Modes have explicit buttons and visible difficulty; mobile actions remain first, invitation form is reachable, and existing lifecycle/state protections are retained. |
| Consistency | 9 | The preview, actual game, puzzle and auth screen share the same palette, typography and controls. Actual player bars sit naturally above/below the playable board. |
| Accessibility | 8 | The preview is honestly labelled and absent from the Tab sequence, errors/labels and keyboard guidance remain legible. Full AT compliance is not inferred from automated results. |
| Originality | 6 | Composition and palette deliberately closely reference Chess.com as requested; own draughts content, mark and supported functionality remain distinct. |
| Product fit | 9 | This is recognizably a board-game workspace rather than a generic dashboard, and the selected layout visibly addresses the user's explicit request. |

## Three strengths

1. **The requested composition is now present in the actual UI.** Home-1440 has the large green board, narrow right play menu, compact left navigation and nearby player bars seen in the reference. This is a structural revision, not another palette-only adjustment. The playable game uses the same spatial language without importing chess-only features.
2. **The preview does not masquerade as an ongoing game.** Its visible top label says «Начальная позиция», its caption tells users to choose a mode, and its accessible role/label communicates the same fact. There is no fabricated opponent or turn state, and users do not Tab through a fake playable grid.
3. **Responsive ordering protects the core task.** On mobile, users reach Find opponent immediately, then bot difficulty/Play and Invite friend, before the large preview. Armenian labels remain whole and readable in the supplied mobile screenshot. My scroll check confirms that the invitation form remains usable despite the fixed bottom navigation.

## Critical issues

**No unresolved critical product issue found.** The actual implementation follows the approved board-and-menu plan and the user's stated preference while preserving the earlier Resume, confirmation, search-cancellation, form and keyboard contracts. No supported core path appears to have been removed to achieve visual fidelity.

## Nonblocking observations and practical limits

- The static preview is intentionally prominent on desktop and adds substantial page height on mobile. Actions precede it, so this does not block starting a game. In future mobile refinement it could be collapsed after the modes, but reducing it is not required for this user-selected revision.
- The 320px menu naturally wraps descriptions more than the previous wide list. Buttons remain explicit and readable; keep testing long RU/HY strings rather than shrinking type to preserve a one-line ideal.
- Daily task now follows the modes and invitation form. It remains reachable but no longer competes with the user's primary play choice. This is a defensible hierarchy for the selected composition.
- Long full-page captures include fixed navigation at the screenshot viewport position. The captured bisecting bar alone does not prove inaccessible content; the actual invitation scroll check demonstrates a reachable state. Other supplied browser regressions remain the evidence for the remaining controls.
- Automated accessibility incomplete cases should be disclosed and manually assessed in the final QA report. Do not describe zero automated violations as a real assistive-technology session.

## Verdict

**APPROVED.** The delivered interface materially matches the requested Chess.com-like board-and-menu layout, keeps the product specifically about Russian draughts, and retains the usability improvements already established. The observed preview semantics, action ordering and reachable invitation form support approval beyond compilation alone. No further conceptual redesign is needed from the Product perspective.
