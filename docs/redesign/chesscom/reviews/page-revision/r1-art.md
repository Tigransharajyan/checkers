# Independent pre-implementation art direction review

**Reviewer:** Agent 2, Art Director / strict Visual Design Critic
**Verdict: CHANGES REQUIRED**

Reviewed independently: `docs/redesign/chesscom/pages-revision.md`; current route templates (`index`, `play-menu`, `play`, `tasks`, `rating`, `history`, `faq`, `login`, `register`, `layout`); current-product `final-desktop.png`; actual-reference `play.png`. No other reviewer reports consulted. No project code changed.

These scores assess the supplied direction and its readiness for implementation, informed by current-product evidence. They do not claim the proposed pages have been rendered or validated.

| Dimension | Score / 10 | Judgment |
|---|---:|---|
| Visual quality | 6 | Sound palette and hierarchy intentions; actual composition is still insufficiently specified to produce a finished result reliably. |
| Usability | 8 | Separating home, launcher, game and archive fixes a substantive navigation problem. |
| Consistency | 8 | Shared navigation, type and interaction tokens provide a coherent basis for distinct page structures. |
| Accessibility | 7 | Keyboard preservation, dark action labels, reduced motion and targets are explicit; board-state contrast and localized layout still need measurable acceptance criteria. |
| Originality | 6 | Appropriately restrained adaptation, but no resolved checkers-specific visual voice beyond existing discs and logo. |
| Product fit | 8 | Honest data, existing capabilities and a board-led game surface fit this product well. |

## Three strengths

1. Direction A correctly distinguishes page purposes. Removing the idle board and full mode selector from home is a meaningful structural change, rather than another recoloring pass.
2. The plan refuses fabricated ratings, archive rows and unsupported puzzle features. That gives the design a credible content foundation and avoids compensating for sparse data with decorative misinformation.
3. The shared warm graphite, restrained green, real boards, local fonts and compact radii can form a recognizable game product while retaining the checkers identity.

## What the supplied screenshots actually show

Both images are 1440×1000. The reference sidebar is about 170px wide; its board begins near x188, y66 and is about 728px square. The current product uses the same approximate sidebar width but its board starts near x253, y80 and is about 760px square. Its mode rail begins around x1085, versus x948 in the reference. Thus the present design has already borrowed the obvious palette/board/sidebar grammar, but loses roughly 65px in its left gutter and pushes the launcher substantially rightward.

More importantly, reference mode tiles are succinct, approximately 268×116px choices with large distinctive visual anchors. The product stacks larger explanatory panels with nested buttons, selectors, borders and an additional invitation form. Its bot panel alone is much taller than the reference tile. With only three supported modes, the product still gives the launcher a heavy form-like rhythm and places the daily task below the first screen. This is why it feels like a generic dashboard using Chess.com colors rather than a resolved game launcher. The answer is not to invent six modes or copy proprietary illustrations; it is to match decisiveness, density and hierarchy with three real choices.

## Page-by-page assessment and concrete requirements

