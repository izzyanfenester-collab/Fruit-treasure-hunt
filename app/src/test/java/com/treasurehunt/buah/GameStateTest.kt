package com.treasurehunt.buah

import org.junit.Assert.*
import org.junit.Test

class GameStateTest {
    @Test fun catchesAwardTenOrTwentyAndTrackStatistics() {
        val game = GameState(Basket.RED)
        assertEquals(10, game.catchFruit(FruitKind.GREEN_APPLE))
        assertEquals(20, game.catchFruit(FruitKind.RED_APPLE))
        assertEquals(30, game.score); assertEquals(2, game.fruitsCaught)
        assertEquals(1, game.bonusCaught); assertEquals(3, game.lives)
    }
    @Test fun eachBasketHasOneMatchingBonusFruitAndAllEightFruitsScore() {
        Basket.entries.forEach { basket ->
            val game = GameState(basket)
            FruitKind.entries.forEach { game.catchFruit(it) }
            assertEquals(90, game.score); assertEquals(8, game.fruitsCaught); assertEquals(1, game.bonusCaught)
        }
    }
    @Test fun threeWrongObjectsEndGameImmediatelyAndFurtherCatchesAreIgnored() {
        val game = GameState(Basket.ORANGE)
        repeat(3) { game.catchWrong() }
        game.catchFruit(FruitKind.ORANGE); game.catchWrong()
        assertTrue(game.ended); assertEquals(0, game.lives); assertEquals(0, game.score); assertEquals(3, game.mistakes)
    }
    @Test fun timerEndsAtExactlySixtySeconds() {
        val game = GameState(Basket.PURPLE)
        game.tick(59f); assertFalse(game.ended)
        game.tick(1f); assertTrue(game.ended); assertEquals(0f, game.remaining)
        game.catchFruit(FruitKind.GRAPES); assertEquals(0, game.score)
    }
    @Test fun timerCompletionCannotBeChangedByWrongCatch() {
        val game = GameState(Basket.GREEN)
        game.tick(70f); game.catchWrong()
        assertEquals(3, game.lives); assertEquals(0, game.mistakes); assertEquals(0f, game.remaining)
    }
    @Test fun difficultyIncreasesAtDocumentedThresholds() {
        val game = GameState(Basket.GREEN)
        assertEquals(Difficulty.SLOW, game.difficulty)
        game.tick(15f); assertEquals(Difficulty.NORMAL, game.difficulty)
        game.tick(15f); assertEquals(Difficulty.FAST, game.difficulty)
        game.tick(15f); assertEquals(Difficulty.FASTEST, game.difficulty)
    }
}
