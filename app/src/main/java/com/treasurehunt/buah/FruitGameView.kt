package com.treasurehunt.buah

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.random.Random

/** Keep the working basket mechanic; static art is cached and round time uses a monotonic clock. */
class FruitGameView(context: Context, private val basket: Basket,
    private val update: (GameState) -> Unit, private val feedbackSound: (Feedback) -> Unit,
    private val finish: (GameState) -> Unit, private val random: Random = Random.Default) : View(context) {
    private data class Drop(val x: Float, var y: Float, val fruit: FruitKind?, val wrong: WrongKind, val speed: Float)
    private data class Points(val x: Float, var y: Float, val label: String, var life: Float = .8f)
    private val state = GameState(basket)
    private val drops = mutableListOf<Drop>()
    private val floatingPoints = mutableListOf<Points>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val art = SceneArt()
    // The same 17 fully isolated PNG objects used by the Windows version.
    private val sprites = GameSprites(context)
    private val fruitSprites = sprites.fruits
    private val wrongSprites = sprites.wrong
    private val basketSprites = sprites.baskets
    private var backgroundScene: Bitmap? = null
    private val lighting = BitmapFactory.decodeResource(resources, R.drawable.gameplay_lighting)
    private val teacherWarning = BitmapFactory.decodeResource(resources, R.drawable.teacher_warning)
    private var basketX = 360f
    private var targetX = 360f
    private var lastFrame = 0L
    private var spawn = .6f
    private var running = true
    private var finished = false
    private var disposed = false
    private var message = "Seret bakul ke kiri dan kanan ↔"
    private var messageTime = 2f
    private var mistakeTime = 0f
    private var warningTime = 0f
    private var warned = false
    private var lastSecond = 60
    private var worldHeight = 280f
    private val worldWidth = 720f
    val isRunning get() = running && !finished && !disposed
    val hasEnded get() = state.ended
    private val endRound = Runnable { if (!disposed && isAttachedToWindow) finish(state) }

    private val loop = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val now = SystemClock.elapsedRealtimeNanos()
            val elapsed = if (lastFrame == 0L) 0f else (now - lastFrame) / 1_000_000_000f
            lastFrame = now
            state.tick(elapsed)
            val dt = elapsed.coerceAtMost(.05f)
            basketX += (targetX - basketX) * (dt * 18f).coerceAtMost(1f)
            spawn -= dt; messageTime -= dt; mistakeTime -= dt; warningTime -= dt
            if (state.remaining <= 10 && !warned && !state.ended) {
                warned = true; warningTime = 2f; feedbackSound(Feedback.WARNING)
                announceForAccessibility("MASA HAMPIR TAMAT! 10 SAAT LAGI!")
            }
            val second = ceil(state.remaining).toInt()
            if (second in 1..5 && second != lastSecond) feedbackSound(Feedback.COUNTDOWN)
            lastSecond = second
            if (spawn <= 0 && !state.ended) {
                val difficulty = state.difficulty
                drops += Drop(random.nextFloat() * (worldWidth - 48) + 24, 51f,
                    if (random.nextFloat() < .19f) null else FruitKind.entries.random(random), WrongKind.entries.random(random),
                    ((worldHeight - 142).coerceAtLeast(40f) / difficulty.travelSeconds) * (random.nextFloat() * .15f + .95f))
                spawn = difficulty.interval
            }
            val iter = drops.iterator()
            val lip = worldHeight - 75f
            while (iter.hasNext() && !state.ended) {
                val d = iter.next(); val oldY = d.y; d.y += d.speed * dt
                if (oldY <= lip && d.y + 16f >= lip && kotlin.math.abs(d.x - basketX) < 48f) {
                    if (d.fruit == null) {
                        state.catchWrong(); feedbackSound(Feedback.WRONG)
                        message = "BUKAN BUAH! −1 ♥"; mistakeTime = .5f
                    } else {
                        val points = state.catchFruit(d.fruit)
                        floatingPoints += Points(d.x, lip - 24, "+$points")
                        feedbackSound(if (points == 20) Feedback.BONUS else Feedback.FRUIT)
                        message = if (points == 20) "Buah bonus! +20" else "Buah ditangkap! +10"
                    }
                    announceForAccessibility(message); messageTime = 1f; iter.remove()
                } else if (d.y > worldHeight + 30f) iter.remove()
            }
            val pointIter = floatingPoints.iterator()
            while (pointIter.hasNext()) {
                val points = pointIter.next(); points.y -= dt * 36; points.life -= dt
                if (points.life <= 0) pointIter.remove()
            }
            update(state); invalidate()
            if (state.ended) { finished = true; running = false; post(endRound) } else postOnAnimation(this)
        }
    }

    init { contentDescription = "Seret bakul ke kiri dan kanan. Tangkap buah-buahan sahaja. Elak barang bukan buah."; isFocusable = true; update(state) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    fun pause() { running = false; removeCallbacks(loop); lastFrame = 0 }
    fun resume() { if (finished || disposed) return; running = true; lastFrame = 0; removeCallbacks(loop); postOnAnimation(loop) }
    fun stop() { disposed = true; pause(); removeCallbacks(endRound) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) { backgroundScene = null; return }
        val scale = w / worldWidth; worldHeight = h / scale
        backgroundScene = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it); canvas.scale(scale, scale); art.aisle(canvas, worldWidth, worldHeight)
            lighting?.let { roof -> canvas.drawBitmap(roof, null, RectF(0f, 0f, worldWidth, 40f), paint) }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isRunning || width == 0) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { targetX = (event.x / width * worldWidth).coerceIn(40f, worldWidth - 40) }
            MotionEvent.ACTION_UP -> performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0) return
        val scale = width / worldWidth
        canvas.save()
        if (mistakeTime > 0) canvas.translate(sin(mistakeTime * 90f) * 4 * scale, 0f)
        backgroundScene?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
        canvas.scale(scale, scale)
        if (mistakeTime > 0) { paint.color = 0x22E53935; canvas.drawRect(0f, 40f, worldWidth, worldHeight, paint) }
        drops.forEach { d ->
            val image = if (d.fruit == null) wrongSprites.getValue(d.wrong) else fruitSprites.getValue(d.fruit)
            drawImageFit(canvas, image, d.x, d.y, 64f, 72f)
        }
        drawImageFit(canvas, basketSprites.getValue(basket), basketX, worldHeight - 58f, 116f, 100f)
        text(canvas, "${basket.fruit} = +20", worldWidth / 2, worldHeight - 8f, 14f, Color.rgb(36, 92, 58))
        floatingPoints.forEach { points -> text(canvas, points.label, points.x, points.y, 24f, Color.rgb(0, 110, 75)) }
        if (messageTime > 0) {
            paint.color = Color.WHITE; canvas.drawRoundRect(170f, 5f, 550f, 36f, 10f, 10f, paint)
            text(canvas, message, worldWidth / 2, 27f, 17f, if (mistakeTime > 0) Color.rgb(185, 28, 28) else Color.rgb(36, 92, 58))
        }
        if (warningTime > 0 || state.remaining <= 5 && !state.ended) {
            paint.color = 0xEFFFFFFF.toInt()
            canvas.drawRoundRect(145f, 45f, 575f, 122f, 12f, 12f, paint)
            teacherWarning?.let { canvas.drawBitmap(it, null, RectF(150f, 49f, 220f, 119f), paint) }
            canvas.save()
            if (state.remaining <= 5) {
                val pulse = 1f + .08f * sin(SystemClock.uptimeMillis() / 90f)
                canvas.scale(pulse, pulse, 398f, 66f)
            }
            text(canvas, if (state.remaining <= 5) "${ceil(state.remaining).toInt()}" else "10 SAAT LAGI!", 398f, 79f,
                if (state.remaining <= 5) 36f else 25f, Color.rgb(190, 48, 35))
            canvas.restore()
            text(canvas, "MASA HAMPIR TAMAT!", 398f, 104f, 14f, Color.rgb(190, 48, 35))
        }
        canvas.restore()
    }
    /**
     * Centre a transparent PNG in the allotted space without squashing its shape.
     * No placeholder circles, labels or per-frame bitmap decoding.
     */
    private fun drawImageFit(c: Canvas, bitmap: Bitmap, x: Float, y: Float, maxW: Float, maxH: Float) {
        val factor = minOf(maxW / bitmap.width, maxH / bitmap.height)
        val w = bitmap.width * factor
        val h = bitmap.height * factor
        paint.alpha = 255
        paint.color = Color.WHITE
        c.drawBitmap(bitmap, null, RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f), paint)
    }
    private fun text(c: Canvas, text: String, x: Float, y: Float, size: Float, colour: Int) {
        paint.color = colour; paint.textSize = size; paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); c.drawText(text, x, y, paint)
    }
}
