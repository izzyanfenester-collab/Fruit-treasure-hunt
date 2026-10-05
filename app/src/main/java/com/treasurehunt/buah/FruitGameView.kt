package com.treasurehunt.buah

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

/** Frame time is capped to avoid teleporting objects after a slow frame. Pauses reset the clock. */
class FruitGameView(context: Context, private val basket: Basket,
    private val update: (GameState) -> Unit, private val sound: (Boolean) -> Unit,
    private val finish: (GameState) -> Unit) : View(context) {
    private val state = GameState(basket)
    private data class Drop(val x: Float,var y: Float,val fruit: Basket?,val speed: Float)
    private val drops = mutableListOf<Drop>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var basketX = 180f
    private var lastFrame = 0L
    private var spawn = 0f
    private var running = true
    private var finished = false
    private var feedback = "Drag your basket ↔"
    private var feedbackTime = 2f
    private var worldHeight = 640f
    private val loop = object : Runnable {
        override fun run() {
            if(!running || finished) return
            val now = System.nanoTime()
            val elapsed = if(lastFrame == 0L) 0f else (now-lastFrame)/1_000_000_000f
            lastFrame = now
            state.tick(elapsed)
            val dt = elapsed.coerceAtMost(.05f)
            spawn -= dt; feedbackTime -= dt
            if(spawn <= 0 && !state.ended) {
                drops += Drop(Random.nextFloat()*312f+24f,-24f,if(Random.nextFloat()<.19f) null else Basket.entries.random(),Random.nextFloat()*65f+115f+(60-state.remaining)*.8f)
                spawn = .55f
            }
            val iter = drops.iterator()
            while(iter.hasNext()) {
                val d = iter.next(); val oldY = d.y; d.y += d.speed*dt
                val lip = worldHeight-85f
                if(oldY <= lip && d.y+16f >= lip && kotlin.math.abs(d.x-basketX)<49f) {
                    state.catchItem(d.fruit); sound(d.fruit != null)
                    feedback = if(d.fruit == null) "Oops! Avoid the tins" else if(d.fruit == basket) "Matching fruit! +25 ★" else "Yummy! +10"
                    feedbackTime = 1.1f; iter.remove()
                } else if(d.y > worldHeight+30f) iter.remove()
            }
            update(state); invalidate()
            if(state.ended) { finished = true; running = false; post { finish(state) } } else postOnAnimation(this)
        }
    }
    init { contentDescription = "Fruit catching area. Drag left or right to move the basket."; isFocusable = true; update(state) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    fun pause() { running = false; removeCallbacks(loop); lastFrame = 0 }
    fun resume() { if(finished) return; running = true; lastFrame = 0; removeCallbacks(loop); postOnAnimation(loop) }
    fun stop() { pause() }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(!running) return true
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE -> { basketX = (event.x/width*360f).coerceIn(45f,315f); invalidate() }
            MotionEvent.ACTION_UP -> performClick()
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(width == 0) return
        val scale = width/360f; worldHeight = height/scale
        canvas.save(); canvas.scale(scale,scale)
        canvas.drawColor(Color.rgb(233,250,239))
        paint.color = Color.rgb(255,220,124); canvas.drawRect(0f,0f,360f,48f,paint)
        text(canvas,"PASAR CERIA • FRUIT AISLE",180f,30f,16f,Color.rgb(75,82,40))
        // Supermarket shelves and colourful produce crates.
        for(row in 0..2) {
            val y = 85f+row*110f
            paint.color = Color.rgb(206,233,208); canvas.drawRoundRect(8f,y,352f,y+58,8f,8f,paint)
            for(col in 0..7) { paint.color = intArrayOf(0xFFFFCC80.toInt(),0xFFB9D9B5.toInt(),0xFFE8C7D5.toInt())[row]; canvas.drawCircle(25f+col*44,y+28,13f,paint) }
            paint.color = Color.rgb(180,211,182); canvas.drawRect(8f,y+55,352f,y+60,paint)
        }
        paint.color = Color.rgb(248,226,176); canvas.drawRect(0f,worldHeight-30f,360f,worldHeight,paint)
        drops.forEach { drawDrop(canvas,it) }
        val y = worldHeight-85f
        paint.color = basket.colour; canvas.drawRoundRect(basketX-43,y,basketX+43,y+52,10f,10f,paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 6f
        canvas.drawArc(basketX-30,y-27,basketX+30,y+25,180f,180f,false,paint)
        paint.color = Color.WHITE; paint.strokeWidth = 2f
        for(i in -2..2) canvas.drawLine(basketX+i*13,y+9,basketX+i*11,y+44,paint)
        canvas.drawLine(basketX-35,y+22,basketX+35,y+22,paint); canvas.drawLine(basketX-35,y+36,basketX+35,y+36,paint)
        paint.style = Paint.Style.FILL
        text(canvas,"${basket.fruit} bonus +25",180f,worldHeight-8f,14f,Color.rgb(36,92,58))
        if(feedbackTime > 0) text(canvas,feedback,180f,65f,17f,Color.rgb(36,92,58))
        canvas.restore()
    }
    private fun drawDrop(c: Canvas,d: Drop) {
        if(d.fruit == null) {
            paint.color = Color.rgb(112,128,144); c.drawRoundRect(d.x-15,d.y-18,d.x+15,d.y+18,4f,4f,paint)
            paint.color = Color.LTGRAY; c.drawOval(d.x-15,d.y-21,d.x+15,d.y-13,paint)
            text(c,"TIN",d.x,d.y+6,10f,Color.WHITE); return
        }
        paint.color = d.fruit.colour
        if(d.fruit == Basket.PURPLE) {
            for(row in 0..2) for(col in 0..(2-row)) c.drawCircle(d.x-12+row*6+col*12,d.y-10+row*12,8f,paint)
        } else c.drawOval(d.x-18,d.y-18,d.x+18,d.y+18,paint)
        paint.color = Color.rgb(45,110,52); c.drawOval(d.x,d.y-25,d.x+16,d.y-17,paint)
        paint.color = 0x77FFFFFF; c.drawOval(d.x-11,d.y-11,d.x-5,d.y-2,paint)
    }
    private fun text(c: Canvas,s: String,x: Float,y: Float,size: Float,colour: Int) {
        paint.color = colour; paint.textSize = size; paint.textAlign = Paint.Align.CENTER; paint.typeface = Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD); c.drawText(s,x,y,paint)
    }
}
