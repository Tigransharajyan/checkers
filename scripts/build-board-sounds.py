"""Prepare real wooden-board recordings without pitch shifts or synthetic layers.

Requires ffmpeg. Run from any directory; sources are bundled under scripts/audio.
"""
from pathlib import Path
import array
import subprocess
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'src/main/resources/static/sounds/board'
RATE = 44100


def recording(name, peak):
    raw = subprocess.check_output([
        'ffmpeg', '-v', 'error', '-i', str(ROOT / 'scripts/audio' / name),
        '-ac', '1', '-ar', str(RATE), '-f', 's16le', '-'
    ])
    samples = array.array('h', raw)
    if sys.byteorder != 'little':
        samples.byteswap()
    scale = peak / max(abs(value) for value in samples)
    # Preserve the recorded contact and body; only soften the edit boundaries.
    fade_in, fade_out = round(.0005 * RATE), round(.008 * RATE)
    pcm = [value * scale * min(1, index / fade_in, (len(samples) - 1 - index) / fade_out)
           for index, value in enumerate(samples)]
    return pcm + [0.] * round(.012 * RATE)


def write(path, pcm):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = array.array('h', (round(max(-1, min(1, value)) * 32767) for value in pcm))
    if sys.byteorder != 'little':
        data.byteswap()
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(data.tobytes())


def main():
    # The original placement stays at its natural pitch; variation is level only.
    clips = {f'move-{index + 1}': recording('board-placement.mp3', peak)
             for index, peak in enumerate([.40, .38, .42])}
    # A single recorded contact, not the old artificially doubled impact.
    clips['capture'] = recording('board-capture.mp3', .58)
    for name, pcm in clips.items():
        write(OUTPUT / (name + '.wav'), pcm)
    # Audition: three ordinary moves, one capture, then three capture landings.
    preview = []
    for name in ['move-1', 'move-2', 'move-3', 'capture']:
        preview.extend(clips[name])
        preview.extend([0.] * round(.45 * RATE))
    for _ in range(3):
        preview.extend(clips['capture'])
        preview.extend([0.] * round(.04 * RATE))
    write(ROOT / 'docs/redesign/chesscom-refinement/board-sounds-preview.wav', preview)


if __name__ == '__main__':
    main()
