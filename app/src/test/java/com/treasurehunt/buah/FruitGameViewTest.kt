package com.treasurehunt.buah

import android.content.Context
import android.os.Looper
import android.view.MotionEvent
import android.view.View
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
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], qualifiers = "w960dp-h480dp-land")
@LooperMode(LooperMode.Mode.PAUSED)
class FruitGameViewTest {
    private lateinit var controller: ActivityController<MainActivity>
    private var latest: GameState? = null
    private var result: GameState? = null
    private val feedback = mutableListOf<Feedback>()
    @Before fun open() { ShadowChoreographer.setPaused(true); ShadowChoreographer.setFrameDelay(Duration.ofMillis(16)); controller = Robolectric.buildActivity(MainActivity::class.java).setup() }
    @After fun close() { controller.pause().stop().destroy() }
    private fun game(random: Random): FruitGameView {
        val view = FruitGameView(controller.get() as Context, Basket.GREEN, { latest = it }, { feedback += it }, { result = it }, random)
        controller.get().setContentView(view)
        view.measure(View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 960, 400)
        return view
    }
    private fun advance(seconds: Long) { advanceFrames(seconds * 1000) }
    private val leftWrong = object : Random() { override fun nextBits(bitCount: Int) = 0 }
    private val middleFruit = object : Random() {
        override fun nextBits(bitCount: Int) = if (bitCount == 24) 1 shl 23 else 0
    }
    private val middleWrong = object : Random() {
        private var floats = 0
        override fun nextBits(bitCount: Int): Int {
            if (bitCount != 24) return 0
            return if (floats++ % 3 == 1) 0 else 1 shl 23
        }
    }

    @Test fun roundLastsSixtySecondsAndTenSecondWarningFiresOnce() {
        game(leftWrong); advance(30)
        assertEquals(30f, latest!!.remaining, .1f); assertNull(result)
        advance(21); assertEquals(1, feedback.count { it == Feedback.WARNING })
        advance(10); assertNotNull(result); assertEquals(0f, result!!.remaining)
        assertEquals(3, result!!.lives); assertFalse(result!!.score > 0)
    }
    @Test fun catchingMatchingFruitAwardsTwentyAndShowsBonusFeedback() {
        game(middleFruit); advance(9)
        assertTrue(latest!!.fruitsCaught > 0); assertEquals(latest!!.fruitsCaught * 20, latest!!.score)
        assertEquals(latest!!.fruitsCaught, latest!!.bonusCaught); assertTrue(feedback.contains(Feedback.BONUS))
    }
    @Test fun thirdWrongObjectEndsRoundImmediately() {
        game(middleWrong); advance(14)
        assertNotNull(result); assertEquals(0, result!!.lives); assertEquals(3, result!!.mistakes)
        assertEquals(0, result!!.score); assertTrue(result!!.remaining > 0)
    }
    @Test fun pauseStopsTimerAndResumeDoesNotCountBackgroundTime() {
        val view = game(leftWrong); advance(2); view.pause()
        val paused = latest!!.remaining; advance(10); assertEquals(paused, latest!!.remaining)
        view.resume(); advance(2); assertEquals(paused - 2, latest!!.remaining, .1f)
    }
    @Test fun draggingPastLeftBoundaryStillCatchesObjectsAtLeftEdge() {
        val view = game(leftWrong)
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, -1000f, 200f, 0)
        view.onTouchEvent(event); event.recycle(); advance(14)
        assertNotNull(result); assertEquals(3, result!!.mistakes)
    }
}
