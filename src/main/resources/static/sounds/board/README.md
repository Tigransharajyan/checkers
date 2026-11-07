# Recorded wooden-board sounds

Real recordings of pieces placed on a wooden board, served locally as mono PCM WAV (44.1 kHz). A quiet move plays one placement; each landing in a capture sequence plays one heavier contact. Game start/end do not add extra taps.

Sources by **el_boss**, both CC0 1.0, recorded on a wooden board for Chess Puzzle Blitz:
- Placement: https://freesound.org/people/el_boss/sounds/546119/
- Capture: https://freesound.org/people/el_boss/sounds/546120/
- License and direct source URLs: `LICENSE.txt` in this directory.

The runtime WAV files are included and need no build step. Processing was limited to sample-rate/mono conversion, volume adjustment, and short edit-edge fades. There are no pitch changes, low-pass muffling, artificial doubled impacts, or synthetic layers. The three move files differ only in volume; the capture uses its own recording.

`BoardUI.onLand` triggers audio when each animated segment finishes, including a player's preview and opponents' moves. Server confirmation and duplicate snapshots do not replay it. Reduced-motion mode preserves separated capture contacts without animating the piece; cancelled and hidden-tab moves stop remaining contacts. Audio URLs include a version to bypass old year-long browser caches.
