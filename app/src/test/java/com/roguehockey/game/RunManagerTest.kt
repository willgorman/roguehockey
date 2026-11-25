package com.roguehockey.game

import com.roguehockey.game.engine.GameScreen
import com.roguehockey.game.engine.RunManager
import com.roguehockey.game.engine.ShopOffer
import com.roguehockey.game.engine.TableGenerator
import com.roguehockey.game.engine.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RunManagerTest {

    @Test
    fun targetScoreScalesMonotonicallyAcrossAntesAndRounds() {
        val r11 = TableGenerator.targetScoreFor(1, 1)
        val r12 = TableGenerator.targetScoreFor(1, 2)
        val r13 = TableGenerator.targetScoreFor(1, 3)
        val r21 = TableGenerator.targetScoreFor(2, 1)
        val r23 = TableGenerator.targetScoreFor(2, 3)

        assertTrue(r11 < r12)
        assertTrue(r12 < r13)
        assertTrue(r13 < r21)
        assertTrue(r21 < r23)
    }

    @Test
    fun clearingRoundPaysOutCoinsAndOpensShop() {
        val rm = RunManager(random = Random(42))
        rm.startNewRun()
        assertEquals(GameScreen.PLAYING, rm.screen)
        val initialCoins = rm.coins

        // Launch a shot and force huge score to clear round immediately
        rm.launchShot(Vec2(0f, -1400f))
        rm.tableState.shotScore.chips = 300
        rm.tableState.shotScore.mult = 4
        rm.recallPucksEarly()

        assertEquals(GameScreen.ROUND_CLEAR, rm.screen)
        val summary = rm.roundClearSummary
        assertNotNull(summary)
        assertTrue("Coins should increase after round clear payout", rm.coins > initialCoins)
        assertEquals(4, summary!!.unusedFlicks)

        rm.openShop()
        assertEquals(GameScreen.SHOP, rm.screen)
        assertEquals(4, rm.shopOffers.size)

        // Buy an affordable offer if available
        val affordable = rm.shopOffers.firstOrNull { it.cost <= rm.coins }
        assertNotNull("Should have at least one affordable shop offer after round 1 payout", affordable)
        val beforeCoins = rm.coins
        assertTrue(rm.buyOffer(affordable!!.id))
        assertEquals(beforeCoins - affordable.cost, rm.coins)
        assertTrue(affordable.purchased)

        // Advance to Round 2
        rm.startNextRound()
        assertEquals(GameScreen.PLAYING, rm.screen)
        assertEquals(1, rm.ante)
        assertEquals(2, rm.roundInAnte)
        assertEquals(0, rm.roundScore)
    }

    @Test
    fun runningOutOfFlicksWithoutReachingTargetTriggersGameOver() {
        val rm = RunManager(random = Random(7))
        rm.startNewRun()

        repeat(rm.modifiers.maxFlicks) {
            rm.launchShot(Vec2(0f, -600f))
            // Zero out shot score to simulate missing everything
            rm.tableState.shotScore.chips = 0
            rm.recallPucksEarly()
            // Advance popup timer so next shot can launch
            repeat(40) { rm.tick(1f / 30f, Vec2.ZERO) }
        }

        assertEquals(GameScreen.GAME_OVER, rm.screen)
    }

    @Test
    fun sellingRelicRefundsCoinsAndFreesSlot() {
        val rm = RunManager(random = Random(99))
        rm.startNewRun()
        rm.launchShot(Vec2(0f, -1400f))
        rm.tableState.shotScore.chips = 500
        rm.tableState.shotScore.mult = 3
        rm.recallPucksEarly()
        rm.openShop()

        val relicOffer = rm.shopOffers.filterIsInstance<ShopOffer.RelicOffer>().first()
        assertTrue(rm.buyOffer(relicOffer.id))
        assertTrue(relicOffer.relic in rm.modifiers.relics)

        val coinsAfterBuy = rm.coins
        assertTrue(rm.sellRelic(relicOffer.relic))
        assertTrue(relicOffer.relic !in rm.modifiers.relics)
        assertTrue(rm.coins > coinsAfterBuy)
    }
}
