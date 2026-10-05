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
@Config(sdk = [24], qualifiers = "w960dp-h480dp-land")
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
        tap("SKIP INTRO"); tap("MULA BERMAIN"); tap("MULA")
        advanceFrames(3500)
        return views().filterIsInstance<FruitGameView>().single()
    }

    @Test fun introUsesStoryboardScenesAndLeadsThroughInstructionsToBasketSelection() {
        assertEquals("story_intro", asset()); tap("NEXT")
        listOf("story_supermarket", "story_enter", "story_briefing", "story_instruction", "story_teamwork").forEach { name ->
            assertEquals(name, asset()); tap("NEXT")
        }
        assertEquals("story_ready", asset()); tap("CARA BERMAIN")
        assertTrue(hasText("Tangkap buah-buahan sahaja.")); assertTrue(hasText("Elak tangkap selain buah."))
        tap("MULA"); assertEquals("story_baskets", asset())
        assertTrue(activity.getPreferences(0).getBoolean("intro_seen", false))
    }

    @Test fun skipIntroPersistsAndNextLaunchOpensMainMenu() {
        tap("SKIP INTRO"); assertTrue(hasText("MULA BERMAIN"))
        controller.pause().stop().destroy()
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertTrue(hasText("MULA BERMAIN")); assertFalse(hasText("SKIP INTRO"))
    }

    @Test fun basketSelectionHighlightsChoiceAndCountdownPrecedesGameplay() {
        tap("SKIP INTRO"); tap("MULA BERMAIN"); tap("OREN • Oren")
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
        tap("NEXT"); assertEquals("story_checkout", asset())
        tap("NEXT"); assertEquals("story_celebration", asset()); assertTrue(hasText("ANDA TELAH MENYELESAIKAN"))
        tap("MENU UTAMA"); assertTrue(hasText("MULA BERMAIN"))
    }

    @Test fun gameOverResultsDoNotOfferSuccessfulEnding() {
        val state = GameState(Basket.GREEN); repeat(3) { state.catchWrong() }
        activity.results(state)
        assertTrue(hasText("GAME OVER")); assertTrue(hasText("Kesalahan: 3")); assertFalse(hasText("LIHAT PENAMAT"))
    }
}
