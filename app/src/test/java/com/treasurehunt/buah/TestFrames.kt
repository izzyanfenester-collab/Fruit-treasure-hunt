package com.treasurehunt.buah

import android.os.Looper
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** Paused Robolectric vsync needs discrete clock advances to deliver every animation frame. */
internal fun advanceFrames(milliseconds: Long) {
    var remaining = milliseconds
    while (remaining > 0) {
        val step = minOf(16L, remaining)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
        remaining -= step
    }
}
