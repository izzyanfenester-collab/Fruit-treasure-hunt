package com.treasurehunt.buah

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class GameSpritesTest {
    @Test fun allSeventeenWindowsArtworkSpritesLoadOnAndroidWithoutClippedEdges() {
        val sprites = GameSprites(RuntimeEnvironment.getApplication())
        assertEquals(4, sprites.baskets.size)
        assertEquals(8, sprites.fruits.size)
        assertEquals(5, sprites.wrong.size)
        (sprites.baskets.values + sprites.fruits.values + sprites.wrong.values).forEach { bitmap ->
            assertTrue(bitmap.width > 30)
            assertTrue(bitmap.height > 30)
            for (x in 0 until bitmap.width) {
                assertEquals(0, Color.alpha(bitmap.getPixel(x, 0)))
                assertEquals(0, Color.alpha(bitmap.getPixel(x, bitmap.height - 1)))
            }
            for (y in 0 until bitmap.height) {
                assertEquals(0, Color.alpha(bitmap.getPixel(0, y)))
                assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width - 1, y)))
            }
        }
    }
}
