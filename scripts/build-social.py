"""Rebuild the social preview as a deterministic draughts diagram, without AI imagery.
Uses Pillow only as an optional asset-build tool, not an application dependency.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1]
im = Image.new('RGB', (1200, 630), '#302E2B')
draw = ImageDraw.Draw(im)
font_root = Path('/usr/share/fonts/truetype/dejavu')
title = ImageFont.truetype(str(font_root / 'DejaVuSerif.ttf'), 54)
body = ImageFont.truetype(str(font_root / 'DejaVuSans.ttf'), 24)
draw.text((64, 180), 'Checkers\nOnline', fill='#81B64C', font=title, spacing=12)
draw.text((68, 350), 'Russian draughts · 8 × 8', fill='#C3C0BC', font=body)
left, top, cell = 622, 63, 63
for row in range(8):
    for col in range(8):
        x, y = left + col * cell, top + row * cell
        dark = (col + row) % 2 == 1
        draw.rectangle((x, y, x + cell, y + cell), fill='#779556' if dark else '#EBECD0')
        if dark and (row < 3 or row > 4):
            white = row > 4
            fill, border = ('#FFFAEA', '#17241F') if white else ('#17241F', '#FFFAEA')
            draw.ellipse((x + 8, y + 8, x + 55, y + 55), fill=fill, outline=border, width=2)
            draw.ellipse((x + 17, y + 17, x + 46, y + 46), outline='#99896A' if white else '#AAB9A9', width=1)
draw.rectangle((left, top, left + 504, top + 504), outline='#52673D', width=2)
im.save(root / 'src/main/resources/static/images/checkers-social.jpg', quality=90, optimize=True)
