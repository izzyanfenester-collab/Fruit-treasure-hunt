#!/usr/bin/env python3
"""Split the user-provided transparent 4x8x5 game sprite sheet into 17 PNG sprites.

Source is designed at 1448x1086. Crop coordinates scale automatically if
the uploaded PNG has a different resolution with the same aspect ratio.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/assets/glossy_grocery_game_icon_set.png"
DEST = ROOT / "src/assets/sprites"
BASE_W, BASE_H = 1448, 1086

# (output filename, bounding rectangle in original sheet)
CELLS = {
    "baskets/bakul_hijau.png": (0, 38, 363, 404),
    "baskets/bakul_merah.png": (361, 38, 719, 404),
    "baskets/bakul_ungu.png": (717, 38, 1081, 404),
    "baskets/bakul_oren.png": (1079, 38, 1448, 404),
    "fruits/epal_hijau.png": (0, 424, 193, 709),
    "fruits/epal_merah.png": (191, 424, 371, 709),
    "fruits/anggur.png": (369, 424, 550, 709),
    "fruits/oren.png": (548, 424, 732, 709),
    "fruits/mangga.png": (730, 424, 913, 709),
    "fruits/pir.png": (911, 424, 1089, 709),
    "fruits/strawberi.png": (1087, 424, 1257, 709),
    "fruits/pisang.png": (1255, 424, 1448, 709),
    "wrong/botol.png": (0, 714, 238, 1086),
    "wrong/kasut.png": (236, 714, 551, 1086),
    "wrong/tin.png": (549, 714, 753, 1086),
    "wrong/kotak.png": (751, 714, 1104, 1086),
    "wrong/mainan.png": (1102, 714, 1448, 1086),
}

def main():
    if not SRC.is_file():
        raise FileNotFoundError(f"Fail PNG tidak dijumpai: {SRC}")
    with Image.open(SRC) as source:
        print(f"Sprite sheet: {source.size}, mode={source.mode}")
        if source.mode != "RGBA" and "transparency" not in source.info:
            print("AMARAN: Fail PNG asal tidak mempunyai alpha/transparency.")
        original = source.convert("RGBA")
        fw, fh = original.size
        if abs(fw / fh - BASE_W / BASE_H) > 0.07:
            raise ValueError("Nisbah PNG berbeza daripada sprite sheet 4:3 yang dijangka")
        for relative, (l, t, r, b) in CELLS.items():
            scaled = (
                max(0, round(l * fw / BASE_W)),
                max(0, round(t * fh / BASE_H)),
                min(fw, round(r * fw / BASE_W)),
                min(fh, round(b * fh / BASE_H)),
            )
            sprite = original.crop(scaled)
            alpha = sprite.getchannel("A")
            # Use visible alpha to remove large transparent borders, while
            # leaving a tiny margin so leaves/handles won't be clipped.
            bbox = alpha.point(lambda x: 255 if x > 12 else 0).getbbox()
            if bbox is None:
                raise ValueError(f"Sprite kosong: {relative}")
            margin = 3
            sprite = sprite.crop((
                max(0, bbox[0] - margin),
                max(0, bbox[1] - margin),
                min(sprite.width, bbox[2] + margin),
                min(sprite.height, bbox[3] + margin),
            ))
            output = DEST / relative
            output.parent.mkdir(parents=True, exist_ok=True)
            sprite.save(output, "PNG", optimize=True)
            print(f"{relative}: {sprite.size}")

    output_count = len(list(DEST.rglob("*.png")))
    if output_count != len(CELLS):
        raise RuntimeError(f"Dijangka 17 sprite, jumpa {output_count}")
    print(f"Berjaya: {output_count} sprites")

if __name__ == "__main__":
    main()
