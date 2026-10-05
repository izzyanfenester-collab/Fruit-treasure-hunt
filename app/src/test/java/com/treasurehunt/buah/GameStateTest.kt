package com.treasurehunt.buah
import org.junit.Assert.*
import org.junit.Test
class GameStateTest {
    @Test fun catchesAwardPointsAndMatchingBonus() {
        val game = GameState(Basket.RED)
        game.catchItem(Basket.GREEN); game.catchItem(Basket.RED)
        assertEquals(35, game.score); assertEquals(3, game.lives)
    }
    @Test fun threeObjectsEndGameAndFurtherCatchesAreIgnored() {
        val game = GameState(Basket.ORANGE)
        repeat(3) { game.catchItem(null) }; game.catchItem(Basket.ORANGE); game.catchItem(null)
        assertTrue(game.ended); assertEquals(0, game.lives); assertEquals(0, game.score)
    }
    @Test fun timerEndsAtSixtySeconds() {
        val game = GameState(Basket.PURPLE)
        game.tick(59f); assertFalse(game.ended)
        game.tick(2f); assertTrue(game.ended); assertEquals(0f, game.remaining)
        game.catchItem(Basket.PURPLE); assertEquals(0, game.score)
    }
}
