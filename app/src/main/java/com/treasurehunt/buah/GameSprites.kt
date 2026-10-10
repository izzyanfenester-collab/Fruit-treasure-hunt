package com.treasurehunt.buah

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Identical transparent sprite artwork to Windows v1.3, bundled offline.
 * The full alpha-connected silhouettes are generated from the same source PNG.
 * Loaded once per game view, never decoded in the animation loop.
 */
internal class GameSprites(private val context: Context) {
    private fun load(path: String): Bitmap {
        return context.assets.open("sprites/$path").use { stream ->
            BitmapFactory.decodeStream(stream) ?: error("Gambar permainan tidak sah: $path")
        }
    }

    val baskets: Map<Basket, Bitmap> = mapOf(
        Basket.GREEN to load("baskets/bakul_hijau.png"),
        Basket.RED to load("baskets/bakul_merah.png"),
        Basket.PURPLE to load("baskets/bakul_ungu.png"),
        Basket.ORANGE to load("baskets/bakul_oren.png")
    )

    val fruits: Map<FruitKind, Bitmap> = mapOf(
        FruitKind.GREEN_APPLE to load("fruits/epal_hijau.png"),
        FruitKind.RED_APPLE to load("fruits/epal_merah.png"),
        FruitKind.GRAPES to load("fruits/anggur.png"),
        FruitKind.ORANGE to load("fruits/oren.png"),
        FruitKind.MANGO to load("fruits/mangga.png"),
        FruitKind.PEAR to load("fruits/pir.png"),
        FruitKind.STRAWBERRY to load("fruits/strawberi.png"),
        FruitKind.BANANA to load("fruits/pisang.png")
    )

    val wrong: Map<WrongKind, Bitmap> = mapOf(
        WrongKind.BOTTLE to load("wrong/botol.png"),
        WrongKind.TOY to load("wrong/mainan.png"),
        WrongKind.CAN to load("wrong/tin.png"),
        WrongKind.BOX to load("wrong/kotak.png"),
        WrongKind.SHOE to load("wrong/kasut.png")
    )
}
