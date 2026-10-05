#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Makes the README's images in docs/images from window captures.

    tools/readme_images.py [SHOTS_DIR]      (default ~/.cache/wavebalance/shots)

SHOTS_DIR holds full-screen adb captures (`adb exec-out screencap -p`) of a Googlebook
(2880x1800 px, 260 dpi) with the WaveBalance window at WINDOW, showing **simulated data**
(the S key), never a real network:

  dashboard.png networks.png optimizer.png survey.png
                          the desktop layout, one capture per screen
  medium.png              the Dashboard with the window resized to MEDIUM (side rail)
  compact.png             the Dashboard with the window resized to COMPACT (bottom bar)

The window's caption bar is cropped off (a debug build says "WaveBalance Dev" there);
each image gets rounded corners and a soft shadow. icon.png is drawn from the app's
adaptive icon vectors (needs rsvg-convert).
"""
import pathlib
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "images"
SHOTS = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else pathlib.Path.home() / ".cache/wavebalance/shots")
WINDOW = (403, 191, 2476, 1487)  # left, top, right, bottom of the desktop-size window
MEDIUM_RIGHT, COMPACT_RIGHT = 1563, 1183  # right edge when resized (same left and top)
CAPTION = 65  # px of window caption above the app
SCREENS = ["dashboard", "networks", "optimizer", "survey"]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def rounded(im, radius):
    im = im.convert("RGBA")
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius, fill=255)
    im.putalpha(ImageChops.multiply(im.getchannel("A"), mask))
    return im


def shadowed(im, blur=18, offset=(0, 10), alpha=110, pad=44):
    canvas = Image.new("RGBA", (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    a = im.getchannel("A").point(lambda v: v * alpha // 255)
    shadow.paste(Image.new("RGBA", im.size, (10, 12, 30, 255)), (pad + offset[0], pad + offset[1]), a)
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(blur)))
    canvas.alpha_composite(im, (pad, pad))
    return canvas


def app_only(name, right=WINDOW[2]):
    im = Image.open(SHOTS / f"{name}.png")
    # Two more pixels off the bottom and sides: the window's own rounded corners show the desktop.
    left, top, _, bottom = WINDOW
    return im.crop((left + 2, top + CAPTION, right - 2, bottom - 2))


def framed(im, width):
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    return shadowed(rounded(im, 16))


def save(im, name):
    im.save(OUT / name, optimize=True)
    print(OUT / name, im.size)


def vector_to_svg(*drawables):
    """Android vector drawables (paths only, as in the launcher icon) as one SVG."""
    paths = []
    for drawable in drawables:
        for p in ET.parse(drawable).getroot().iter("path"):
            a = {k.replace(ANDROID, ""): v for k, v in p.attrib.items()}
            style = {
                "d": a["pathData"],
                "fill": a.get("fillColor", "none"),
                "stroke": a.get("strokeColor", "none"),
                "stroke-width": a.get("strokeWidth", "0"),
                "stroke-linecap": a.get("strokeLineCap", "butt"),
                "stroke-linejoin": a.get("strokeLineJoin", "miter"),
                "fill-opacity": a.get("fillAlpha", "1"),
                "stroke-opacity": a.get("strokeAlpha", "1"),
            }
            # #AARRGGBB colours become #RRGGBB plus an opacity
            for key, opacity in (("fill", "fill-opacity"), ("stroke", "stroke-opacity")):
                m = re.fullmatch(r"#([0-9A-Fa-f]{2})([0-9A-Fa-f]{6})", style[key])
                if m:
                    style[key] = "#" + m[2]
                    style[opacity] = str(round(int(m[1], 16) / 255 * float(style[opacity]), 3))
            paths.append("<path " + " ".join(f'{k}="{v}"' for k, v in style.items()) + "/>")
    # The launcher shows the middle 72 of the 108-unit canvas; round it like an app tile.
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">'
            '<clipPath id="tile"><rect x="18" y="18" width="72" height="72" rx="16"/></clipPath>'
            '<g clip-path="url(#tile)">' + "".join(paths) + "</g></svg>")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    save(framed(app_only("dashboard"), 1600), "hero.png")
    # Networks spans both columns of the README's table
    save(framed(app_only("networks"), 1600), "networks.png")
    for name in SCREENS[2:]:
        save(framed(app_only(name), 1200), f"{name}.png")

    # The same Dashboard at two smaller window sizes, side by side, at the same scale.
    medium, compact = app_only("medium", MEDIUM_RIGHT), app_only("compact", COMPACT_RIGHT)
    gap = 48
    pair = Image.new("RGBA", (medium.width + gap + compact.width, medium.height), (0, 0, 0, 0))
    pair.alpha_composite(rounded(medium, 24), (0, 0))
    pair.alpha_composite(rounded(compact, 24), (medium.width + gap, 0))
    pair = pair.resize((1100, round(pair.height * 1100 / pair.width)), Image.LANCZOS)
    save(shadowed(pair), "adaptive.png")

    res = ROOT / "app/src/main/res/drawable"
    svg = OUT / "icon.svg"
    svg.write_text(vector_to_svg(res / "ic_launcher_background.xml", res / "ic_launcher_foreground.xml"))
    subprocess.run(["rsvg-convert", "-w", "256", "-h", "256", str(svg), "-o", str(OUT / "icon.png")], check=True)
    svg.unlink()
    print(OUT / "icon.png")


if __name__ == "__main__":
    main()
