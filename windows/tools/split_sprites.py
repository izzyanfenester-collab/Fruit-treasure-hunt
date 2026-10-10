#!/usr/bin/env python3
"""Safely isolate 17 cartoon sprites with alpha-connected components.

The supplied PNG has adjacent sprites whose X extents overlap, so vertical
rectangular cuts inevitably slice grapes, strawberries and banana. Here each
object is extracted by its own 8-connected visible alpha silhouette.
"""
from pathlib import Path
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/assets/glossy_grocery_game_icon_set.png"
DEST = ROOT / "src/assets/sprites"
PADDING = 16
ALPHA_THRESHOLD = 32

ROWS = [
    (0, 414, [
        "baskets/bakul_hijau.png",
        "baskets/bakul_merah.png",
        "baskets/bakul_ungu.png",
        "baskets/bakul_oren.png",
    ]),
    (415, 713, [
        "fruits/epal_hijau.png",
        "fruits/epal_merah.png",
        "fruits/anggur.png",
        "fruits/oren.png",
        "fruits/mangga.png",
        "fruits/pir.png",
        "fruits/strawberi.png",
        "fruits/pisang.png",
    ]),
    (714, 1086, [
        "wrong/botol.png",
        "wrong/kasut.png",
        "wrong/tin.png",
        "wrong/kotak.png",
        "wrong/mainan.png",
    ]),
]


def components(alpha_image):
    """Return substantial 8-connected alpha groups, with exact pixel indices."""
    width, height = alpha_image.size
    opaque = alpha_image.tobytes()
    visited = bytearray(len(opaque))
    groups = []
    for starting in range(len(opaque)):
        if visited[starting] or opaque[starting] <= ALPHA_THRESHOLD:
            continue
        visited[starting] = 1
        pending = [starting]
        members = []
        left, top, right, bottom = width, height, 0, 0
        while pending:
            index = pending.pop()
            y, x = divmod(index, width)
            members.append(index)
            left = min(left, x)
            right = max(right, x)
            top = min(top, y)
            bottom = max(bottom, y)

            # Check 8 neighbours so thin stems and diagonal leaves stay
            # attached to their parent fruit.
            for yy in (y - 1, y, y + 1):
                if yy < 0 or yy >= height:
                    continue
                for xx in (x - 1, x, x + 1):
                    if xx < 0 or xx >= width or (xx == x and yy == y):
                        continue
                    adjacent = yy * width + xx
                    if not visited[adjacent] and opaque[adjacent] > ALPHA_THRESHOLD:
                        visited[adjacent] = 1
                        pending.append(adjacent)
        if len(members) >= 1000:
            groups.append((members, (left, top, right + 1, bottom + 1)))

    return groups


def export_sprite(source, alpha_row_top, row_width, component, filename):
    members, (left, top, right, bottom) = component
    # Crop only near THIS object's actual silhouette. Every other item is
    # removed using the component mask even when two bounding boxes overlap.
    spread = 4
    crop_left = max(0, left - spread)
    crop_top = max(0, top - spread)
    crop_right = min(row_width, right + spread)
    crop_bottom = min(source.height-alpha_row_top, bottom + spread)
    crop_width = crop_right - crop_left
    crop_height = crop_bottom - crop_top
    local_mask = bytearray(crop_width * crop_height)
    for index in members:
        y, x = divmod(index, row_width)
        if crop_left <= x < crop_right and crop_top <= y < crop_bottom:
            local_mask[(y - crop_top) * crop_width + x - crop_left] = 255

    # Restore 1–2 pixel antialiased fringes around each silhouette.
    mask = Image.frombytes("L", (crop_width, crop_height), bytes(local_mask))
    mask = mask.filter(ImageFilter.MaxFilter(5))
    original_pixels = source.crop(
        (crop_left, crop_top + alpha_row_top,
         crop_right, crop_bottom + alpha_row_top)
    )
    isolated = Image.new("RGBA", (crop_width, crop_height), (0, 0, 0, 0))
    isolated.paste(original_pixels, (0, 0), mask)

    visible = isolated.getchannel("A").getbbox()
    if not visible:
        raise RuntimeError(f"Tiada piksel kelihatan untuk {filename}")
    trimmed = isolated.crop(visible)
    sprite = Image.new("RGBA", (
        trimmed.width + 2 * PADDING,
        trimmed.height + 2 * PADDING
    ), (0, 0, 0, 0))
    sprite.alpha_composite(trimmed, (PADDING, PADDING))

    # The entire 16-pixel margin must remain empty.
    alpha = sprite.getchannel("A")
    assert alpha.crop((0, 0, sprite.width, PADDING)).getbbox() is None
    assert alpha.crop((0, sprite.height-PADDING, sprite.width, sprite.height)).getbbox() is None
    assert alpha.crop((0, 0, PADDING, sprite.height)).getbbox() is None
    assert alpha.crop((sprite.width-PADDING, 0, sprite.width, sprite.height)).getbbox() is None

    destination = DEST / filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    sprite.save(destination, "PNG", optimize=True)
    print(f"{filename}: pixels={len(members)}, bbox={(left,top,right,bottom)}, output={sprite.size}")


def main():
    if not SOURCE.is_file():
        raise FileNotFoundError(f"Fail PNG sprite tidak dijumpai: {SOURCE}")
    with Image.open(SOURCE) as picture:
        source = picture.convert("RGBA")

    sheet_width, sheet_height = source.size
    if abs(sheet_width / sheet_height - 1448 / 1086) > .07:
        raise ValueError(f"Unexpected sprite-sheet size: {sheet_width}x{sheet_height}")
    scale_y = sheet_height / 1086

    # Remove earlier generated PNGs before extracting, avoiding stale assets.
    if DEST.exists():
        for png in DEST.rglob("*.png"):
            png.unlink()

    for nominal_top, nominal_bottom, filenames in ROWS:
        top = round(nominal_top * scale_y)
        bottom = min(sheet_height, round(nominal_bottom * scale_y))
        stripe = source.crop((0, top, sheet_width, bottom))
        groups = components(stripe.getchannel("A"))
        groups = sorted(groups, key=lambda item: len(item[0]), reverse=True)
        if len(groups) < len(filenames):
            raise RuntimeError(
                f"Baris {nominal_top}: hanya ada {len(groups)} objek, "
                f"dijangka {len(filenames)}. Tidak mahu bina PNG terpotong."
            )
        groups = groups[:len(filenames)]
        groups.sort(key=lambda item: (item[1][0] + item[1][2]) / 2)
        print(f"Baris {nominal_top}-{nominal_bottom}: {len(groups)} bentuk bersambung")
        for component, filename in zip(groups, filenames):
            export_sprite(source, top, sheet_width, component, filename)

    all_png = list(DEST.rglob("*.png"))
    if len(all_png) != 17:
        raise RuntimeError(f"Hanya ada {len(all_png)}/17 fail PNG")
    print("BERJAYA: 17/17 objek dipisahkan menggunakan alpha 8-connected; tiada potongan petak.")


if __name__ == "__main__":
    main()
