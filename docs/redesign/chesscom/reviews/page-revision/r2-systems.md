# Round 2 independent review — UI Systems Architect

**APPROVED for implementation planning.** The revised specification resolves the critical systems gaps identified in my independent first review. This approves the concrete direction and integration contract, not unimplemented visuals or runtime behavior; the document's implementation acceptance gates still apply.

I read the updated `docs/redesign/chesscom/pages-revision.md`, re-inspected the actual local `final-desktop.png` and Chess.com `play.png`, and checked the current PageController, SecurityConfig, SeoService, locale-link handling, launcher template/mode fragment, and lobby JS against the proposal. Earlier inspected templates, foundations and game/overlay code informed compatibility assessment. No other reviewers' reports were read. No application code was modified.

| Dimension | Score / 10 |
|---|---:|
| Visual quality | 8 |
| Usability | 8 |
| Consistency | 8 |
| Accessibility | 8 |
| Originality | 7 |
| Product fit | 9 |

Scores concern the reviewable proposed system, with current screenshots establishing the problem and reference proportions. Rendered implementation will need separate verification.

## Three strengths

1. **Page compositions now have actionable boundaries.** Home is an action/activity overview with no large initial board; Play alone uses the reference's board-and-rail silhouette; Game, Tasks, Progress, History, FAQ and auth have explicit hierarchies. The original screenshot's oversized board and nested mode cards no longer define every route. Draughts identity stays present without importing Chess.com assets.
2. **Architecture fits the existing stack and behavior.** Three named shells, shared fragment ownership, the existing Overlay manager and guarded `data-*` initialization offer a small Thymeleaf/JS evolution. The existing `/play` controller split and BoardUI/websocket lifecycles remain intact. Reusing the real home matchmaking hook prevents a duplicate game-start implementation and an extra navigation step.
3. **Acceptance is now about observable states and interaction.** The state matrix distinguishes network failure from empty data; game navigation retains account/language/all sections; puzzle instruction precedes the board; width/height/zoom/locales and keyboard focus are explicit. These are substantially more useful than a generic responsive or accessibility promise.

## Resolution of the prior critical concerns

| Concern | Assessment |
|---|---|
| `/home` security, SEO and localization | Resolved at planning level: controller alias, public matcher, locale allowlist, home metadata, canonical/hreflang to root and sitemap exclusion are all named. This correctly addresses current code, where `/home` is absent and SeoService would otherwise select error metadata. Existing game query routing remains explicit. |
| Shell/component ownership | Resolved: ordinary app, focused game and auth shells are selected; shared identity/navigation/account/language/skip-link fragments have ownership. Guarded existing lobby logic is a valid alternative to unnecessary module/framework replacement. |
| Global navigation inside focused game | Resolved: a real drawer with matching `aria-controls`, full navigation, language/account controls and existing overlay focus/inert behavior replaces the large sidebar. Tasks explicitly retains standard navigation. |
| Responsive and DOM order | Resolved: named width transitions, static non-tabbable Play preview, mobile preview removal from accessibility tree, puzzle objective first, document scroll, safe areas and landscape/zoom checks. Height-constrained boards cannot justify shrinking text or clipping controls. |
| Token and localization migration | Resolved sufficiently for pre-code approval: update existing variables in place, no appended parallel theme, explicit contrast roles and font support; language-preserving links, localized key reuse and three-locale acceptance are specified. |
| Async home state independence | Resolved: active/recent/task loading, success/empty/error/retry states are specified independently, with stable status regions and no result-driven focus theft. Guest versus registered personal data is explicit. |

## Critical concerns

**None remaining that require another pre-code revision.** Existing source does not yet implement these contracts, as expected at this stage; those current omissions are not evidence that the revised plan failed to address them.

## Concrete implementation guidance and required verification

These are execution details under the approved contract, not new approval blockers:

- Normalize `/home` to the home metadata key and canonical root deliberately in SeoService; adding it only to `PAGES` would produce a `/home` canonical. Verify title, description, robots policy, canonical and alternate links independently from visible language-switch URLs. Preserve root/index behavior.
- Guard module initialization before issuing unnecessary active/history requests when its region is absent. Current lobby code sometimes checks containers only after fetching, and its active-games catch hides failures. Replace that presentation behavior with the planned error/retry states while retaining endpoint and socket semantics.
- Keep one matchmaking button/status/cancel hook per page and include the entire queue UI on Home, not just its CTA. Retain the confirmation modal required by active-game End actions. Friend-link generation must still reveal/copy/open the real invitation; home Bot/Friend anchors must land on the relevant existing mode rows.
- Use one responsive source tree per page; the wide static Play board can precede the action rail without introducing preceding Tab stops, then be fully removed below 1180px. Avoid duplicate mobile/desktop forms and repeated IDs. Make new game drawer focus return and game language switching preserve the same `id`/`code` URL.
- Token replacement must include existing hard-coded boundary colors such as `#68645e`, not only the prose's `#68645f` spelling. Test actual adjacent surfaces and states. Preserve semantic error/success tokens rather than treating all boundaries as the same state.
- Keep every new visible/accessible status, retry label, opponent-name interpolation and drawer label in EN/RU/HY message resources. Use existing safe text/escaping and locale formatting. Verify long text and zero-total win-rate behavior instead of relying only on English screenshots.
- Treat x188/x948 as the supplied 1440×1000 reference target, as the document states; at shorter heights, width constraints and normal document flow take precedence. Verify assistant/toast/drawer placement after removing the sidebar so inherited margins do not leave dead space or cover game actions.

The revised plan is concrete enough to implement with the existing Spring/Thymeleaf/vanilla JS architecture. Its measured screenshot, state, focus and routing gates should determine whether the resulting implementation is accepted.
