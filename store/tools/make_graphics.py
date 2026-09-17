"""Store graphics from the master icon: full-bleed icons for Play (512) and App Store (1024),
and the Play feature graphic (1024x500). Run from the repo root: python3 store/tools/make_graphics.py"""
from PIL import Image, ImageDraw, ImageFont
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ICON = os.path.join(ROOT, "composeApp/icons/icon.png")
OUT = os.path.join(ROOT, "store")
BLUE_A, BLUE_B = (0x2F, 0x6B, 0xFF), (0x16, 0x38, 0xB8)


def gradient(w, h):
    """Same diagonal gradient as icon.svg (top-left #2F6BFF to bottom-right #1638B8)."""
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / (w - 1) + y / (h - 1)) / 2
            px[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(BLUE_A, BLUE_B))
    return img


def full_bleed_icon(size):
    """The rounded icon on top of its own gradient, so the corners are filled (stores mask the shape themselves)."""
    base = gradient(1024, 1024)
    icon = Image.open(ICON).convert("RGBA")
    base.paste(icon, (0, 0), icon)
    return base.resize((size, size), Image.LANCZOS)


def font(size, bold=False):
    for path, idx in (("/System/Library/Fonts/HelveticaNeue.ttc", 1 if bold else 0), ("/System/Library/Fonts/Helvetica.ttc", 1 if bold else 0)):
        if os.path.exists(path):
            try:
                f = ImageFont.truetype(path, size, index=idx)
                return f
            except OSError:
                continue
    return ImageFont.load_default()


def feature_graphic():
    w, h = 1024, 500
    img = gradient(w, h)
    icon = Image.open(ICON).convert("RGBA").resize((300, 300), Image.LANCZOS)
    img.paste(icon, (80, 100), icon)
    d = ImageDraw.Draw(img)
    d.text((430, 150), "WorkTracker", font=font(84, bold=True), fill="white")
    d.text((434, 262), "Clock in. Track projects.", font=font(40), fill=(230, 236, 255))
    d.text((434, 312), "Book your hours in minutes.", font=font(40), fill=(230, 236, 255))
    return img


os.makedirs(os.path.join(OUT, "icons"), exist_ok=True)
os.makedirs(os.path.join(OUT, "graphics"), exist_ok=True)
full_bleed_icon(512).save(os.path.join(OUT, "icons/play-icon-512.png"))
full_bleed_icon(1024).save(os.path.join(OUT, "icons/appstore-icon-1024.png"))
feature_graphic().save(os.path.join(OUT, "graphics/play-feature-graphic-1024x500.png"))
print("ok")
