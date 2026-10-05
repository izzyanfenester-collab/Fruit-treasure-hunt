package com.treasurehunt.buah

enum class Basket(val title: String, val colour: Int, val fruit: String) {
    GREEN("Green", 0xFF43A047.toInt(), "Guava"),
    RED("Red", 0xFFE53935.toInt(), "Apple"),
    PURPLE("Purple", 0xFF8E24AA.toInt(), "Grapes"),
    ORANGE("Orange", 0xFFFB8C00.toInt(), "Orange")
}

class GameState(val basket: Basket) {
    var score = 0; private set
    var lives = 3; private set
    var remaining = 60f; private set
    val ended get() = lives == 0 || remaining <= 0f
    fun tick(seconds: Float) { if (!ended) remaining = (remaining - seconds.coerceAtLeast(0f)).coerceAtLeast(0f) }
    fun catchItem(fruit: Basket?) {
        if (ended) return
        if (fruit == null) lives-- else score += if (fruit == basket) 25 else 10
    }
}
