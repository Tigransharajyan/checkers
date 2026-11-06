"""Build short, softened wooden board sounds from Kenney's CC0 impact sample.
Requires ffmpeg. Run from any directory; output is self-contained 44.1 kHz WAV.
"""
from pathlib import Path
import array
import subprocess
import sys
import wave

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'src/main/resources/static/sounds/board'
RATE = 44100


def tap(speed, duration, peak):
    filters = (f'asetrate={int(RATE * speed)},aresample={RATE},'
               f'highpass=f=110,lowpass=f=3200,atrim=duration={duration},'
               f'afade=t=in:d=0.0015,afade=t=out:st={duration * .52}:d={duration * .48}')
    raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i',
        str(ROOT / 'scripts/audio/wood-impact.ogg'), '-af', filters,
        '-ac', '1', '-ar', str(RATE), '-f', 's16le', '-'])
    pcm = array.array('h', raw)
    if sys.byteorder != 'little':
        pcm.byteswap()
    scale = peak / max(abs(x) for x in pcm)
    return [x * scale for x in pcm]


def mix(events):
    result = [0.] * max(int(delay * RATE) + len(pcm) for delay, pcm, volume in events)
    for delay, pcm, volume in events:
        offset = int(delay * RATE)
        for i, value in enumerate(pcm):
            result[offset + i] += value * volume
    return result


def write(path, pcm):
    data = array.array('h', (round(max(-.8, min(.8, value)) * 32767) for value in pcm))
    if sys.byteorder != 'little':
        data.byteswap()
    with wave.open(str(path), 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(data.tobytes())


OUTPUT.mkdir(parents=True, exist_ok=True)
moves = [tap(1.12, .17, .46), tap(1.18, .16, .43), tap(1.06, .19, .45)]
clips = {f'move-{i+1}': pcm for i, pcm in enumerate(moves)}
clips['capture'] = mix([(0, tap(.97, .21, .5), .9), (.072, moves[1], .62)])
clips['start'] = mix([(0, moves[0], .7), (.13, moves[2], .8)])
clips['end'] = mix([(0, moves[1], .72), (.12, moves[0], .6), (.27, tap(.94, .22, .4), .62)])
for name, pcm in clips.items():
    write(OUTPUT / (name + '.wav'), pcm)
# The preview plays move, capture, start, end, separated by pauses.
preview = mix([(0, moves[0], 1), (.7, clips['capture'], 1),
               (1.5, clips['start'], 1), (2.4, clips['end'], 1)])
write(ROOT / 'docs/redesign/chesscom-refinement/board-sounds-preview.wav', preview)
