# Independent review — Brand & Product Identity Designer

Reviewer: Agent 5. Reviewed `docs/redesign/chesscom/pages-revision.md`, `screenshots/final-desktop.png`, the real reference `screenshots/play.png`, and current home, play menu, game, tasks, rating, history, FAQ, sidebar and mode templates plus relevant identity strings. No other reviews were read. No project code was edited.

This is a review of the proposed pre-implementation specification, informed by the current UI. Scores describe the proposed direction; screenshots establish existing problems, not proof that the proposal is implemented. Accessibility is a design-readiness assessment, not an interaction audit.

| Dimension | Score / 10 |
|---|---:|
| Visual quality | 8 |
| Usability | 8 |
| Consistency | 8 |
| Accessibility | 7 |
| Originality | 6 |
| Product fit | 8 |

## Three strengths

1. Page differentiation is the right reference lesson. A useful home summary, dedicated mode launcher, focused game, tactical workbench and archive reproduce Chess.com's task hierarchy without transplanting its content catalogue. This directly fixes the current initial-board home and near-identical play menu.
2. Product honesty is unusually clear. The specification excludes chess coaches, tournaments, puzzle maps, invented global rankings and synthetic chart data. Real daily problems, bot difficulty, friend invitations and actual game results fit the existing Russian draughts service.
3. The visual vocabulary translates credibly. The reference's warm dark navigation, green/cream board and compact game-first layout are recognizable in the current screenshot. Draughts discs and the two-disc brand mark replace chess pieces and the pawn logo; the proposed flat surfaces and restrained actions should unify the new page types.

## Critical concerns and specific fixes

### 1. Russian draughts is not sufficiently explicit at the primary decision point

The current first screen says “Шашки Онлайн” and “Играть в шашки”; the 8×8 initial position also fits other draughts variants. The proposal changes page hierarchy but does not require a visible variant identifier on home/play. The existing localized `app.tagline` and `redesign.lobby.title` already offer accurate wording, while the clearer rules description is currently buried in FAQ/SEO. This is a product identity gap: an English-speaking visitor choosing a game needs to know which ruleset is used before starting.

**Required specification fix:** require one concise visible localized “Русские шашки / Russian draughts” descriptor on home and the play launcher, with a nearby rules link. Retain the established product name. If adding a rules summary, use existing verified rules content; do not invent a variant switch or new features.

### 2. The common identity layer is less specified than the borrowed foundation

The foundation specifies essentially the reference palette, board palette, dark sidebar and composition, but omits what remains distinct across the new game header, mobile header and standalone auth. The existing ownable two-disc mark appears in the desktop sidebar/auth, while mobile uses text only; desktop Play uses a gamepad and mobile Play uses swords. With substantially different page shells, this omission can turn a coherent draughts club into generic dark gaming pages whose strongest identifying feature is Chess.com's palette.

**Required specification fix:** explicitly carry the existing disc mark and localized product naming through the regular shell, compact game header and auth; define its mobile treatment. Specify one consistent semantic icon mapping across desktop/mobile (especially Play), and retain the existing original draughts piece treatment, including a distinct readable king state. No new logo artwork is necessary. Document that third-party reference logos, chess pieces, characters, emoji-like illustrated mode icons and other branded assets are reference-only, not implementation assets.

## Other refinements

- Give the personal statistics route a single user-facing “Progress” identity across navigation and page heading. A trophy and `/rating` implementation name must not imply a competitive leaderboard or rating value absent from the actual data.
- Keep the current reference comparison focused on hierarchy: Chess.com's many colorful illustrated tiles are acceptable for its broad catalogue; repeating that tile stack for this smaller three-mode product would recreate the current generic-card problem. The specification's open lists and separators are a good corrective.
- The typography, 44px targets, keyboard board instructions, visible focus and reduced-motion requirements are sound. Before release verify green text on raised surfaces, king/man distinction without relying on color alone, and localized compact-header naming at narrow widths. Static screenshots do not establish those outcomes.
- No Chess.com logo, chess piece art or chess-only mode was found in the inspected local templates/screenshot. This is an observation of inspected material, not a complete asset provenance audit.

## Decision

**CHANGES REQUIRED.** Direction A is structurally appropriate and faithfully borrows the useful parts of Chess.com. Approval is held only for the two specification gaps above: make the Russian ruleset visible before starting and define the small original identity layer across the newly distinct shells. These are bounded documentation changes, not a demand for another aesthetic direction or unsupported product features.
