# Independent Art Director — actual Chess.com-layout revision

## Evidence actually inspected

Read post.md and plan.md; reference play.png/computer.png were actually viewed in the preceding plan review. Viewed all requested new images through view_image: home1440/390, game-registered1440/390, home-hy1440, tasks390, login390, assistant1440.

Also opened current localhost read-only in Chromium1440×1000 with HY, waited for document fonts, measured actual per-character line rectangles in sidebar labels and captured `/tmp/checkers-chesscom/art-hy-viewport.png`. Then opened RU home at390×844, scrolled to650 and captured `/tmp/checkers-chesscom/art-mobile-scroll.png`. This confirms that invitation and puzzle are reachable; fixed navigation appearing midway through a supplied full-page capture is not treated as an inaccessible-content defect. No app state/data was changed, no source was edited and other reviewers' conclusions were not read.

## Scores for actual UI

| Criterion | Score /10 | Rationale |
|---|---:|---|
| Visual quality | 8 | The large board and independent narrow action column finally reproduce the reference's defining composition; one visible localisation polish issue remains. |
| Usability | 8 | Distinct labelled actions, visible difficulty and actions-first mobile order preserve practical clarity. |
| Consistency | 8 | Board, sidebar, action panels, auth and assistant use the same warm-charcoal system; typography on HY navigation needs refinement. |
| Accessibility | 8 | Readable text and large controls in observed views; no independent full AT/keyboard audit in this art review. |
| Originality | 6 | Intentionally close to the selected reference, with own draughts pieces/identity. This is aligned with explicit user preference. |
| Product fit | 9 | The main image is now the product's real board, not a decorative promotional scene or a generic dashboard. |

## Three strongest outcomes

1. **The revision is compositional, not just a palette swap.** A760px initial board on home1440 now dominates the left, while the320px action panel sits independently on the right. Bold menu entries, solid neutral surfaces, narrow navigation and player bars visibly resemble the selected real Chess.com screen. The previous text-first lobby has actually been replaced.
2. **The resemblance does not require unsupported content.** Online/bot/friend work as the real modes; no coach, tournaments, fake ratings or invented lesson cards fill the menu. The static board is labelled as an initial position rather than pretending to be a live game. Daily puzzle remains a separate real position.
3. **Actual game preserves an efficient working view.** Opponent and self bars sit above/below the board, while status, surrender and notation occupy the narrow side. The former145px artificial gap has not returned. Mobile presents controls before preview on home, and actual board stays near full width.

## Required visual correction

### ART-CHESS-1 — Armenian sidebar leaves isolated final letters on new lines

**Priority: Medium; must fix before final visual sign-off.** Current home-hy1440 visibly breaks single navigation words into an almost complete first line and a final1–2-letter second line. This looks accidental rather than a finished localisation and damages fast reading of persistent navigation.

The actual browser measurement confirms this is not screenshot rescaling:

- `Առաջընթաց` renders as `Առաջընթա` / `ց`.
- `Պատմություն` renders as `Պատմությո` / `ւն`.
- Text area is100px. Current170px sidebar,28px icons,10px gap, link padding and `overflow-wrap:anywhere` cause the split.

The two-line `Կանոններ և FAQ` is different: it has a meaningful word boundary and is acceptable. Account identifiers may wrap arbitrarily; ordinary navigation labels should not be treated as identifiers.

**Concrete correction:** allocate several more pixels to HY navigation text without losing readable type. A locale-specific smaller icon/gap/padding combination or a slightly wider sidebar for HY are valid. Preserve approximately16px label size,44px+ targets and the reference's dense character. Avoid ellipsis, clipping or shrinking labels into microtype. If a word must wrap, use proper language-aware breaking rather than a single orphan glyph. Verify all six labels on actual HY desktop and the mobile drawer; recheck widths901/1280/1440 so the fix does not create page overflow.

This is a local polish correction, not a request to change the selected concept.

## Nonblocking notes

- The main board/action gap is wider than the nominal24px because the height-capped board is centred inside its column. It still reads coherently and does not obscure the requested board/menu relationship; no decorative filler is needed.
- The mobile initial preview extends the page after useful controls. It is explicitly requested by the selected board-led direction and honestly labelled, so its presence is justified. Do not move it above the actions merely to resemble desktop.
- In assistant1440, a transient toast overlaps part of the question area. It is a short status, and no inability to continue was established in this art review. It is not treated as a new critical defect.

## Verdict

**CHANGES REQUIRED — one local visual correction.** The actual composition now satisfies the requested Chess.com board-and-menu direction convincingly. Before final visual approval, fix the visibly orphaned HY navigation letters. No architectural redesign or return to earlier concepts is needed; all other observed major visual decisions are acceptable.
