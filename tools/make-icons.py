#!/usr/bin/env python3
"""Draws the HyperVoice icon and the TV banner (PIL only). Run from the repository root:

    python3 tools/make-icons.py

Writes docs/img/icon.png, docs/img/banner.png and app/src/main/res/drawable-xhdpi/banner.png.
The launcher icon itself is the vector app/src/main/res/drawable/ic_launcher.xml (same geometry).
The wordmark uses Noto Sans Bold (SIL Open Font License).
"""
from PIL import Image, ImageDraw, ImageFont
import math
import os

TOP = (15, 23, 42)        # #0F172A
BOTTOM = (67, 56, 202)    # #4338CA
ACCENT = (34, 211, 238)   # #22D3EE
SOFT = (199, 210, 254)    # #C7D2FE
FONT = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

# The mark, in a 100-unit box: a microphone (white capsule, cradle and stand) with an indigo grille and
# two pairs of sound waves in the accent colour.
CENTER = (50, 38)
CAPSULE = (38, 14, 62, 54)       # x0, y0, x1, y1
CRADLE_R = 21                    # a half circle under the capsule, centre (50, 40)
WAVES = ((31.5, 1.0), (39.5, 0.55))  # radius round CENTER, opacity
WAVE_HALF_ANGLE = 38
STROKE = 4.4


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = min(1.0, max(0.0, 0.35 * x / w + 0.65 * y / h))
            px[x, y] = tuple(int(TOP[i] + (BOTTOM[i] - TOP[i]) * t) for i in range(3))
    return img


def arc_points(cx, cy, r, a0, a1, n=90):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]


def stroke(d, pts, k, width, color):
    """A line through [pts] with round caps and joints, drawn as discs a fraction of a unit apart so it is smooth."""
    r = width * k / 2
    for (xa, ya), (xb, yb) in zip(pts, pts[1:]):
        steps = max(1, int(math.hypot(xb - xa, yb - ya) / 0.2))
        for i in range(steps + 1):
            x = (xa + (xb - xa) * i / steps) * k
            y = (ya + (yb - ya) * i / steps) * k
            d.ellipse([x - r, y - r, x + r, y + r], fill=color)


