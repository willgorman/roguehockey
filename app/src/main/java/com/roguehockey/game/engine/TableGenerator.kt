package com.roguehockey.game.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class RunModifiers(
    val relics: List<RelicId> = emptyList(),
    val maxFlicks: Int = 5,
    val chipBonusUpgrade: Int = 0,
    val multBonusUpgrade: Int = 0,
    val extraPowerUps: Int = 0,
    val launchSpeedMultiplier: Float = 1f
) {
    fun hasRelic(id: RelicId): Boolean = relics.contains(id)
}

object TableGenerator {

    fun targetScoreFor(ante: Int, roundInAnte: Int): Int {
        val baseAnte = 450.0 * 2.55.pow((ante - 1).toDouble())
        val roundFactor = when (roundInAnte) {
            1 -> 1.0
            2 -> 1.45
            else -> 2.0
        }
        val raw = (baseAnte * roundFactor).roundToInt()
        return ((raw + 25) / 50) * 50
    }

    fun bossModifierFor(ante: Int, roundInAnte: Int, random: Random): BossModifier {
        if (roundInAnte < 3) return BossModifier.NONE
        val pool = BossModifier.entries.filter { it != BossModifier.NONE }
        return pool[(ante - 1 + random.nextInt(pool.size)) % pool.size]
    }

