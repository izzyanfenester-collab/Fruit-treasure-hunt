package com.treasurehunt.buah

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.concurrent.Executors

/** One optimised scene is decoded in the background; originals and videos are never decoded here. */
class StoryboardView(context: Context, assetName: String, fallback: Scene) : FrameLayout(context) {
    private val resourceId = StoryAssets.images.getValue(assetName)
    val hasStoryboardAsset get() = resourceId != 0
    private val imageView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private var generation = 0
    init {
        tag = assetName
        contentDescription = fallback.description
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        addView(imageView, LayoutParams(-1, -1))
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (resourceId == 0 || w == 0 || h == 0) return
        val request = ++generation
        val appResources = context.applicationContext.resources
        decoder.execute {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(appResources, resourceId, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
            val bitmap = BitmapFactory.decodeResource(appResources, resourceId,
                BitmapFactory.Options().apply { inSampleSize = sample })
            post { if (generation == request && isAttachedToWindow) imageView.setImageBitmap(bitmap) }
        }
    }
    override fun onDetachedFromWindow() { generation++; imageView.setImageDrawable(null); super.onDetachedFromWindow() }
    companion object {
        private val decoder = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "story-scene-decoder").apply { isDaemon = true } }
    }
}
