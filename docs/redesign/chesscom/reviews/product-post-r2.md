# Product post-review — round 2, DOM order and Armenian navigation

## Materials and independent checks

Read `post-r2.md`, retaining the common `plan.md`/`post.md` materials and my preceding actual-interface observations. Viewed the updated real `after/home-hy-1440.png`. Inspected the current home/play templates and relevant grid/sidebar CSS. Did not read other reviewers' verdicts or edit code.

Ran a read-only Playwright check against localhost for home and /play, both in Armenian, at390×844 and1440×844. In all four cases, focusing #main-content then pressing Tab focused [data-find-random] directly. scrollY remained0→0 in every case. Document widths matched viewport widths390/1440. At desktop the sidebar measured200px and the actual grid remained two-column848px+320px; on mobile it remained one column366px with the closed drawer using its separate mobile width. This verifies the corrections against the actual rendered UI rather than source order alone.

## Scores

| Criterion | Score / 10 | Reason |
|---|---:|---|
| Visual quality | 8 | The requested board-left/menu-right composition survives the DOM correction; the wider HY sidebar allows whole labels without reducing type. |
| Usability | 9 | Keyboard users now reach the main action before lower history links, matching the visible mobile action priority and avoiding a scroll jump. |
| Consistency | 9 | Home and /play use the same logical action-first order and explicit desktop grid placement. |
| Accessibility | 9 | Actual focus order matches the task hierarchy without positive tabindex or duplicate hooks; Armenian labels remain readable. This is still not an AT user test. |
| Originality | 6 | The interface deliberately follows the user's requested Chess.com-like layout while keeping draughts content and own identity. |
| Product fit | 9 | Both fixes improve the chosen product experience without adding unsupported functions or changing game logic. |

## Three strengths

1. **Task-first order now includes keyboard access.** The first control reached from main is Find opponent on both routes and both tested widths. It no longer requires traversing lower history content or changing scroll position.
2. **Visual fidelity and logical order coexist.** Explicit columns keep the large board on the left and320px menu on the right at desktop, while mobile keeps working actions before preview/history. The correction does not compromise the layout the user requested.
3. **Armenian is accommodated structurally.** The200px sidebar gives long navigation words adequate space at the same16px label/28px icon scale. The updated screenshot shows complete progress/history words, and the board/menu still fits at1440px.

## Closure and critical issues

- **Actions before preview/history in DOM — closed.** Confirmed in both templates and in four actual Tab checks with unchanged scrollY.
- **HY orphan letters in navigation — closed.** Confirmed by the current screenshot and computed desktop sidebar width. No truncation or smaller text was introduced to hide the issue.
- **New critical product problems — none found.** The changes are limited, preserve one data-hook instance per control and retain the existing interaction protections.

## Limits and nonblocking observations

The static preview still adds mobile page length, as deliberately selected. The working actions come first, so this remains a nonblocking tradeoff. The existing final test log reports8/8 passed; I do not infer that my read-only focus check substitutes for every behavioural regression or screen-reader testing. The wider HY sidebar should remain in the full responsive matrix at neighbouring breakpoints, but no failure was observed at the tested390/1440 widths.

## Verdict

**APPROVED.** Both round-two corrections are verified in the actual UI. The result keeps the requested visual composition while making its keyboard sequence and Armenian navigation more usable. There is no unresolved critical issue in my Product review.