def draw_mark(img, x0, y0, unit):
    """The mark in a 100-unit box whose corner is (x0, y0), [unit] pixels a unit."""
    s = 4
    k = unit * s
    big = Image.new("RGBA", (int(100 * k), int(100 * k)), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    white = (255, 255, 255, 255)
    indigo = BOTTOM + (255,)
    cx, cy = CENTER
    for r, alpha in WAVES:
        col = ACCENT + (int(255 * alpha),)
        stroke(d, arc_points(cx, cy, r, -WAVE_HALF_ANGLE, WAVE_HALF_ANGLE), k, STROKE, col)
        stroke(d, arc_points(cx, cy, r, 180 - WAVE_HALF_ANGLE, 180 + WAVE_HALF_ANGLE), k, STROKE, col)
    x0c, y0c, x1c, y1c = CAPSULE
    d.rounded_rectangle([x0c * k, y0c * k, x1c * k, y1c * k], radius=12 * k, fill=white)
    for y, half in ((24, 6.5), (31, 6.5), (38, 6.5)):
        stroke(d, [(50 - half, y), (50 + half, y)], k, 2.6, indigo)
    stroke(d, arc_points(50, 40, CRADLE_R, 0, 180), k, STROKE, white)
    stroke(d, [(50, 61), (50, 76)], k, STROKE, white)
    stroke(d, [(38, 78), (62, 78)], k, STROKE, white)
    small = big.resize((int(100 * unit), int(100 * unit)), Image.LANCZOS)
    img.alpha_composite(small, (int(x0), int(y0)))


def icon(size=512):
    base = gradient(size, size).convert("RGBA")
    draw_mark(base, 0, 0, size / 100)
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=int(size * 4 * 0.22), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out


def banner(w=640, h=360):
    base = gradient(w, h).convert("RGBA")
    draw_mark(base, 28, 66, 2.3)
    d = ImageDraw.Draw(base)
    d.text((262, 136), "HyperVoice", font=ImageFont.truetype(FONT, 50), fill=(255, 255, 255, 255))
    d.text((264, 206), "Voice search for HyperOS TV", font=ImageFont.truetype(FONT_REGULAR, 23), fill=SOFT + (255,))
    return base.convert("RGB")


def vector():
    """The same drawing as an Android vector drawable (the launcher icon), computed from the numbers above."""
    def arc(r, a0, a1):
        cx, cy = CENTER
        x0, y0 = cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0))
        x1, y1 = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
        return f"M{x0:.2f},{y0:.2f}A{r},{r} 0 0 1 {x1:.2f},{y1:.2f}"

    paths = []
    for r, alpha in WAVES:
        col = "#%02X22D3EE" % int(255 * alpha)
        for a0, a1 in ((-WAVE_HALF_ANGLE, WAVE_HALF_ANGLE), (180 - WAVE_HALF_ANGLE, 180 + WAVE_HALF_ANGLE)):
            paths.append((None, col, arc(r, a0, a1)))
    x0, y0, x1, y1 = CAPSULE
    paths.append(("#FFFFFFFF", None, f"M{x0 + 12},{y0}H{x1 - 12}A12,12 0 0 1 {x1},{y0 + 12}V{y1 - 12}A12,12 0 0 1 {x1 - 12},{y1}H{x0 + 12}A12,12 0 0 1 {x0},{y1 - 12}V{y0 + 12}A12,12 0 0 1 {x0 + 12},{y0}Z"))
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           "<!-- Same drawing as docs/img/icon.png (tools/make-icons.py): a microphone with sound waves. -->",
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    xmlns:aapt="http://schemas.android.com/aapt"',
           '    android:width="108dp"', '    android:height="108dp"',
           '    android:viewportWidth="100"', '    android:viewportHeight="100">',
           '    <path android:pathData="M22,0H78A22,22 0 0 1 100,22V78A22,22 0 0 1 78,100H22A22,22 0 0 1 0,78V22A22,22 0 0 1 22,0Z">',
           '        <aapt:attr name="android:fillColor">',
           '            <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="100" android:endY="100">',
           '                <item android:offset="0" android:color="#FF0F172A" />',
           '                <item android:offset="1" android:color="#FF4338CA" />',
           '            </gradient>', '        </aapt:attr>', '    </path>']

    def stroke_path(color, data, width=STROKE):
        out.append(f'    <path android:strokeColor="{color}" android:strokeWidth="{width}" android:strokeLineCap="round" android:fillColor="#00000000" android:pathData="{data}" />')

    for fill, line, data in paths[:4]:
        stroke_path(line, data)
    out.append(f'    <path android:fillColor="#FFFFFFFF" android:pathData="{paths[4][2]}" />')
    for y in (24, 31, 38):
        stroke_path("#FF4338CA", f"M43.5,{y}H56.5", 2.6)
    stroke_path("#FFFFFFFF", f"M{50 - CRADLE_R},40A{CRADLE_R},{CRADLE_R} 0 0 0 {50 + CRADLE_R},40")
    stroke_path("#FFFFFFFF", "M50,61V76")
    stroke_path("#FFFFFFFF", "M38,78H62")
    out.append("</vector>")
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    for sub in ("docs/img", "app/src/main/res/drawable-xhdpi", "app/src/main/res/drawable"):
        os.makedirs(os.path.join(root, sub), exist_ok=True)
    icon().save(os.path.join(root, "docs/img/icon.png"))
    b = banner()
    b.save(os.path.join(root, "docs/img/banner.png"))
    b.resize((320, 180), Image.LANCZOS).save(os.path.join(root, "app/src/main/res/drawable-xhdpi/banner.png"))
    open(os.path.join(root, "app/src/main/res/drawable/ic_launcher.xml"), "w").write(vector())
    print("ok")
