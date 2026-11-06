# Independent actual UI review, round 2 — Accessibility & Usability

**APPROVED**, with the nonblocking refinements below. No confirmed blocking access barrier was found in the inspected implementation and supplied evidence. This is a bounded source/screenshot review, not a WCAG conformance certification.

## Scope and method

Independently inspected the approved `pages-revision.md` access/layout contracts, current templates, CSS, board/game/progress/shared-overlay JavaScript, localized move strings, and all 18 supplied `actual-final-r2` PNGs. I did not read peer reports or use their conclusions. I visually inspected screenshots and reasoned from source. I did **not** exercise a browser, screen reader, axe, touch hardware, live keyboard navigation, zoom, or network-failure simulation during this review. Their runtime outcomes are not claimed as passed.

Some filenames overstate the captured state: `home-session-error-1440x1000.png` and `home-active-1440x1000.png` actually show short mobile-navigation compositions, and `tasks-390x844.png` shows an open drawer. I assess the visible states, not the filename's presumed desktop viewport or a hidden board.

| Dimension | Score / 10 |
|---|---:|
| Visual quality | 8 |
| Usability | 8 |
| Consistency | 8 |
| Accessibility | 8 |
| Originality | 7 |
| Product fit | 9 |

## Strengths

1. **Distinct, readable routes.** Home exposes a large primary action before the daily puzzle on mobile. Play has labelled modes and visible bot controls. Guest Progress explains sign-in without a fake numeric metric; zero-account Progress shows real totals and an explanation instead of a meaningless percentage. Populated history wraps into labelled mobile fields with a visible replay action.
2. **Board semantics and operation are concrete.** Board source provides grid/row/gridcell semantics, coordinate/color/king labels, a roving Tab entry, arrow navigation adjusted for orientation, Enter/Space selection, and state labels for mandatory captures and destinations. Two-tone focus and crown/outline state distinctions supplement hue. The correct-task screenshot visibly distinguishes the promoted king with a crown.
3. **Moves have a stable announcement destination.** `play.html` includes a persistent atomic polite status region specifically for moves. The accepted move event announces localized moving color and complete path; `positionChanged` avoids replaying duplicate position events. Turn status remains a separate region rather than replacing the move text immediately. Runtime screen-reader delivery order remains untested here.
4. **Navigation and overlays preserve access.** The focused-game header exposes Lobby and a complete drawer. Shared Overlay source traps Tab, makes background inert, handles Escape and restores focus. Standard shells retain skip/main landmarks; ordinary mobile controls are large, and the game avoids an extra fixed bottom-navigation layer.
5. **Errors and result states use words.** The session error screenshot explains the failure and presents retry instead of pretending the account is a guest. Wrong/correct puzzle states contain explicit text, with contrasting boundary and background treatment. CSS and board code honor reduced motion; viewport metadata permits browser zoom.

## Blocking barriers

**None confirmed.** The short-landscape board has a source-defined floor: for 601–900px widths and heights ≤650px, its frame uses `min(100%,600px,max(360px,100svh - 120px))`. The 844×390 screenshot visibly shows normal-sized squares and natural continuation below the viewport, consistent with scrolling rather than shrinking the whole board into the first screen. Larger short screens have a corresponding sizing rule. This establishes a credible access path, although scrolling to the last control was not exercised.

The move announcement is implemented in code and localized in EN/RU/HY. I do not infer that it has passed real assistive-technology testing solely from that implementation.

## Nonblocking refinements and precise fixes

- **Landscape player metadata still uses the old height cap.** At 844×390, `.board-column > .player-card` retains `min(...,100svh - 270px)` while the board receives its 360px floor. The screenshot visibly wraps “Бот (Средний)” into several narrow lines above a much wider board. Apply the same usable frame width to player cards and keyboard instructions in the 601–900px short-height rule. This improves alignment and reading efficiency; the player name remains present and no action is lost.
- **Associate task objective with the board.** The objective correctly precedes the board in DOM, but `aria-describedby` references only the keyboard instructions. Give side-to-move/objective text IDs and include them in the description. This helps users returning directly to the board; current linear reading already exposes the instruction, so it is not a blocker.
- **Retain the composite board focus style.** Generic `--focus #abd573` alone has only about 2:1 contrast against dark board green. Current `.sq:focus-visible` uses dark outline plus light border, which should be preserved. Validate focused dark/light/selected cells and both piece colors in a running browser, including forced-colors mode.
- **Ordinary text at 15px remains in narrow Home/task copy.** Return these body instructions to 16px to match the approved typography, then inspect long Armenian/Russian strings at 200% text sizing. Current screenshots remain readable, so this is a refinement rather than a demonstrated reading barrier.
- **Improve evidence coverage.** Capture settled drawer-open and closed task states; the current mobile task drawer appears offset at the left edge, which could reflect a transition frame. Do not diagnose a persistent layout fault from that single frame. Capture actual desktop active/error states under accurately named files. Include an actual board-focus screenshot and scrolled landscape controls.

## Contrast, touch and validation limits

Token arithmetic supports approximately 3.16:1 for `#8b8882` boundaries against raised `#3d3b38`, and 6.85:1 for `#18220e` labels on `#81b64c`. These are selected adjacent-token calculations, not an exhaustive rendered contrast audit. Main labels and muted text look legible in supplied images. Spatial board squares may be smaller than ordinary 44px controls on narrow screens, as explicitly allowed by the plan, while single-tap and keyboard alternatives remain in source.

A final runtime pass should exercise board arrows/selection/capture/promotion, draw/resign dialog restoration, drawer opening and closing, queue cancellation/error recovery, real screen-reader move/result delivery, bottom-control reachability with an on-screen keyboard, 200% text and 400% zoom. Those checks remain unperformed in this review; their absence is disclosed rather than presented as a discovered defect.

**APPROVED:** source and screenshots support usable route-specific layouts, recoverable states, board keyboard access and move announcements; remaining observations are targeted refinements without a confirmed blocking barrier.
