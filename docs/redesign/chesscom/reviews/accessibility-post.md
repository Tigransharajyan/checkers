# Accessibility & Usability — actual Chess.com-layout revision

Read `plan.md` and `post.md`; personally viewed reference `play.png` and all requested actual screenshots: after/home1440/390, game-registered1440/390, home-hy1440, tasks390, login390 and assistant1440. No other reviewers' reports read, no application code changed. Independently exercised current localhost in Chromium, including AX snapshots, preview focusability, mobile Tab order, a real puzzle move, drawer outside click and form errors. No speech-output screen reader or physical-device testing claimed.

## Scores

| Criterion | /10 | Basis |
|---|---:|---|
| Visual quality | 8 | Board-led charcoal/green composition is materially closer to the selected reference and remains legible. |
| Usability | 8 | Primary mobile action is visibly early, but actual keyboard/reading order contradicts that priority. |
| Consistency | 8 | Board/player bars and neutral controls align across pages; Armenian sidebar wrapping is somewhat strained. |
| Accessibility | 8 | Grid, live response, labels and overlays survive, but the home focus sequence regresses. |
| Originality | 6 | Deliberate reference fidelity matches the user's explicit preference; own identity and real functionality remain. |
| Product fit | 9 | Actual draughts position plus menu fulfils the user's board-and-menu request without fabricated chess features. |

## Three strengths

1. **Static preview is honestly represented.** Own Chromium check found one labelled role=img, no interactive grid on the home page and zero focusable descendants within the preview. Its text explains that this is the starting position rather than a live game. Actual game retains the interactive board.
2. **Functional accessibility fixes survive the visual revision.** Repeated `g5–f6` puzzle move entirely with keyboard received real reply `e5:g7`; focus stayed at `f6`. Exactly one meaningful live update reported both moves, next turn and progress1/4. Drawer backdrop remains non-inert/aria-hidden/tabIndex-1, outside tap closes it and focus returns. Auth still marks/associates/focuses the first error.
3. **Primary contrast and touch geometry are sound.** Actual mobile primary is 48px high with computed #18220E on #81B64C (6.85:1). The board has clear opposing piece rims on new green fields. The mobile action panel appears before the preview visually, as requested.

## Blocking issue

### AX-POST1 — High: mobile home keyboard and reading order starts at the bottom, then jumps back to the primary action

The DOM still contains `.dashboard-main` (preview, site counts, recent games) BEFORE `.dashboard-aside` (h1 and game actions), while mobile CSS reverses only their visual order using `.dashboard-aside { order:-1 }`.

Independently reproduced at390×844, fresh guest home, focus main then press Tab:

1. “Все партии”, scrollY1295;
2. “Войдите, чтобы сохранять следующие партии.”, scrollY1295;
3. “Найти соперника”, scrollY0;
4. difficulty select, scrollY0.

The primary action is visually around y257, but keyboard users first jump over a screen down to history/login before being sent back up. The actual main AX snapshot likewise announces preview and recent-games heading before the page h1/game actions. This is exactly the reading/focus-order risk identified in my plan review and is an interaction regression, not an aesthetic preference. Automatic axe does not detect the contradictory meaningful sequence.

**Fix:** place the action/heading block before preview/history in DOM; explicitly position it in the right desktop grid column and the preview in the left column. Keep active games first. Avoid positive tabindex and duplicated markup. Prefer splitting preview and recent into their own grid areas if necessary, so desktop geometry is preserved and mobile DOM order becomes active games → heading/actions/invite/daily task → preview → recent. Update /play menu consistently even though it lacks the recent links that expose the most visible keyboard jump. Verify first Tab after main reaches the primary action (or actual Resume when present) without scrolling to recent history; inspect AX heading order and mobile all-language focus path.

## Additional, nonblocking findings

- The preview instruction is currently repeated as the containing region accessible name, image name and visible paragraph. Keep the image description, but name the region simply “Starting position” or omit a redundant region name. A screen reader should not hear the same full instruction three times.
- In the170px Armenian sidebar, some words break with a final single character on another line. Labels remain visible, so this is not a blocked navigation path, but use a locale-specific type/gap adjustment or sufficient text width to preserve whole-word readability. Do not hide or truncate labels.
- Fixed bottom nav bisects long screenshots at the initial viewport position. I do not infer that content is unreachable from this artifact; normal scrolling exists. No new requirement to remove the navigation.

## Current technical evidence inspected

Completed matrix.log:384 cases, failures[], runtime[]. accessibility.log:58 screens, violations[]; incomplete contrast items remain for auth-home-link and mobile assistant and are not a claim of full accessibility. Scenarios.log has six exercised scenario groups with errors[]. tests.log completed with no cancelled/skipped/todo; the final suite summary shows the tests finished, while package.log ends normally. These support the preserved functionality, but do not remove AX-POST1.

Own browser checks also confirmed current toast foreground#F7F6F5 on#262421 and auth error focus outline; login at320px had document scrollWidth320. This is not full browser-zoom testing.

## Verdict: CHANGES REQUIRED

The actual composition now matches the user's chosen reference substantially better and preserves the key accessibility fixes. One concrete home-page reading/focus-order regression must be fixed before final approval. It can be corrected structurally without changing the requested visual layout or adding features. After that, repeat the short Tab/AX sequence; no new design-research round is necessary.
