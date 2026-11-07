# Draws and multiple premoves

The same server-side policy runs after every human and bot move in friend,
matchmaking and bot games. A real win (last capture or no legal moves) takes
precedence over a draw counter. `Game.drawState` stores the history in the
database; it is independent of the bot's speculative search boards. Older active
games reconstruct it from their move log on their next move.

## Automatic rules

| Condition | Limit |
| --- | --- |
| Identical pieces, squares, colours/types and side to move | Third occurrence, including the initial position |
| 4–5 pieces; kings only, no man moves or captures | 15 moves by **each** player |
| 6–7 pieces; kings only, no man moves or captures | 25 moves by **each** player |
| Both players have kings; no captures or promotions; 2–3 pieces | 5 moves by each player |
| Same, 4–5 pieces | 30 moves by each player |
| Same, 6–7 pieces | 60 moves by each player |
| Three or more kings (possibly additional men) against a lone king | 15 attacking moves since that material balance was established |
| Three pieces off the long diagonal against a lone king on a1–h8 | 5 attacking moves while that condition persists; mandatory tactical continuations are exempt |

Moving a man resets only the kings-only clock. Captures and promotions reset the
material clocks; entering a different ending starts its clock from zero. Counts
are per player, not a rounded half-move count. Draw reasons use `DRAW_*` and are
treated as draws in game results, saved history, and account statistics.

The long-diagonal rule requires all three attacking pieces to be off that
diagonal. It defers adjudication while either side has a capture continuation,
so a forced capture or a winning capture is not cut off by the five-move clock.

Primary reference: Federation of Russian Draughts, 2022 rules, sections
6.1.8.3–6.1.8.8:
https://shashki.ru/download/2022/doc/FSHR_shashki_pravila_2022.doc
Repetition and agreement also appear in IDF article 7:
https://idf64.org/rules-of-the-game/

## Agreement and bot offers

There is no referee role. A position merely *evaluated* as drawn never forces a
result. Humans can offer and accept a draw. A bot can propose a draw after five
stable moves by both sides in a balanced king ending; the player can decline and
continue. Repeated bot proposals have a 20-ply cooldown. A human can also ask the
bot for a draw; the bot may decline. No approximate score is used by the automatic
draw adjudicator.

Offers are saved with the game, cannot be accepted by their proposer, and expire
when the recipient plays a move. Game mutations take a database row lock so
concurrent moves, acceptance, resignation and bot replies cannot finish the same
game twice. Casual games retain immediate agreement; tournament deployments may
set `app.game.draw-offer-minimum-moves: 20` (default `0`).

## Multiple premoves

While waiting, click or drag several own moves. The board projects the queued
piece positions, allowing the same piece to be selected again at its future
square. The authoritative position is never modified. The existing turn banner
reports the queue length; no page layout or styling changes are required.

Only the next premove is checked against the server's actual legal moves after
an opponent move. Mandatory captures and complete capture paths remain binding.
Consecutive planned capture legs can be combined into one full legal capture;
ordinary future turns remain separate. Confirmation consumes only the executed
entries. An invalid next move, capture of its source, server rejection, game end,
right-click or Escape cancels the remaining queue. Old legal-move requests cannot
execute a move on a newer position. Execution uses the same animation and
per-landing sounds as a normal move.
