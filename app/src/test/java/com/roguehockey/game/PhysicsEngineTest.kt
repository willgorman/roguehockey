package com.roguehockey.game

import com.roguehockey.game.engine.BossModifier
import com.roguehockey.game.engine.GoalGate
import com.roguehockey.game.engine.Obstacle
import com.roguehockey.game.engine.ObstacleType
import com.roguehockey.game.engine.PhysicsEngine
import com.roguehockey.game.engine.PowerUpPickup
import com.roguehockey.game.engine.PowerUpType
import com.roguehockey.game.engine.Puck
import com.roguehockey.game.engine.RelicId
import com.roguehockey.game.engine.RunModifiers
import com.roguehockey.game.engine.ShotPhase
import com.roguehockey.game.engine.SoundEvent
import com.roguehockey.game.engine.TableConstants
import com.roguehockey.game.engine.TableState
import com.roguehockey.game.engine.Target
import com.roguehockey.game.engine.TargetType
import com.roguehockey.game.engine.TiltSensitivity
import com.roguehockey.game.engine.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhysicsEngineTest {

    private fun emptyTable(): TableState = TableState(
        ante = 1,
        roundInAnte = 1,
        bossModifier = BossModifier.NONE,
        targetScore = 500,
        goalGate = GoalGate(xMin = 375f, xMax = 625f),
        pucks = mutableListOf(Puck(id = 1, pos = Vec2(500f, 1200f))),
        targets = mutableListOf(),
        obstacles = mutableListOf(),
        powerUps = mutableListOf()
    )

    @Test
    fun bankShotProRelicAddsMultOnWallBounce() {
        val physics = PhysicsEngine(Random(1))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(120f, 800f)
        val mods = RunModifiers(relics = listOf(RelicId.BANK_SHOT_PRO))

        physics.launchPuck(table, Vec2(-1200f, 0f), mods)
        var wallBounced = false
        repeat(40) {
            val res = physics.step(table, 1f / 60f, Vec2.ZERO, TiltSensitivity.OFF, mods)
            if (SoundEvent.WALL_BOUNCE in res.soundEvents) wallBounced = true
        }

        assertTrue("Expected wall bounce event", wallBounced)
        assertTrue("Expected Bank Shot Pro to increase mult", table.shotScore.mult >= 3)
    }

    @Test
    fun cryoBarrelDetonatesAndShattersAdjacentIceBlock() {
        val physics = PhysicsEngine(Random(2))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(500f, 900f)
        val barrel = Obstacle(
            id = 10,
            type = ObstacleType.CRYO_BARREL,
            pos = Vec2(500f, 700f),
            halfW = 34f,
            halfH = 34f,
            hp = 1,
            maxHp = 1,
            chipBonus = 90
        )
        val iceBlock = Obstacle(
            id = 11,
            type = ObstacleType.ICE_BLOCK,
            pos = Vec2(620f, 700f),
            halfW = 50f,
            halfH = 22f,
            hp = 2,
            maxHp = 2,
            chipBonus = 50
        )
        table.obstacles.addAll(listOf(barrel, iceBlock))

        val mods = RunModifiers(relics = listOf(RelicId.CHAIN_REACTION))
        physics.launchPuck(table, Vec2(0f, -1400f), mods)
        var exploded = false
        repeat(40) {
            val res = physics.step(table, 1f / 60f, Vec2.ZERO, TiltSensitivity.OFF, mods)
            if (SoundEvent.BARREL_EXPLODE in res.soundEvents) exploded = true
        }

        assertTrue("Expected cryo-barrel to explode", exploded)
        assertTrue("Barrel should be marked destroyed", barrel.destroyed)
        assertTrue("Adjacent ice block within blast radius should shatter", iceBlock.destroyed)
        assertTrue("Shot chips should include barrel + chain reaction + ice bonus", table.shotScore.chips >= 240)
    }

    @Test
    fun multiPuckPowerUpSpawnsSecondPuck() {
        val physics = PhysicsEngine(Random(3))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(500f, 1000f)
        table.powerUps.add(PowerUpPickup(id = 20, type = PowerUpType.MULTI_PUCK, pos = Vec2(500f, 820f)))

        physics.launchPuck(table, Vec2(0f, -1200f), RunModifiers())
        repeat(30) {
            physics.step(table, 1f / 60f, Vec2.ZERO, TiltSensitivity.OFF, RunModifiers())
        }

        assertEquals("Multi-puck pickup should create a 2nd active puck", 2, table.pucks.size)
        assertTrue("Pickup should be marked collected", table.powerUps[0].collected)
    }

    @Test
    fun tiltCurvesPuckTrajectoryLaterally() {
        val physics = PhysicsEngine(Random(4))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(500f, 1200f)

        physics.launchPuck(table, Vec2(0f, -900f), RunModifiers())
        repeat(45) {
            physics.step(table, 1f / 60f, Vec2(1f, 0f), TiltSensitivity.HIGH, RunModifiers())
        }

        assertTrue(
            "Positive X tilt should curve puck clearly to the right (x=${table.pucks[0].pos.x})",
            table.pucks[0].pos.x > 620f
        )
    }

    @Test
    fun topGoalGateScoresGoalAndTopShelfDoublesMult() {
        val physics = PhysicsEngine(Random(5))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(500f, 220f)
        val mods = RunModifiers(relics = listOf(RelicId.GOAL_POACHER))

        physics.launchPuck(table, Vec2(0f, -1400f), mods)
        var scoredGoal = false
        repeat(30) {
            val res = physics.step(table, 1f / 60f, Vec2.ZERO, TiltSensitivity.OFF, mods)
            if (SoundEvent.GOAL_SCORE in res.soundEvents) scoredGoal = true
        }

        assertTrue("Expected goal gate score event", scoredGoal)
        assertEquals(1, table.shotScore.goalsScored)
        // Base mult 1 + goal mult 2 = 3, doubled by Top Shelf relic = 6
        assertEquals(6, table.shotScore.mult)
    }

    @Test
    fun targetHitsAccumulateChipsAndMultAndBankOnRecall() {
        val physics = PhysicsEngine(Random(6))
        val table = emptyTable()
        table.pucks[0].pos = Vec2(500f, 950f)
        table.targets.add(
            Target(id = 30, type = TargetType.MULT_BUMPER, pos = Vec2(500f, 750f), radius = 38f, chipValue = 20, multValue = 2)
        )

        physics.launchPuck(table, Vec2(0f, -1100f), RunModifiers())
        repeat(25) {
            physics.step(table, 1f / 60f, Vec2.ZERO, TiltSensitivity.OFF, RunModifiers())
        }
        val banked = physics.recallPucksEarly(table)

        assertEquals(ShotPhase.SCORING_POPUP, table.shotPhase)
        assertTrue("Expected banked score >= 60", (banked.shotBankedScore ?: 0) >= 60)
    }

    @Test
    fun aimTrajectoryReflectsOffSideWall() {
        val physics = PhysicsEngine()
        val pts = physics.computeAimTrajectory(
            start = Vec2(160f, 1200f),
            direction = Vec2(-1f, -1f),
            puckRadius = TableConstants.DEFAULT_PUCK_RADIUS,
            maxLength = 600f
        )
        assertEquals("Should contain start, wall hit, and bounce endpoint", 3, pts.size)
        assertTrue("Bounce should reflect rightward", pts[2].x > pts[1].x)
    }
}
