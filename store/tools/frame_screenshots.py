"""Marketing versions of the raw screenshots: caption on the app gradient, screenshot scaled below
with rounded corners. Play phone shots become 9:16 (1080x1920); App Store shots keep their exact
device size. Run after the capture scripts: python3 store/tools/frame_screenshots.py"""
from PIL import Image, ImageDraw, ImageFont
import glob, os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHOTS = os.path.join(ROOT, "store/screenshots")
BLUE_A, BLUE_B = (0x2F, 0x6B, 0xFF), (0x16, 0x38, 0xB8)
CAPTIONS = {
    "en": {"today": "Clock in, pick a task – done", "reports": "Month-end hours per project", "tasks": "Projects, tasks and deadlines", "settings": "Backup to Drive or iCloud"},
    "de": {"today": "Einstempeln, Aufgabe wählen – fertig", "reports": "Monatsstunden pro Projekt", "tasks": "Projekte, Aufgaben und Termine", "settings": "Sicherung in Drive oder iCloud"},
    "fr": {"today": "Pointer, choisir une tâche – fini", "reports": "Heures du mois par projet", "tasks": "Projets, tâches et délais", "settings": "Sauvegarde sur Drive ou iCloud"},
}


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / (w - 1) + y / (h - 1)) / 2
            px[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(BLUE_A, BLUE_B))
    return img


def font(size):
    for path in ("/System/Library/Fonts/HelveticaNeue.ttc", "/System/Library/Fonts/Helvetica.ttc"):
        if os.path.exists(path):
            return ImageFont.truetype(path, size, index=1)
    return ImageFont.load_default()


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def frame(src, dst, canvas_w, canvas_h, caption):
    shot = Image.open(src).convert("RGB")
    bg = gradient(canvas_w, canvas_h)
    d = ImageDraw.Draw(bg)
    f = font(int(canvas_w * 0.055))
    # Caption, wrapped to two lines at most.
    words, lines, line = caption.split(), [], ""
    for w in words:
        trial = (line + " " + w).strip()
        if d.textlength(trial, font=f) > canvas_w * 0.86 and line:
            lines.append(line); line = w
        else:
            line = trial
    lines.append(line)
    y = int(canvas_h * 0.045)
    for ln in lines:
        d.text(((canvas_w - d.textlength(ln, font=f)) / 2, y), ln, font=f, fill="white")
        y += int(f.size * 1.25)
    top = y + int(canvas_h * 0.03)
    # Screenshot scaled to the remaining height, bleeding off the bottom edge slightly like a device in hand.
    target_h = canvas_h - top + int(canvas_h * 0.04)
    scale = min(target_h / shot.height, (canvas_w * 0.86) / shot.width)
    resized = shot.resize((round(shot.width * scale), round(shot.height * scale)), Image.LANCZOS)
    # Phones get a generous radius; tablets a small one so the status bar text is not clipped.
    resized = rounded(resized, int(canvas_w * (0.06 if shot.height > shot.width * 1.6 else 0.025)))
    bg.paste(resized, ((canvas_w - resized.width) // 2, top), resized)
    bg.save(dst)


count = 0
for src in sorted(glob.glob(os.path.join(SHOTS, "**", "*.png"), recursive=True)):
    if "/framed/" in src:
        continue
    lang = os.path.basename(os.path.dirname(src))
    tab = os.path.splitext(os.path.basename(src))[0].split("-", 1)[1]
    caption = CAPTIONS.get(lang, {}).get(tab)
    if not caption:
        continue
    shot = Image.open(src)
    if "/android/" in src:
        w, h = 1080, 1920          # Play: 9:16
    else:
        w, h = shot.size           # App Store: exact device size
    dst = os.path.join(os.path.dirname(src), "framed", os.path.basename(src))
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    frame(src, dst, w, h, caption)
    count += 1
print("framed", count)
