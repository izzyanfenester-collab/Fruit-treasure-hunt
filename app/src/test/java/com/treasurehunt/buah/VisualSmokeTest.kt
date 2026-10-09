package com.treasurehunt.buah

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowChoreographer
import java.io.File
import java.time.Duration

/** Render the actual Android views and supplied image resources, using native graphics. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w360dp-h800dp-port-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualSmokeTest {
    @Test fun storyboardAndGameScenesRenderWithDecodedArtAndBasketIcon() {
        ShadowChoreographer.setPaused(true); ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        fun views(v: View = activity.window.decorView): List<View> = listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
        fun tap(label: String) { views().filterIsInstance<Button>().single { it.text.toString() == label }.performClick() }
        val output = File("build/reports/visual-smoke").apply { mkdirs() }
        fun capture(name: String, story: Boolean) {
            val root = activity.findViewById<View>(android.R.id.content)
            root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, 1080, 1920)
            advanceFrames(32)
            if (story) {
                val image = views().filterIsInstance<ImageView>().single()
                var attempts = 0
                while (image.drawable == null && attempts++ < 50) { Thread.sleep(20); advanceFrames(16) }
                assertNotNull("Storyboard image must be decoded before rendering", image.drawable)
            }
            val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(output, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        }
        try {
            capture("title", true)
            tap("MULA BERMAIN"); tap("OREN • Oren"); capture("basket-selection", true)
            tap("MULA"); advanceFrames(3500); advanceFrames(3000)
            assertEquals(1, views().filterIsInstance<FruitGameView>().size); capture("gameplay", false)
            val state = GameState(Basket.ORANGE); state.catchFruit(FruitKind.ORANGE); state.tick(60f)
            activity.results(state); capture("results", true)
            tap("LIHAT PENAMAT"); tap("NEXT"); capture("checkout", true)
            val icon = activity.getDrawable(R.mipmap.ic_launcher)!!
            assertTrue(icon is android.graphics.drawable.AdaptiveIconDrawable)
            val iconBitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 192, 192); icon.draw(Canvas(iconBitmap))
            File(output, "icon.png").outputStream().use { assertTrue(iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { controller.pause().stop().destroy() }
    }
}
