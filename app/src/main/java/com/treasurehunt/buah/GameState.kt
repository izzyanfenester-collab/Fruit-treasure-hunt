package com.treasurehunt.buah

enum class Basket(val title: String, val colour: Int, val fruit: String) {
    GREEN("HIJAU", 0xFF43A047.toInt(), "Epal hijau"),
    RED("MERAH", 0xFFD93636.toInt(), "Epal merah"),
    PURPLE("UNGU", 0xFF8525A6.toInt(), "Anggur"),
    ORANGE("OREN", 0xFFE57500.toInt(), "Oren")
}

enum class FruitKind(val title: String, val colour: Int, val bonusBasket: Basket? = null) {
    GREEN_APPLE("Epal hijau", Basket.GREEN.colour, Basket.GREEN),
    RED_APPLE("Epal merah", Basket.RED.colour, Basket.RED),
    ORANGE("Oren", Basket.ORANGE.colour, Basket.ORANGE),
    GRAPES("Anggur", Basket.PURPLE.colour, Basket.PURPLE),
    MANGO("Mangga", 0xFFFBC02D.toInt()),
    PEAR("Pir", 0xFF99BD40.toInt()),
    STRAWBERRY("Strawberi", 0xFFF34B65.toInt()),
    BANANA("Pisang", 0xFFFFD54F.toInt())
}

enum class WrongKind { BOTTLE, TOY, CAN, BOX, SHOE }
enum class Feedback { FRUIT, BONUS, WRONG, COUNTDOWN, WARNING, SUCCESS, GAME_OVER }
enum class Difficulty(val interval: Float, val travelSeconds: Float) {
    SLOW(.85f, 5f), NORMAL(.7f, 4.2f), FAST(.55f, 3.5f), FASTEST(.42f, 2.8f)
}

class GameState(val basket: Basket) {
    var score = 0; private set
    var lives = 3; private set
    var remaining = 60f; private set
    var fruitsCaught = 0; private set
    var bonusCaught = 0; private set
    var mistakes = 0; private set
    val ended get() = lives == 0 || remaining <= 0f
    val difficulty get() = when {
        remaining > 45f -> Difficulty.SLOW
        remaining > 30f -> Difficulty.NORMAL
        remaining > 15f -> Difficulty.FAST
        else -> Difficulty.FASTEST
    }
    fun tick(seconds: Float) {
        if (!ended && seconds.isFinite()) remaining = (remaining - seconds.coerceAtLeast(0f)).coerceAtLeast(0f)
    }
    fun catchFruit(fruit: FruitKind): Int {
        if (ended) return 0
        val bonus = fruit.bonusBasket == basket
        val points = if (bonus) 20 else 10
        score += points; fruitsCaught++
        if (bonus) bonusCaught++
        return points
    }
    fun catchWrong() {
        if (ended) return
        lives--; mistakes++
    }
}
