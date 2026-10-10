#!/usr/bin/env python3
"""Extract the 17 transparent sprites without slicing adjacent objects.

The artist's PNG is laid out in three rows. Vertical boundaries are selected at
transparent gaps *from actual pixel data* instead of old hard-coded cell cuts
that created unwanted bits of neighbouring fruit.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/assets/glossy_grocery_game_icon_set.png"
OUTPUT = ROOT / "src/assets/sprites"
EXPECTED_ASPECT = 1448 / 1086
PADDING = 16
ALPHA_THRESHOLD = 24

# Each row: (top in source pixels, bottom, nominal x dividers, filenames).
# The search around each nominal divider prevents pieces of adjacent images
# becoming stuck to fruits, grapes, strawberry, shoes and other items.
LAYOUT = [
    (
        0, 414,
        [363, 719, 1081],
        ["baskets/bakul_hijau.png", "baskets/bakul_merah.png",
         "baskets/bakul_ungu.png", "baskets/bakul_oren.png"],
    ),
    (
        415, 713,
        [192, 370, 549, 731, 912, 1088, 1256],
        ["fruits/epal_hijau.png", "fruits/epal_merah.png",
         "fruits/anggur.png", "fruits/oren.png", "fruits/mangga.png",
         "fruits/pir.png", "fruits/strawberi.png", "fruits/pisang.png"],
    ),
    (
        714, 1086,
        [237, 550, 752, 1103],
        ["wrong/botol.png", "wrong/kasut.png", "wrong/tin.png",
         "wrong/kotak.png", "wrong/mainan.png"],
    ),
]


def get_gap(alpha, nominal_x, top, bottom, img_width, max_offset):
    """Find the column containing least visible alpha near a cell divider."""
    left = max(1, nominal_x - max_offset)
    right = min(img_width - 2, nominal_x + max_offset)
    pixels = alpha.load()
    results = []
    for x in range(left, right + 1):
        # A separator is safe only if pixels at x/x+1 are transparent. Strong
        # penalty for actual objects crossing the cut and a weak centring tie.
        hits = sum(
            1 for y in range(top, bottom)
            if pixels[x, y] > ALPHA_THRESHOLD
            or pixels[x + 1, y] > ALPHA_THRESHOLD
        )
        results.append((hits, abs(x - nominal_x), x))
    hits, _distance, divider = min(results)
    print(f"Divider near {nominal_x}: using x={divider}, alpha-row hits={hits}")
    return divider, hits


def save_sprite(source, bounds, filename):
    tile = source.crop(bounds)
    bbox = tile.getchannel("A").point(
        lambda v: 255 if v > ALPHA_THRESHOLD else 0
    ).getbbox()
    if not bbox:
        raise RuntimeError(f"Tiada objek dalam sprite {filename}")

    trimmed = tile.crop(bbox)
    # Explicit transparent border OUTSIDE the crop. Merely expanding the old
    # fixed cell was insufficient, because it included adjacent object shards.
    out = Image.new(
        "RGBA",
        (trimmed.width + PADDING * 2, trimmed.height + PADDING * 2),
        (0, 0, 0, 0),
    )
    out.alpha_composite(trimmed, (PADDING, PADDING))

    # Assert the visible sprite never touches the final PNG edges.
    pixels = out.getchannel("A")
    assert pixels.crop((0, 0, out.width, PADDING)).getbbox() is None
    assert pixels.crop((0, out.height-PADDING, out.width, out.height)).getbbox() is None
    assert pixels.crop((0, 0, PADDING, out.height)).getbbox() is None
    assert pixels.crop((out.width-PADDING, 0, out.width, out.height)).getbbox() is None

    dest = OUTPUT / filename
    dest.parent.mkdir(parents=True, exist_ok=True)
    out.save(dest, "PNG", optimize=True)
    print(f"Sprite: {filename} {out.size}")


def main():
    if not SOURCE.exists():
        raise FileNotFoundError(f"Fail sumber tiada: {SOURCE}")

    with Image.open(SOURCE) as original:
        source = original.convert("RGBA")
    w, h = source.size
    if abs(w / h - EXPECTED_ASPECT) > .07:
        raise ValueError(f"Unexpected source sheet dimensions: {w}x{h}")
    sx, sy = w / 1448, h / 1086
    alpha = source.getchannel("A")
    separator_warnings = []
    for row_number, (top, bottom, dividers, names) in enumerate(LAYOUT, 1):
        y0 = max(0, round(top * sy))
        y1 = min(h, round(bottom * sy))
        x_cuts = [0]
        # Choose the centre of actual transparent gaps and record if the
        # original art touches neighbouring objects at a potential boundary.
        for nominal in dividers:
            divider, hits = get_gap(
                alpha, round(nominal * sx), y0, y1, w,
                max(8, round(27 * sx)),
            )
            x_cuts.append(divider)
            if hits:
                separator_warnings.append((row_number, nominal, hits))
        x_cuts.append(w)
        if len(x_cuts) - 1 != len(names):
            raise ValueError("Sprite sheet segment count mismatch")
        for i, name in enumerate(names):
            save_sprite(source, (x_cuts[i], y0, x_cuts[i+1], y1), name)

    count = sum(1 for _ in OUTPUT.rglob("*.png"))
    if count != 17:
        raise RuntimeError(f"Dijangka 17 PNG, ada {count}")
    print(f"Berjaya: {count} sprites, masing-masing dengan {PADDING}px ruang lutsinar")
    if separator_warnings:
        for row, nominal, hits in separator_warnings:
            print(f"AMARAN: sempadan baris {row} x={nominal} ada {hits} pixel alpha")
    else:
        print("Semua pemisah ialah jurang lutsinar: tiada objek jiran terpotong.")


if __name__ == "__main__":
    main()
