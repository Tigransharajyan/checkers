# Accessibility & Usability — actual revision, post-review round 2

Read `post-r2.md`, retained plan/post materials and personally viewed refreshed `after/home-hy-1440.png`. Previously viewed all requested current-layout screenshots. Rechecked current localhost independently in Chromium390×844: main AX snapshot, preview semantics, focus order on both home and /play. No code edits and no other reviewers' reports read.

## Scores

| Criterion | /10 | Basis |
|---|---:|---|
| Visual quality | 8 | Selected board/menu proportions retained; wider HY navigation no longer breaks labels into orphan characters. |
| Usability | 9 | Primary actions are now first both visually on mobile and in actual focus order. |
| Consistency | 9 | Home and /play use the same corrected ordering without duplicate controls. |
| Accessibility | 9 | The concrete focus/reading-order regression is closed; preview remains noninteractive and earlier functional checks remain valid. |
| Originality | 6 | Deliberate reference fidelity, as explicitly requested by the user. |
| Product fit | 9 | Real draughts board and actual game paths match the user's selected layout. |

## Three strengths

1. **Reading and keyboard order now match the start task.** Main AX snapshot begins with the game-selection heading and actions, followed by preview/history. Focus no longer visits the bottom of the page before the primary action.
2. **Preview semantics remain honest.** Independently found one labelled role=img, zero focusable descendants and zero interactive grids on home. The functional game board is still separate.
3. **Armenian navigation is visibly improved.** Refreshed actual screenshot shows full progress/history labels without the previous orphan letters. The adjustment preserves visible labels and icon sizes rather than hiding information.

## Closure of blocking issue AX-POST1

Directly repeated main.focus → four Tabs at390×844 on both home and /play. Home now produces:

1. «Найти соперника», scrollY0;
2. difficulty select, scrollY0;
3. «Играть», scrollY0;
4. «Пригласить друга», scrollY0.

/play follows the same action-first sequence. Main AX snapshot likewise starts with h1 «Играть в шашки», mode headings, labelled controls and invite form. Preview/history follow. The desktop screenshot retains board-left/menu-right geometry through CSS placement. No positive tabindex or duplicate hooks were needed. This directly closes my previous High finding.

Actual primary remains48px high with computed #18220E on#81B64C. The preview stays lower on mobile, arounddocument y1162 in the independently checked state, so it does not delay access to the start action.

## Remaining critical issues

None found in this round. Earlier independently verified puzzle live response, stable grid focus, drawer outside-click/return-focus and linked form errors remain applicable because this correction changes lobby ordering and locale sidebar width, not those implementations.

Nonblocking refinement retained from previous report: the preview's full explanatory sentence is repeated as region name, image name and paragraph in AX. A shorter containing region name would reduce repetition, but this does not obstruct completing a task or warrant holding this revision.

No claim of physical-device testing, speech-output screen-reader testing or comprehensive WCAG certification. Own new checks specifically target the reported regression, rather than inferring its closure from test counts.

## Verdict: APPROVED

The only blocking finding from my previous actual-interface review is closed by observed browser behavior and AX order. HY navigation readability also improved. The requested desktop composition is preserved, while mobile keyboard navigation now follows the actual main task without a scroll jump.