| Route | Assessment | Required clarification/change before implementation |
|---|---|---|
| `/`, `/home`, `/index` | Correct new role; no resolved composition for guests or accounts with no activity. When active games, metrics and recent results disappear, the described two-column layout can collapse back into a few disconnected cards on a large canvas. | Specify desktop and mobile content order for guest, signed-in empty, populated, and active-game states. Define a compact home header and one primary launch region; place daily puzzle as an actual secondary module, recent games as an open list, and omit absent metrics without leaving empty columns. State rail/main widths and where the rail collapses. |
| `/play` | Biggest visual risk. “Preview permitted on desktop” leaves the core composition optional, while the current overgrown nested mode panels could survive unchanged. | Commit to one wide layout and one compact layout. At 1440px, use an approximately 170px sidebar, 24px outer gutter, an approximately 640–728px board constrained by height, 24–32px gap and 300–340px control rail; allow unused space beyond the composition. Use three compact mode rows, one dominant action, and reveal bot/friend settings in a shared selected-mode region. Keep create and join together without displaying every control as a separate boxed card. At a documented threshold, remove/reduce the preview and place action first. These are proposed targets, not a demand for pixel copying. |
| Active `/play?id=…` / `?code=…` | Board priority and contextual controls are appropriate. Removing the sidebar is a deliberate local adaptation; the supplied Chess.com screenshot does not establish that sidebar removal itself is a reference trait. | Choose a specific compact header rather than leaving “remove or collapse” unresolved. Draw the board/player bars/status/control relationships, including short-height desktop and 360px mobile. Keep primary state next to the relevant player, quiet draw/resign controls, and notation as a dense functional surface rather than another marketing card. |
| `/tasks` | Distinct workbench is promised, but the current context/board/answer trio could simply acquire new CSS and retain the same visual pattern. | Specify a task header with date/progress, board, and a single adjacent answer/control region. Show exactly where wrong/correct/reset feedback appears without shifting the board or burying the objective. On mobile keep a concise “side to move / objective” before the board, then feedback and actions; the instruction cannot be deferred entirely below the board. |
| `/rating` | Reframing as personal performance is honest; the visual solution remains abstract. Existing summary/stat/about cards and em dashes are precisely the pattern to eliminate. | Define a single typographic performance band for real win-rate and W/L/D/total, followed by real results if available. Define a separate bounded guest explanation with an account CTA and no empty stat boxes. For a new account explicitly state no completed games and offer play. Avoid a full-width generic panel surrounding another grid of metric cards. |
| `/history` | Archive table is the right page identity; a “full explanatory panel” alone risks recreating the existing guest box. | Specify a consistent archive heading/column rhythm and a compact, deliberately placed guest/empty state whose width is tied to readable copy rather than viewport width. Preserve a table for real data; define how players/result/open action behave at narrow widths, and how loading/error occupy the same content area. Do not use decorative sample tables. |
| `/faq` | Editorial direction is good but “editorial knowledge page” is not a layout specification. | Define a 680–760px reading column, search width, question spacing, answer line-height and separators. Use a page title/short introduction then search and open details rows; avoid an outer card plus individual accordion cards. Explicitly define a contrasting keyboard focus ring and visible open/closed indicator. |
| `/login`, `/register` | Existing standalone auth structure is already reasonable and needs polish more than reinvention. | Commit to a compact approximately 400–440px form with a quiet brand/header, clear heading, labels and inline errors. Distinguish primary submit, guest continuation and cross-link visually; reserve error space without excessive empty vertical bands. Show a long Armenian/Russian error and the mobile form layout. |

## Critical cross-page issues

1. **Tokens are more precise than compositions.** Colors, radii and transition timings are resolved, but many page instructions remain “specialize,” “compact,” “clear” or “good width.” Those words cannot prevent another universal-card implementation. Add annotated wireframes or equivalent dimensioned composition diagrams for every route family and its principal empty state. This is a pre-implementation deliverable, not a request to change code before the gate.
2. **Reference fidelity must be about proportion and information rhythm.** The supplied current image already has the colors. Establish reference-to-local measurements for sidebar/gutters, board-to-rail balance, tile density and title/body scale. Explain intentional deviations for only three real modes and inline configuration. Do not assume adding a 32px title creates page identity.
3. **Surface rules need component-level decisions.** “Flat solid fills and 1px borders” could produce a border around everything despite “no card around every paragraph.” Specify which surfaces are raised: launcher configuration and notation may be raised; recent results, metrics and FAQ rows should primarily use open typography/separators. Avoid nested raised panels.
4. **Icon/piece treatment is unresolved.** The reference uses substantial, distinctive game symbols; the product uses thin colored outlines. Define one coherent local icon weight and size for navigation and a separate larger mode-symbol treatment using original/vector assets. Keep discs legible as checkers; do not borrow chess pieces or create decorative artwork to disguise missing hierarchy.
5. **Visual acceptance needs actual criteria.** The existing 384 technical combinations do not prove finished layouts. Add side-by-side route screenshots at 1440×1000, 1280×720 and 390/360px widths with guest and populated states, plus Armenian text stress. Check board selection/legal-move/focus against both square colors, non-color turn/result cues, body text contrast, and absence of clipped controls at short heights. Do not claim accessibility is proven from token declarations alone.

## Approval rationale

The strategic direction is correct and substantially improves the current product plan. It is not ready for an art-direction approval: the distinguishing layouts and density rules remain largely verbal, so the implementation could satisfy every token and route requirement while still looking like the same panel system. Resolve the compositions, conditional empty states and mode-selection anatomy first. Approval should follow review of those concrete pre-implementation layouts, then finished route screenshots after implementation. **CHANGES REQUIRED.**
