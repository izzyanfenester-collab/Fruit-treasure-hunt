package com.treasurehunt.buah

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], qualifiers = "w360dp-h800dp-port")
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    private lateinit var controller: ActivityController<MainActivity>
    private val activity get() = controller.get()
    @Before fun open() { ShadowChoreographer.setPaused(true); ShadowChoreographer.setFrameDelay(Duration.ofMillis(16)); controller = Robolectric.buildActivity(MainActivity::class.java).setup() }
    @After fun close() { controller.pause().stop().destroy() }
    private fun views(view: View = activity.window.decorView): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun tap(label: String, root: View = activity.window.decorView) {
        views(root).filterIsInstance<Button>().single { it.text.toString() == label }.performClick()
    }
    private fun pauseTap(label: String) = tap(label, ShadowAlertDialog.getLatestAlertDialog().window!!.decorView)
    private fun hasText(value: String) = views().filterIsInstance<TextView>().any { it.text.contains(value) }
    private fun asset(): String {
        val view = views().filterIsInstance<StoryboardView>().single()
        assertTrue("Story screen must use its supplied image resource", view.hasStoryboardAsset)
        return view.tag.toString()
    }
    private fun startRound(): FruitGameView {
        tap("MULA BERMAIN"); tap("MULA")
        advanceFrames(3500)
        return views().filterIsInstance<FruitGameView>().single()
    }

    @Test fun launchOpensMalayMainMenuWithRequestedButtonOrder() {
        assertEquals("story_fruit_hunt", asset())
        val labels = views().filterIsInstance<Button>().map { it.text.toString() }
        assertEquals(listOf("CERITA", "CARA BERMAIN", "MULA BERMAIN", "SKOR TERTINGGI", "BUNYI ON"), labels)
        tap("CARA BERMAIN")
        assertTrue(hasText("Tangkap buah-buahan sahaja."))
        assertTrue(hasText("Elak tangkap selain buah."))
        tap("MENU UTAMA")
        assertTrue(hasText("MULA BERMAIN"))
    }

    @Test fun menuIsShownOnSubsequentLaunchesWithoutIntro() {
        assertTrue(hasText("CERITA"))
        controller.pause().stop().destroy()
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue(hasText("MULA BERMAIN"))
        assertFalse(hasText("SKIP INTRO"))
    }

    @Test fun basketSelectionHighlightsChoiceAndCountdownPrecedesGameplay() {
        tap("MULA BERMAIN"); tap("OREN • Oren")
        assertTrue(hasText("✓ OREN • Oren")); tap("MULA")
        advanceFrames(100)
        assertEquals("story_countdown", asset()); assertTrue(hasText("3..."))
        assertTrue(views().none { it is FruitGameView })
        advanceFrames(3400)
        assertTrue(views().filterIsInstance<FruitGameView>().single().isRunning)
        assertTrue(hasText("MASA: 60")); assertTrue(hasText("SKOR: 000"))
    }

    @Test fun pauseFreezesTimerAndSoundToggleKeepsGamePaused() {
        val round = startRound(); tap("JEDA"); assertFalse(round.isRunning)
        val timer = views().filterIsInstance<TextView>().single { it.text.contains("MASA:") }.text.toString()
        advanceFrames(3000)
        assertTrue(hasText(timer)); pauseTap("BUNYI ON"); assertFalse(round.isRunning)
        pauseTap("SAMBUNG"); assertTrue(round.isRunning)
        tap("JEDA"); pauseTap("MENU UTAMA"); assertFalse(round.isRunning); assertTrue(hasText("MULA BERMAIN"))
    }

    @Test fun restartAndBackgroundPauseRequireFreshCountdownAndExplicitResume() {
        val oldRound = startRound(); tap("JEDA"); pauseTap("MULA SEMULA")
        assertFalse(oldRound.isRunning); assertEquals("story_countdown", asset())
        advanceFrames(3500)
        val newRound = views().filterIsInstance<FruitGameView>().single()
        assertNotSame(oldRound, newRound); assertTrue(newRound.isRunning)
        controller.pause(); assertFalse(newRound.isRunning)
        controller.resume(); assertFalse(newRound.isRunning)
        assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing)
        pauseTap("SAMBUNG"); assertTrue(newRound.isRunning)
    }

    @Test fun successResultsShowStatisticsAndAllThreeEndingScenes() {
        val state = GameState(Basket.RED)
        state.catchFruit(FruitKind.RED_APPLE); state.catchFruit(FruitKind.MANGO); state.catchWrong(); state.tick(60f)
        activity.results(state)
        assertTrue(hasText("TAHNIAH!")); assertTrue(hasText("Skor akhir: 30"))
        assertTrue(hasText("Buah ditangkap: 2")); assertTrue(hasText("Buah bonus: 1")); assertTrue(hasText("Kesalahan: 1"))
        tap("LIHAT PENAMAT"); assertEquals("story_finish", asset())
        tap("SETERUSNYA"); assertEquals("story_checkout", asset())
        tap("SETERUSNYA"); assertEquals("story_celebration", asset()); assertTrue(hasText("ANDA TELAH MENYELESAIKAN"))
        tap("MENU UTAMA"); assertTrue(hasText("MULA BERMAIN"))
    }

    @Test fun gameOverResultsDoNotOfferSuccessfulEnding() {
        val state = GameState(Basket.GREEN); repeat(3) { state.catchWrong() }
        activity.results(state)
        assertTrue(hasText("PERMAINAN TAMAT")); assertTrue(hasText("Kesalahan: 3")); assertFalse(hasText("LIHAT PENAMAT"))
    }
}