    fun generateTable(
        ante: Int,
        roundInAnte: Int,
        modifiers: RunModifiers,
        forcedBoss: BossModifier? = null,
        random: Random = Random.Default
    ): TableState {
        val bossMod = forcedBoss ?: bossModifierFor(ante, roundInAnte, random)
        val targetScore = targetScoreFor(ante, roundInAnte)

        val goalWidth = if (bossMod == BossModifier.SPEED_TRAP) 185f else 255f
        val goalHalf = goalWidth * 0.5f
        val goalGate = GoalGate(
            xMin = TableConstants.TABLE_WIDTH * 0.5f - goalHalf,
            xMax = TableConstants.TABLE_WIDTH * 0.5f + goalHalf,
            chipReward = 130 + ante * 25 + modifiers.chipBonusUpgrade,
            multReward = 2 + modifiers.multBonusUpgrade
        )

        val targets = mutableListOf<Target>()
        val obstacles = mutableListOf<Obstacle>()
        val powerUps = mutableListOf<PowerUpPickup>()
        var nextId = 100

        val chipBase = 35 + (ante - 1) * 8 + modifiers.chipBonusUpgrade
        val multBase = 1 + modifiers.multBonusUpgrade
        val bumperRestitution = if (bossMod == BossModifier.BUMPER_CHAOS) 1.32f else 1.10f

        fun addTarget(
            type: TargetType,
            x: Float,
            y: Float,
            patrolAxis: Vec2 = Vec2.ZERO,
            patrolAmp: Float = 0f,
            patrolSpeed: Float = 0f
        ) {
            val radius = when (type) {
                TargetType.CHIP_BUMPER -> 38f
                TargetType.MULT_BUMPER -> 36f
                TargetType.SPINNER_RING -> 44f
                TargetType.JACKPOT_STAR -> 40f
            }
            val chips = when (type) {
                TargetType.CHIP_BUMPER -> chipBase
                TargetType.MULT_BUMPER -> 15 + modifiers.chipBonusUpgrade / 2
                TargetType.SPINNER_RING -> 25 + modifiers.chipBonusUpgrade / 2
                TargetType.JACKPOT_STAR -> chipBase * 2 + 40
            }
            val mult = when (type) {
                TargetType.CHIP_BUMPER -> 0
                TargetType.MULT_BUMPER -> multBase
                TargetType.SPINNER_RING -> 1
                TargetType.JACKPOT_STAR -> multBase
            }
            val coins = if (type == TargetType.JACKPOT_STAR) 2 else 0
            val speedBoost = if (bossMod == BossModifier.SPEED_TRAP) 1.65f else 1f
            targets.add(
                Target(
                    id = nextId++,
                    type = type,
                    pos = Vec2(x, y),
                    radius = radius,
                    chipValue = chips,
                    multValue = mult,
                    coinValue = coins,
                    restitution = bumperRestitution,
                    patrolCenter = Vec2(x, y),
                    patrolAxis = patrolAxis,
                    patrolAmplitude = patrolAmp,
                    patrolSpeed = patrolSpeed * speedBoost,
                    patrolPhase = random.nextFloat() * (2f * PI.toFloat())
                )
            )
        }

        fun addObstacle(
            type: ObstacleType,
            x: Float,
            y: Float,
            halfW: Float = 56f,
            halfH: Float = 24f,
            hp: Int? = null,
            dropsCoin: Int = 0,
            dropsPowerUp: PowerUpType? = null
        ) {
            val resolvedHp = hp ?: when (type) {
                ObstacleType.ICE_BLOCK -> (1 + (ante / 2)).coerceAtMost(3)
                ObstacleType.STEEL_CRATE -> (2 + (ante / 2)).coerceAtMost(4)
                ObstacleType.CRYO_BARREL -> 1
            }
            val resolvedW = if (type == ObstacleType.CRYO_BARREL) 34f else halfW
            val resolvedH = if (type == ObstacleType.CRYO_BARREL) 34f else halfH
            val bonus = when (type) {
                ObstacleType.ICE_BLOCK -> 45 + ante * 10
                ObstacleType.STEEL_CRATE -> 70 + ante * 15
                ObstacleType.CRYO_BARREL -> 90 + ante * 15
            }
            obstacles.add(
                Obstacle(
                    id = nextId++,
                    type = type,
                    pos = Vec2(x, y),
                    halfW = resolvedW,
                    halfH = resolvedH,
                    hp = resolvedHp,
                    maxHp = resolvedHp,
                    chipBonus = bonus,
                    dropsCoin = dropsCoin,
                    dropsPowerUp = dropsPowerUp
                )
            )
        }

        val archetype = (ante * 3 + roundInAnte + random.nextInt(5)) % 5
        when (archetype) {
            0 -> {
                // Diamond Fortress: Jackpot star guarded by ice diamond, barrels on flanks
                addTarget(TargetType.JACKPOT_STAR, 500f, 340f, Vec2(1f, 0f), 110f, 1.3f)
                addTarget(TargetType.MULT_BUMPER, 240f, 330f)
                addTarget(TargetType.MULT_BUMPER, 760f, 330f)
                addTarget(TargetType.CHIP_BUMPER, 355f, 565f)
                addTarget(TargetType.CHIP_BUMPER, 645f, 565f)
                addTarget(TargetType.SPINNER_RING, 500f, 700f)
                addTarget(TargetType.CHIP_BUMPER, 225f, 835f)
                addTarget(TargetType.CHIP_BUMPER, 775f, 835f)
                addTarget(TargetType.MULT_BUMPER, 500f, 945f)

                addObstacle(ObstacleType.ICE_BLOCK, 500f, 470f, 76f, 22f)
                addObstacle(ObstacleType.ICE_BLOCK, 360f, 415f, 52f, 20f)
                addObstacle(ObstacleType.ICE_BLOCK, 640f, 415f, 52f, 20f)
                addObstacle(ObstacleType.CRYO_BARREL, 210f, 575f)
                addObstacle(ObstacleType.CRYO_BARREL, 790f, 575f)
                addObstacle(
                    ObstacleType.STEEL_CRATE,
                    500f,
                    825f,
                    54f,
                    24f,
                    dropsCoin = 2,
                    dropsPowerUp = randomPowerUp(random)
                )
            }

            1 -> {
                // Pinball Alley: Twin angled lanes with upper bumper cluster
                addTarget(TargetType.MULT_BUMPER, 500f, 265f)
                addTarget(TargetType.CHIP_BUMPER, 320f, 355f)
                addTarget(TargetType.CHIP_BUMPER, 680f, 355f)
                addTarget(TargetType.JACKPOT_STAR, 500f, 495f, Vec2(1f, 0f), 150f, 1.45f)
                addTarget(TargetType.SPINNER_RING, 215f, 650f)
                addTarget(TargetType.SPINNER_RING, 785f, 650f)
                addTarget(TargetType.CHIP_BUMPER, 375f, 775f)
                addTarget(TargetType.CHIP_BUMPER, 625f, 775f)
                addTarget(TargetType.MULT_BUMPER, 500f, 920f)

                addObstacle(ObstacleType.ICE_BLOCK, 270f, 520f, 62f, 22f)
                addObstacle(ObstacleType.ICE_BLOCK, 730f, 520f, 62f, 22f)
                addObstacle(ObstacleType.CRYO_BARREL, 500f, 655f)
                addObstacle(ObstacleType.STEEL_CRATE, 250f, 890f, 48f, 24f, dropsCoin = 1)
                addObstacle(
                    ObstacleType.STEEL_CRATE,
                    750f,
                    890f,
                    48f,
                    24f,
                    dropsPowerUp = randomPowerUp(random)
                )
            }

            2 -> {
                // Twin Vaults: Left & right vaults packed with mult bumpers and barrels
                addTarget(TargetType.JACKPOT_STAR, 250f, 310f)
                addTarget(TargetType.JACKPOT_STAR, 750f, 310f)
                addTarget(TargetType.MULT_BUMPER, 500f, 340f, Vec2(0f, 1f), 65f, 1.5f)
                addTarget(TargetType.CHIP_BUMPER, 240f, 560f)
                addTarget(TargetType.CHIP_BUMPER, 760f, 560f)
                addTarget(TargetType.SPINNER_RING, 500f, 625f)
                addTarget(TargetType.MULT_BUMPER, 350f, 825f)
                addTarget(TargetType.MULT_BUMPER, 650f, 825f)
                addTarget(TargetType.CHIP_BUMPER, 500f, 975f)

                addObstacle(ObstacleType.ICE_BLOCK, 250f, 430f, 74f, 22f)
                addObstacle(ObstacleType.ICE_BLOCK, 750f, 430f, 74f, 22f)
                addObstacle(ObstacleType.CRYO_BARREL, 380f, 460f)
                addObstacle(ObstacleType.CRYO_BARREL, 620f, 460f)
                addObstacle(
                    ObstacleType.STEEL_CRATE,
                    500f,
                    765f,
                    60f,
                    24f,
                    dropsCoin = 2
                )
                addObstacle(ObstacleType.ICE_BLOCK, 210f, 745f, 48f, 22f)
                addObstacle(ObstacleType.ICE_BLOCK, 790f, 745f, 48f, 22f)
            }

            3 -> {
                // Crossfire Rink: Patrol lines + diagonal cryo chain
                addTarget(TargetType.JACKPOT_STAR, 500f, 285f, Vec2(1f, 0f), 200f, 1.55f)
                addTarget(TargetType.MULT_BUMPER, 245f, 445f)
                addTarget(TargetType.MULT_BUMPER, 755f, 445f)
                addTarget(TargetType.CHIP_BUMPER, 500f, 515f)
                addTarget(TargetType.CHIP_BUMPER, 290f, 690f, Vec2(1f, 0f), 70f, 1.2f)
                addTarget(TargetType.CHIP_BUMPER, 710f, 690f, Vec2(-1f, 0f), 70f, 1.2f)
                addTarget(TargetType.SPINNER_RING, 500f, 815f)
                addTarget(TargetType.MULT_BUMPER, 230f, 920f)
                addTarget(TargetType.MULT_BUMPER, 770f, 920f)

                addObstacle(ObstacleType.CRYO_BARREL, 385f, 385f)
                addObstacle(ObstacleType.CRYO_BARREL, 615f, 385f)
                addObstacle(ObstacleType.ICE_BLOCK, 500f, 635f, 78f, 22f)
                addObstacle(
                    ObstacleType.STEEL_CRATE,
                    365f,
                    890f,
                    52f,
                    24f,
                    dropsCoin = 1,
                    dropsPowerUp = randomPowerUp(random)
                )
                addObstacle(ObstacleType.STEEL_CRATE, 635f, 890f, 52f, 24f, dropsCoin = 2)
            }

            else -> {
                // Orbit Arena: Ring of bumpers around a high-value center
                val centerX = 500f
                val centerY = 580f
                addTarget(TargetType.JACKPOT_STAR, centerX, centerY)
                for (i in 0 until 6) {
                    val angle = i * (PI.toFloat() / 3f)
                    val tx = centerX + cos(angle) * 245f
                    val ty = centerY + sin(angle) * 245f
                    val type = if (i % 2 == 0) TargetType.CHIP_BUMPER else TargetType.MULT_BUMPER
                    addTarget(type, tx, ty)
                }
                addTarget(TargetType.SPINNER_RING, 250f, 285f)
                addTarget(TargetType.SPINNER_RING, 750f, 285f)
                addTarget(TargetType.CHIP_BUMPER, 500f, 960f)

                addObstacle(ObstacleType.ICE_BLOCK, 375f, 455f, 46f, 20f)
                addObstacle(ObstacleType.ICE_BLOCK, 625f, 455f, 46f, 20f)
                addObstacle(ObstacleType.ICE_BLOCK, 375f, 705f, 46f, 20f)
                addObstacle(ObstacleType.ICE_BLOCK, 625f, 705f, 46f, 20f)
                addObstacle(ObstacleType.CRYO_BARREL, 500f, 430f)
                addObstacle(
                    ObstacleType.STEEL_CRATE,
                    500f,
                    740f,
                    56f,
                    22f,
                    dropsCoin = 2,
                    dropsPowerUp = randomPowerUp(random)
                )
            }
        }

        // Boss modifier extra table geometry
        if (bossMod == BossModifier.GLACIAL_WALL) {
            val wallY = 1045f
            val xs = listOf(190f, 345f, 500f, 655f, 810f)
            for (wx in xs) {
                addObstacle(ObstacleType.ICE_BLOCK, wx, wallY, 58f, 22f, hp = 2 + (ante / 2))
            }
            // Add a cryo barrel right behind center to reward piercing / curving around
            addObstacle(ObstacleType.CRYO_BARREL, 500f, 965f)
        } else if (bossMod == BossModifier.SPEED_TRAP) {
            addTarget(TargetType.JACKPOT_STAR, 500f, 215f, Vec2(1f, 0f), 260f, 2.2f)
        }

        // Place Power-Up pickups in clean open lanes
        val candidateSpots = listOf(
            Vec2(175f, 1020f),
            Vec2(825f, 1020f),
            Vec2(500f, 1065f),
            Vec2(165f, 420f),
            Vec2(835f, 420f),
            Vec2(500f, 210f)
        ).shuffled(random)

        val powerUpCount = (2 + modifiers.extraPowerUps).coerceAtMost(candidateSpots.size)
        val availableTypes = PowerUpType.entries.shuffled(random)
        var placed = 0
        for (spot in candidateSpots) {
            if (placed >= powerUpCount) break
            val overlapsObstacle = obstacles.any { obs ->
                spot.distanceTo(obs.pos) < (obs.halfW + 48f)
            }
            val overlapsTarget = targets.any { tgt ->
                spot.distanceTo(tgt.pos) < (tgt.radius + 46f)
            }
            if (!overlapsObstacle && !overlapsTarget) {
                powerUps.add(
                    PowerUpPickup(
                        id = nextId++,
                        type = availableTypes[placed % availableTypes.size],
                        pos = spot,
                        pulsePhase = random.nextFloat() * 6.28f
                    )
                )
                placed++
            }
        }
        // Guarantee at least 1 power-up even on crowded Boss layouts
        if (powerUps.isEmpty()) {
            powerUps.add(
                PowerUpPickup(
                    id = nextId++,
                    type = availableTypes.first(),
                    pos = Vec2(165f, 1110f)
                )
            )
        }

        val puckMass = if (modifiers.hasRelic(RelicId.HEAVY_METAL_PUCK)) 1.35f else 1f
        val mainPuck = Puck(
            id = 1,
            pos = Vec2(TableConstants.DEFAULT_PUCK_X, TableConstants.DEFAULT_PUCK_Y),
            vel = Vec2.ZERO,
            mass = puckMass,
            isMainPuck = true
        )

        return TableState(
            ante = ante,
            roundInAnte = roundInAnte,
            bossModifier = bossMod,
            targetScore = targetScore,
            goalGate = goalGate,
            pucks = mutableListOf(mainPuck),
            targets = targets,
            obstacles = obstacles,
            powerUps = powerUps,
            nextEntityId = nextId + 10
        )
    }

    private fun randomPowerUp(random: Random): PowerUpType {
        val all = PowerUpType.entries
        return all[random.nextInt(all.size)]
    }
}
