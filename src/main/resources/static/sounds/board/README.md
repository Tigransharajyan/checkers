# Wooden board sounds

Short, softened wooden impacts for moving draughts pieces, capturing, starting and ending games. All clips are mono PCM WAV (44.1 kHz), served locally. No oscillators or musical notification tones are used.

Derived from Kenney's Impact Sounds 1.0, `impactWood_heavy_000.ogg`, CC0 1.0:
- Creator/source pack: https://kenney.nl/assets/impact-sounds
- Retrieved source copy: https://github.com/shoemoney/commander-in-chief/blob/main/assets/cc0_extra/sfx_impact/impactWood_heavy_000.ogg
- Original source: `scripts/audio/wood-impact.ogg`.
- License: `LICENSE.txt` in this directory.

Rebuild with `python3 scripts/build-board-sounds.py` (requires ffmpeg). Processing: mono conversion, subtle resampling variations, high/low-pass filtering, short fades, level control. Capture uses two contacts; start/end use short, quiet sequences of the same wooden taps.
