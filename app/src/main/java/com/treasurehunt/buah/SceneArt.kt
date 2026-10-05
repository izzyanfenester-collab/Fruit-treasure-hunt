package com.treasurehunt.buah

import android.graphics.*

/** Story scene accessibility descriptions; scene images come from the supplied storyboard. */
enum class Scene(val description: String) {
    TITLE("Teacher and children arriving at a colourful supermarket"),
    BRIEFING("A teacher explaining the fruit hunt to two children with baskets"),
    INSTRUCTIONS("A teacher explaining how to collect fruit and avoid other objects"),
    BASKETS("Teachers and children with four colourful baskets in the supermarket fruit section"),
    CHECKOUT("Teacher and children celebrating their fruit baskets at checkout"),
    RETRY("The teacher encouraging children to try the fruit hunt again")
}

internal class SceneArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = Color.rgb(37, 82, 63)
    private val fruitGradients = FruitKind.entries.associateWith { fruit ->
        val colour = fruit.colour
        val light = Color.rgb((Color.red(colour) + 255) / 2, (Color.green(colour) + 255) / 2, (Color.blue(colour) + 255) / 2)
        val dark = Color.rgb((Color.red(colour) * .72f).toInt(), (Color.green(colour) * .72f).toInt(), (Color.blue(colour) * .72f).toInt())
        RadialGradient(-7f, -10f, 38f, intArrayOf(light, colour, dark), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    }
    private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, colour: Int, radius: Float = 0f) {
        p.color = colour; p.style = Paint.Style.FILL
        c.drawRoundRect(l, t, r, b, radius, radius, p)
    }
    private fun circle(c: Canvas, x: Float, y: Float, radius: Float, colour: Int) {
        p.color = colour; p.style = Paint.Style.FILL; c.drawCircle(x, y, radius, p)
    }
    private fun label(c: Canvas, text: String, x: Float, y: Float, size: Float, colour: Int = ink) {
        p.color = colour; p.style = Paint.Style.FILL; p.textSize = size
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        c.drawText(text, x, y, p)
    }
    fun aisle(c: Canvas, width: Float, height: Float) {
        box(c, 0f, 0f, width, height, 0xFFFFF1D9.toInt())
        box(c, 0f, 0f, width, 42f, 0xFFFCDA9B.toInt())
        label(c, "BAHAGIAN BUAH-BUAHAN", width / 2, 27f, 16f)
        for (row in 0..1) {
            val y = 68f + row * (height - 155f).coerceAtLeast(35f) / 2f
            box(c, 14f, y, width - 14, y + 32f, 0xFFE2C9A4.toInt(), 7f)
            for (i in 0..15) circle(c, 30f + i * (width - 60) / 15, y + 16, 8f,
                if (row == 0) 0xFFEBC994.toInt() else 0xFFC9D4A2.toInt())
            box(c, 14f, y + 30, width - 14, y + 35f, 0xFFCAAA80.toInt(), 3f)
        }
        box(c, 0f, height - 62f, width, height, 0xFFEFD8B4.toInt())
        for (i in 0..10) box(c, i * width / 10, height - 61, i * width / 10 + 1, height, 0xFFDBC29D.toInt())
    }
    fun fruit(c: Canvas, x: Float, y: Float, basket: Basket, scale: Float = 1f) {
        fruit(c, x, y, FruitKind.entries.first { it.bonusBasket == basket }, scale)
    }
    fun fruit(c: Canvas, x: Float, y: Float, kind: FruitKind, scale: Float = 1f) {
        c.save(); c.translate(x, y); c.scale(scale, scale)
        p.color = kind.colour; p.style = Paint.Style.FILL; p.shader = fruitGradients[kind]
        when (kind) {
            FruitKind.GRAPES -> for (row in 0..2) for (col in 0..(2-row))
                circle(c, -12f + row*6 + col*12, -10f + row*12, 8f, kind.colour)
            FruitKind.BANANA -> {
                p.style = Paint.Style.STROKE; p.strokeWidth = 11f; p.strokeCap = Paint.Cap.ROUND
                c.drawArc(-21f, -27f, 19f, 17f, 12f, 150f, false, p)
                p.style = Paint.Style.FILL; p.strokeCap = Paint.Cap.BUTT
            }
            FruitKind.PEAR -> { c.drawOval(-17f, -2f, 17f, 21f, p); c.drawOval(-10f, -19f, 10f, 8f, p) }
            FruitKind.MANGO -> { c.rotate(-25f); c.drawOval(-14f, -22f, 14f, 20f, p); c.rotate(25f) }
            FruitKind.STRAWBERRY -> {
                val path = Path().apply { moveTo(-18f, -13f); quadTo(-19f, 7f, 0f, 23f); quadTo(19f, 7f, 18f, -13f); close() }
                c.drawPath(path, p)
                p.shader = null
                for (row in 0..2) for (col in 0..1) circle(c, -6f + col*12, -4f + row*7, 1.5f, 0xFFFFD67E.toInt())
            }
            else -> {
                c.drawOval(-18f, -18f, 18f, 18f, p)
                if (kind != FruitKind.ORANGE) { circle(c, -8f, -7f, 12f, kind.colour); circle(c, 8f, -7f, 12f, kind.colour) }
            }
        }
        p.shader = null; p.color = 0xFF2D6E34.toInt(); c.drawOval(0f, -25f, 16f, -17f, p)
        p.color = 0x88FFFFFF.toInt(); c.drawOval(-11f, -11f, -5f, -2f, p)
        c.restore()
    }
    fun wrong(c: Canvas, x: Float, y: Float, kind: WrongKind) {
        c.save(); c.translate(x, y)
        when (kind) {
            WrongKind.CAN -> {
                box(c, -15f, -18f, 15f, 18f, 0xFF617787.toInt(), 4f)
                p.color = Color.LTGRAY; c.drawOval(-15f, -21f, 15f, -13f, p)
                label(c, "TIN", 0f, 6f, 10f, Color.WHITE)
            }
            WrongKind.BOTTLE -> {
                box(c, -7f, -25f, 7f, -10f, 0xFF637D92.toInt(), 3f)
                box(c, -14f, -12f, 14f, 21f, 0xFF7FAFC2.toInt(), 7f)
                box(c, -13f, 0f, 13f, 11f, 0xFFF6E8CE.toInt(), 2f)
            }
            WrongKind.BOX -> {
                box(c, -19f, -18f, 19f, 18f, 0xFFBD8958.toInt(), 3f)
                box(c, -4f, -18f, 4f, 18f, 0xFFE9C693.toInt())
            }
            WrongKind.TOY -> {
                box(c, -20f, -10f, 20f, 13f, 0xFF6D8EE4.toInt(), 6f)
                box(c, -11f, -19f, 11f, -3f, 0xFF6D8EE4.toInt(), 4f)
                circle(c, -12f, 15f, 6f, ink); circle(c, 12f, 15f, 6f, ink)
            }
            WrongKind.SHOE -> {
                box(c, -19f, -1f, 22f, 16f, 0xFF936AA7.toInt(), 8f)
                box(c, -18f, -15f, -1f, 12f, 0xFF936AA7.toInt(), 5f)
                box(c, -18f, 13f, 22f, 18f, Color.WHITE, 4f)
            }
        }
        c.restore()
    }
    fun basket(c: Canvas, x: Float, y: Float, colour: Int, scale: Float = 1f) {
        c.save(); c.translate(x, y); c.scale(scale, scale)
        p.color = colour; p.style = Paint.Style.STROKE; p.strokeWidth = 6f
        c.drawArc(-30f, -27f, 30f, 25f, 180f, 180f, false, p)
        box(c, -43f, 0f, 43f, 52f, colour, 10f)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0xBBFFFFFF.toInt()
        for(i in -2..2) c.drawLine(i * 13f, 9f, i * 11f, 44f, p)
        c.drawLine(-35f, 22f, 35f, 22f, p); c.drawLine(-35f, 36f, 35f, 36f, p)
        p.style = Paint.Style.FILL; c.restore()
    }
}
