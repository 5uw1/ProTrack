"""Launch-screen image set for iOS from the master icon. Run from the repo root:
python3 store/tools/make_launch_screen.py

The image is a transparent canvas of 500x1000 pt with the icon centred at 160 pt. iOS scales it
with aspect fit, so the logo comes out at roughly a third of the screen width on every device and
the transparent padding lets the LaunchBackground colour (light or dark) show through.
"""
from PIL import Image
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ICON = os.path.join(ROOT, "composeApp/icons/icon.png")
OUT = os.path.join(ROOT, "iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset")

CANVAS = (500, 1000)  # points
LOGO = 160  # points


def launch_image(scale):
    w, h = CANVAS[0] * scale, CANVAS[1] * scale
    size = LOGO * scale
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    icon = Image.open(ICON).convert("RGBA").resize((size, size), Image.LANCZOS)
    canvas.paste(icon, ((w - size) // 2, (h - size) // 2), icon)
    return canvas


os.makedirs(OUT, exist_ok=True)
images = []
for scale in (1, 2, 3):
    name = f"launch-logo@{scale}x.png"
    launch_image(scale).save(os.path.join(OUT, name))
    images.append({"idiom": "universal", "filename": name, "scale": f"{scale}x"})

with open(os.path.join(OUT, "Contents.json"), "w") as f:
    json.dump({"images": images, "info": {"author": "xcode", "version": 1}}, f, indent=2)
    f.write("\n")
print("ok")
