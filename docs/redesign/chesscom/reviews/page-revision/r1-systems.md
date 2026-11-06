# Independent review — Agent 3, UI Systems Architect

**Decision: CHANGES REQUIRED.** Direction A is appropriate, but the specification leaves route integration, shared component boundaries, and responsive shell behavior too ambiguous for implementation approval. These are repairable planning gaps, not a recommendation to abandon A.

Reviewed only `pages-revision.md`, the supplied local `final-desktop.png` and actual Chess.com `play.png`, and current controller/templates/CSS/JS. No earlier or other agent reviews were read. This is a source and screenshot review, not a live interaction certification. Scores assess the proposed direction supported by current evidence; no redesigned page mockups are provided.

| Dimension | Score / 10 |
|---|---:|
| Visual quality | 7 |
| Usability | 7 |
| Consistency | 7 |
| Accessibility | 7 |
| Originality | 6 |
| Product fit | 8 |

## Three strengths

1. **Correct division by user task.** Separating home, launcher, game, puzzle, archive and reference reading resolves a real architectural problem. The local screenshot devotes almost its entire first screen to an initial board and a long launcher; Chess.com's supplied Play capture uses that composition specifically for selecting play. The proposal correctly avoids treating that as a universal home template.
2. **Realistic product and stack boundaries.** Existing `PageController.play` already dispatches `play-menu` versus `play` by `id`/`code`. Distinct server-rendered templates, existing BoardUI, existing endpoints and truthful guest/data states fit Spring/Thymeleaf and avoid an unnecessary SPA migration or invented leaderboard.
3. **Reusable foundations with restrained expression.** Explicit neutral/green colors, local Armenian support, spacing, control states, native FAQ details, substantial targets and reduced motion provide a credible common language across different compositions. Keeping auth standalone is already compatible with the application.

## Critical concerns and concrete remedies

### 1. The new `/home` route is specified only at controller level

`SecurityConfig` publicly permits `/` and `/index`, but does not list `/home`. `common.js` has an explicit locale-preserving route allowlist excluding `/home`. SEO metadata is generated from the request URI through `SeoModelAdvice`/`SeoService`. Adding only `@GetMapping` is insufficient to establish the promised guest-accessible, localized canonical alias.

**Remedy:** Add a route integration checklist to the plan: public security matcher, locale link allowlist, PageController alias, explicit SEO metadata/canonical resolution for `/home`, active Home navigation, and no duplicate sitemap entry unless intentional. Verify guest and registered `/home?lang=en|ru|hy`, language switching, canonical/hreflang, and existing `/`/`/index` compatibility. Keep game `id`/`code` query parameters intact when switching language.

### 2. Shared components versus page shells are named but not assigned ownership

`layout.html` is not the wrapper currently used by individual templates; they independently repeat shell, sidebar/mobile fragments, toast and assistant placement. The existing `lobby.js` serves both home and launcher and owns matchmaking, invites, active games, recent results and metrics through global selectors. “Separate templates” is already true today and does not itself solve shared layout or behavior coupling. CSS also contains late overrides of the same dashboard and game selectors.

**Remedy:** Specify three reusable shell contracts (standard app, focused game, standalone auth) using Thymeleaf fragments or a consistent explicit composition; retain shared head, account/language controls, toast and overlay behavior. Assign home modules to home, mode launcher to play, and shared launch/auth helpers to a small shared JS unit, or document guarded initialization if keeping `lobby.js`. Preserve existing `data-*` runtime contracts and unique IDs. Give page compositions scoped classes; consolidate contradictory late CSS rules instead of appending another override layer. Do not require a framework rewrite.

### 3. The focused game shell removes essential utilities without defining their replacement

Removing the sidebar also removes the existing skip link, account/logout and language switch. Removing the sidebar DOM while leaving current drawer toggles (`aria-controls="main-sidebar"`) creates a broken control. Game shell choice is left as “remove or collapse,” so reviewers cannot assess one concrete architecture. Tasks are called a separate workbench, but it is unclear whether their ordinary nav remains.

**Remedy:** Choose one game shell before implementation: compact header with Home/lobby return, game identity/status context and a clearly reachable account/language menu; specify mobile equivalent and skip-to-main. Keep or remove drawer markup and initialization coherently. Explicitly state that Tasks retains the standard app navigation (or define its focused shell). Preserve auth destination and overlay focus-return behavior, and state how switching locale restores the same game URL.

### 4. Responsive rules lack height, overflow and source-order contracts

Width checks at 1440/1280/768/390/360 do not establish usable boards at short landscape heights or zoom. Existing CSS switches game navigation at 1200px, standard navigation at 900px and dashboards around 1220px. The specification does not resolve these independent breakpoints. Source order currently places the launcher aside before board preview, while desktop CSS visually reorders it; a new layout can easily put tab order at odds with visual order. Mobile puzzle “board then condition” risks hiding the instruction needed to solve it.

**Remedy:** Define per-shell transitions and a board sizing rule constrained by available width and usable dynamic viewport height, including player bars/header/coordinates. Prefer document scroll with reachable controls over shrinking the board to unusable squares or trapping nested scroll. Explicitly define notation overflow, safe-area padding and assistant/toast placement. Set semantic source order for home action/continuation, play actions before decorative preview, and puzzle concise condition before board with feedback/actions adjacent. Require checks at 1280×720, 768×1024, 390×844, 360×640, a short landscape viewport and 200% zoom, including long HY/RU labels and open dialogs.

### 5. Tokens and localization need a migration and acceptance contract

The proposed token names are prose while current CSS already has `--bg`, `--surface`, `--surface-alt`, `--line`, etc. Current `--content-width` is 1200px versus proposed 1320px; current font-size aliases such as `--fs-11` actually resolve to 14px. A second foundation layer would make the system less predictable. “HY/RU wrap” does not address translated JS messages, number/date formatting, greeting interpolation or localized accessible names in the new game header.

**Remedy:** Provide a token mapping/update table using existing CSS variables, name text/border/focus/error/success roles and document any deliberate legacy aliases. Keep one source of truth. Inventory new message keys across EN/RU/HY for both Thymeleaf and `window.i18n`; use existing locale helpers/Intl formatting and text-safe interpolation for user names. Verify missing-key fallbacks, longest translations, text wrapping, keyboard focus and contrast for each interactive state. Add `aria-current="page"` for active section navigation; current sidebar only adds a visual class.

### 6. Home's new asynchronous composition lacks the same explicit states required for History

Home prioritizes active games and actual daily/recent data, but the plan only explicitly distinguishes guest/loading/empty/error for archive. Current `lobby.js` drives several modules and auth refresh together. A failed active-games request must not imply there is no resumable game; a task failure must not leave an empty or misleading preview.

**Remedy:** Specify independent loading, success, empty, guest and recoverable-error states per applicable home module; show continuation only after real data arrives and keep start play available. Preserve guest-session versus registered-account distinctions from current auth behavior. Define a stable loading footprint and polite announcements without moving keyboard focus when results arrive. Reuse status/retry patterns rather than inventing separate logic for each page.

## Approval conditions

Revise the document with the route integration checklist, explicit shell/component contracts, responsive/source-order rules, token/localization mapping and asynchronous home-state rules above. Include concrete page-level wireframes or annotated compositions for Home, Play and focused Game so that the next review can judge hierarchy and density rather than infer them from prose. The changes should preserve the existing Spring controller split, Thymeleaf rendering, BoardUI and game websocket/state behavior. No application code was modified in this review.
