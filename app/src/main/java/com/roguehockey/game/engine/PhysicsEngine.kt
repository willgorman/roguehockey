package com.roguehockey.game.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class StepResult(
    val soundEvents: List<SoundEvent>,
    val shotBankedScore: Int? = null,
    val shotBankedCoins: Int = 0
)

class PhysicsEngine(private val random: Random = Random.Default) {

    companion object {
        private const val SUBSTEP_DT = 1f / 120f
        private const val MAX_FRAME_DT = 0.05f
        private const val MAX_PUCK_SPEED = 2450f
        private const val SETTLE_TIME_SECONDS = 0.35f
        private const val MAX_SHOT_SECONDS = 11.5f
        private const val SCORING_POPUP_DURATION = 1.05f
    }

    fun launchPuck(
        state: TableState,
        velocity: Vec2,
        modifiers: RunModifiers
    ): List<SoundEvent> {
        if (state.shotPhase != ShotPhase.AIMING) return emptyList()
        val mainPuck = state.pucks.firstOrNull() ?: return emptyList()

        val maxSpeed = TableConstants.MAX_LAUNCH_SPEED * modifiers.launchSpeedMultiplier
        val speed = velocity.length().coerceIn(TableConstants.MIN_LAUNCH_SPEED, maxSpeed)
        val dir = velocity.normalized()
        if (dir.lengthSquared() < 0.01f) return emptyList()

        mainPuck.vel = dir * speed
        mainPuck.slowTimer = 0f
        mainPuck.trail.clear()
        state.shotScore.reset()
        state.shotPhase = ShotPhase.IN_PLAY

        spawnParticles(state, mainPuck.pos, count = 12, colorHex = 0xFF38BDF8, speedRange = 120f..360f)

        // Relic: Split Personality (Twin Launch) on first flick of each round
        if (!state.firstFlickUsed && modifiers.hasRelic(RelicId.SPLIT_PERSONALITY)) {
            spawnTwinPuck(state, mainPuck, angleOffsetRad = 0.24f)
            addFloatingText(state, mainPuck.pos + Vec2(0f, -55f), "TWIN LAUNCH!", 0xFF8B5CF6, scale = 1.1f)
        }
        state.firstFlickUsed = true

        return listOf(SoundEvent.FLICK_LAUNCH)
    }

    fun recallPucksEarly(state: TableState): StepResult {
        if (state.shotPhase != ShotPhase.IN_PLAY) return StepResult(emptyList())
        return finishShot(state)
    }

    fun step(
        state: TableState,
        frameDt: Float,
        tiltInput: Vec2,
        tiltSensitivity: TiltSensitivity,
        modifiers: RunModifiers
    ): StepResult {
        val dt = frameDt.coerceIn(0f, MAX_FRAME_DT)
        state.tableTime += dt

        val events = mutableListOf<SoundEvent>()

        // Update screen shake & cosmetic animations every frame
        state.screenShake = max(0f, state.screenShake - dt * 28f)
        updateEffects(state, dt)
        updateMovingTargets(state, dt)
        updatePowerUpPulses(state, dt)

        // Crosswind Boss Modifier oscillation
        state.currentWindX = if (state.bossModifier == BossModifier.MAGNETIC_DRIFT) {
            sin(state.tableTime * 1.45f) * 520f
        } else {
            0f
        }

        if (state.shotPhase == ShotPhase.SCORING_POPUP) {
            state.scoringPopupTimer -= dt
            if (state.scoringPopupTimer <= 0f) {
                resetPuckForNextFlick(state, modifiers)
                state.shotPhase = ShotPhase.AIMING
            }
            return StepResult(events)
        }

        if (state.shotPhase != ShotPhase.IN_PLAY) {
            return StepResult(events)
        }

        // Fixed substep integration for rock-solid high-speed air hockey collisions
        var remaining = dt
        while (remaining > 1e-5f) {
            val subDt = min(remaining, SUBSTEP_DT)
            stepPhysicsSubstep(state, subDt, tiltInput, tiltSensitivity, modifiers, events)
            remaining -= subDt
        }

        state.shotScore.shotElapsed += dt
        state.shotScore.lastHitElapsed += dt

        // Check whether all pucks have settled
        var allSlow = true
        for (puck in state.pucks) {
            val speed = puck.vel.length()
            if (speed < TableConstants.STOP_SPEED_THRESHOLD) {
                puck.slowTimer += dt
            } else {
                puck.slowTimer = 0f
                allSlow = false
            }
        }

        val timedOut = state.shotScore.shotElapsed >= MAX_SHOT_SECONDS
        val settled = allSlow && state.pucks.all { it.slowTimer >= SETTLE_TIME_SECONDS }
        if (settled || timedOut) {
            val finishResult = finishShot(state)
            return StepResult(
                soundEvents = events + finishResult.soundEvents,
                shotBankedScore = finishResult.shotBankedScore,
                shotBankedCoins = finishResult.shotBankedCoins
            )
        }

        return StepResult(events)
    }

    private fun finishShot(state: TableState): StepResult {
        val total = state.shotScore.previewTotal
        val coins = state.shotScore.coinsEarnedThisShot
        state.lastBankedChips = state.shotScore.chips
        state.lastBankedMult = state.shotScore.mult
        state.lastBankedScore = total

        // Freeze pucks cleanly
        for (p in state.pucks) {
            p.vel = Vec2.ZERO
        }

        state.shotPhase = ShotPhase.SCORING_POPUP
        state.scoringPopupTimer = if (total > 0) SCORING_POPUP_DURATION else 0.42f

        val sound = if (total > 0) listOf(SoundEvent.SHOT_BANKED) else emptyList()
        return StepResult(
            soundEvents = sound,
            shotBankedScore = total,
            shotBankedCoins = coins
        )
    }

    fun resetPuckForNextFlick(state: TableState, modifiers: RunModifiers) {
        val mass = if (modifiers.hasRelic(RelicId.HEAVY_METAL_PUCK)) 1.35f else 1f
        val main = Puck(
            id = 1,
            pos = Vec2(TableConstants.DEFAULT_PUCK_X, TableConstants.DEFAULT_PUCK_Y),
            vel = Vec2.ZERO,
            mass = mass,
            isMainPuck = true
        )
        state.pucks.clear()
        state.pucks.add(main)
        state.shotScore.reset()
    }

    private fun stepPhysicsSubstep(
        state: TableState,
        dt: Float,
        tiltInput: Vec2,
        tiltSensitivity: TiltSensitivity,
        modifiers: RunModifiers,
        events: MutableList<SoundEvent>
    ) {
        val baseFriction = when {
            state.bossModifier == BossModifier.HEAVY_ICE -> 0.88f
            modifiers.hasRelic(RelicId.AIR_CUSHION) -> 0.34f
            else -> 0.52f
        }
        val tiltPower = tiltSensitivity.accelMultiplier *
            (if (modifiers.hasRelic(RelicId.TILT_MASTER)) 1.60f else 1f)
        val tiltAccel = Vec2(tiltInput.x * tiltPower, tiltInput.y * tiltPower)
        val isTiltingActively = tiltSensitivity != TiltSensitivity.OFF && tiltInput.length() > 0.18f

        // Update cooldowns on targets
        for (t in state.targets) {
            if (t.hitCooldown > 0f) t.hitCooldown = max(0f, t.hitCooldown - dt)
            if (t.hitFlash > 0f) t.hitFlash = max(0f, t.hitFlash - dt * 3.5f)
            if (t.type == TargetType.SPINNER_RING) {
                t.spinAngle += dt * (2.2f + t.hitFlash * 10f)
            }
        }
        for (o in state.obstacles) {
            if (o.hitFlash > 0f) o.hitFlash = max(0f, o.hitFlash - dt * 4f)
        }
        if (state.goalGate.flashTimer > 0f) {
            state.goalGate.flashTimer = max(0f, state.goalGate.flashTimer - dt * 2.2f)
        }

        // Integrate pucks
        val newlySpawnedPucks = mutableListOf<Puck>()
        for (puck in state.pucks) {
            if (puck.overdriveTimer > 0f) puck.overdriveTimer = max(0f, puck.overdriveTimer - dt)
            if (puck.giantTimer > 0f) puck.giantTimer = max(0f, puck.giantTimer - dt)
            if (puck.magnetTimer > 0f) puck.magnetTimer = max(0f, puck.magnetTimer - dt)
            if (puck.goalCooldown > 0f) puck.goalCooldown = max(0f, puck.goalCooldown - dt)

            // Apply tilt acceleration (scaled gently when puck is moving so tablet tilt feels tactile and controllable)
            val currentSpeed = puck.vel.length()
            val tiltFactor = if (currentSpeed > 30f) 1f else 0.35f
            puck.vel += tiltAccel * (tiltFactor * dt)

            // Apply Boss Crosswind in the middle band of the rink
            if (state.currentWindX != 0f && puck.pos.y in 360f..1040f) {
                puck.vel += Vec2(state.currentWindX * dt, 0f)
            }

            // Apply Target Magnet curve if active
            if (puck.isMagnet && currentSpeed > 60f) {
                val nearest = state.targets.minByOrNull { it.pos.distanceTo(puck.pos) }
                if (nearest != null) {
                    val toTarget = nearest.pos - puck.pos
                    val dist = toTarget.length()
                    if (dist in 40f..360f) {
                        puck.vel += toTarget.normalized() * (980f * dt)
                    }
                }
            }

            // Ice air-cushion exponential damping
            val damp = (1f - baseFriction * dt).coerceIn(0.85f, 1f)
            puck.vel = (puck.vel * damp).clampLength(MAX_PUCK_SPEED)
            puck.pos += puck.vel * dt

            // Record sleek trail every few substeps
            if (puck.trail.isEmpty() || puck.trail.last().distanceTo(puck.pos) > 14f) {
                puck.trail.add(puck.pos)
                if (puck.trail.size > 16) puck.trail.removeAt(0)
            }

            // 1. Walls + Top Goal Gate
            resolveWallsAndGoal(state, puck, modifiers, events)

            // 2. Power-Ups
            resolvePowerUps(state, puck, newlySpawnedPucks, events)

            // 3. Targets
            resolveTargets(state, puck, modifiers, isTiltingActively, events)

            // 4. Destructible Obstacles
            resolveObstacles(state, puck, modifiers, events)
        }

        if (newlySpawnedPucks.isNotEmpty()) {
            state.pucks.addAll(newlySpawnedPucks)
        }

        // 5. Puck-to-Puck collisions
        for (i in 0 until state.pucks.size) {
            for (j in i + 1 until state.pucks.size) {
                resolvePuckToPuck(state.pucks[i], state.pucks[j])
            }
        }
    }

    private fun resolveWallsAndGoal(
        state: TableState,
        puck: Puck,
        modifiers: RunModifiers,
        events: MutableList<SoundEvent>
    ) {
        val r = puck.radius
        val minX = TableConstants.WALL_MARGIN + r
        val maxX = TableConstants.TABLE_WIDTH - TableConstants.WALL_MARGIN - r
        val minY = TableConstants.WALL_MARGIN + r
        val maxY = TableConstants.TABLE_HEIGHT - TableConstants.WALL_MARGIN - r
        val wallRestitution = 0.90f

        var bouncedWall = false

        if (puck.pos.x < minX) {
            puck.pos = Vec2(minX, puck.pos.y)
            if (puck.vel.x < 0f) {
                puck.vel = Vec2(-puck.vel.x * wallRestitution, puck.vel.y)
                bouncedWall = true
            }
        } else if (puck.pos.x > maxX) {
            puck.pos = Vec2(maxX, puck.pos.y)
            if (puck.vel.x > 0f) {
                puck.vel = Vec2(-puck.vel.x * wallRestitution, puck.vel.y)
                bouncedWall = true
            }
        }

        if (puck.pos.y < minY) {
            val inGoalMouth = puck.pos.x in (state.goalGate.xMin + r * 0.35f)..(state.goalGate.xMax - r * 0.35f)
            puck.pos = Vec2(puck.pos.x, minY)
            if (puck.vel.y < 0f) {
                if (inGoalMouth && puck.goalCooldown <= 0f) {
                    // GOAL SCORED!
                    puck.goalCooldown = 0.75f
                    puck.vel = Vec2(puck.vel.x * 0.85f, abs(puck.vel.y) * 0.72f + 220f)
                    state.goalGate.flashTimer = 1f
                    state.screenShake = max(state.screenShake, 12f)
                    state.shotScore.goalsScored++
                    state.shotScore.chips += state.goalGate.chipReward
                    state.shotScore.mult += state.goalGate.multReward
                    if (modifiers.hasRelic(RelicId.GOAL_POACHER)) {
                        state.shotScore.mult *= 2
                        addFloatingText(
                            state,
                            Vec2(puck.pos.x, minY + 58f),
                            "TOP SHELF! x2 MULT",
                            0xFF7C3AED,
                            scale = 1.25f
                        )
                    } else {
                        addFloatingText(
                            state,
                            Vec2(puck.pos.x, minY + 52f),
                            "GOAL! +${state.goalGate.chipReward}",
                            0xFF10B981,
                            scale = 1.2f
                        )
                    }
                    spawnShockwave(state, Vec2(puck.pos.x, TableConstants.WALL_MARGIN + 8f), 190f, 0xFF10B981)
                    spawnParticles(state, Vec2(puck.pos.x, minY), 22, 0xFF10B981, 180f..560f)
                    events.add(SoundEvent.GOAL_SCORE)
                } else {
                    puck.vel = Vec2(puck.vel.x, -puck.vel.y * wallRestitution)
                    bouncedWall = true
                }
            }
        } else if (puck.pos.y > maxY) {
            puck.pos = Vec2(puck.pos.x, maxY)
            if (puck.vel.y > 0f) {
                puck.vel = Vec2(puck.vel.x, -puck.vel.y * wallRestitution)
                bouncedWall = true
            }
        }

        if (bouncedWall) {
            state.shotScore.wallBounces++
            events.add(SoundEvent.WALL_BOUNCE)
            spawnParticles(state, puck.pos, 5, 0xFF94A3B8, 70f..210f)
            if (modifiers.hasRelic(RelicId.BANK_SHOT_PRO)) {
                state.shotScore.mult += 2
                addFloatingText(state, puck.pos + Vec2(0f, -24f), "BANK +2x", 0xFF0EA5E9, scale = 0.92f)
            }
        }
    }

    private fun resolvePowerUps(
        state: TableState,
        puck: Puck,
        newlySpawnedPucks: MutableList<Puck>,
        events: MutableList<SoundEvent>
    ) {
        for (pu in state.powerUps) {
            if (pu.collected) continue
            val dist = puck.pos.distanceTo(pu.pos)
            if (dist < puck.radius + pu.radius) {
                pu.collected = true
                events.add(SoundEvent.POWERUP_PICKUP)
                spawnShockwave(state, pu.pos, 120f, 0xFF06B6D4)
                spawnParticles(state, pu.pos, 16, 0xFF06B6D4, 140f..400f)

                when (pu.type) {
                    PowerUpType.MULTI_PUCK -> {
                        val copy = createSplitPuck(state, puck, 0.35f)
                        newlySpawnedPucks.add(copy)
                        addFloatingText(state, pu.pos, "MULTI-PUCK!", 0xFF0284C7, scale = 1.15f)
                    }
                    PowerUpType.MAGNET_PULL -> {
                        puck.magnetTimer = 5.5f
                        addFloatingText(state, pu.pos, "MAGNET ON!", 0xFF7C3AED, scale = 1.15f)
                    }
                    PowerUpType.OVERDRIVE_FIRE -> {
                        puck.overdriveTimer = 6.0f
                        puck.vel = (puck.vel * 1.18f).clampLength(MAX_PUCK_SPEED)
                        addFloatingText(state, pu.pos, "OVERDRIVE!", 0xFFF97316, scale = 1.15f)
                    }
                    PowerUpType.GIANT_PUCK -> {
                        puck.giantTimer = 6.0f
                        addFloatingText(state, pu.pos, "GIANT PUCK!", 0xFF10B981, scale = 1.15f)
                    }
                    PowerUpType.SCORE_SURGE -> {
                        state.shotScore.mult = max(2, state.shotScore.mult * 2)
                        addFloatingText(state, pu.pos, "SURGE x2 MULT!", 0xFFF59E0B, scale = 1.25f)
                    }
                }
            }
        }
    }

    private fun resolveTargets(
        state: TableState,
        puck: Puck,
        modifiers: RunModifiers,
        isTiltingActively: Boolean,
        events: MutableList<SoundEvent>
    ) {
        for (target in state.targets) {
            val delta = puck.pos - target.pos
            val distSq = delta.lengthSquared()
            val minDist = puck.radius + target.radius
            if (distSq >= minDist * minDist) continue

            if (target.type == TargetType.SPINNER_RING) {
                // Pass-through kinetic boost ring
                if (target.hitCooldown <= 0f) {
                    target.hitCooldown = 0.28f
                    target.hitFlash = 1f
                    target.hitCount++
                    puck.vel = (puck.vel * 1.16f).clampLength(MAX_PUCK_SPEED)
                    applyTargetScore(state, target, puck, modifiers, isTiltingActively, events)
                }
                continue
            }

            // Solid bumper bounce
            val dist = sqrt(max(distSq, 1e-4f))
            val normal = delta / dist
            val overlap = minDist - dist
            puck.pos += normal * (overlap + 0.5f)

            val vn = puck.vel.dot(normal)
            if (vn < 0f) {
                val kick = if (state.bossModifier == BossModifier.BUMPER_CHAOS) 240f else 110f
                val reflected = puck.vel - normal * ((1f + target.restitution) * vn) + normal * kick
                puck.vel = reflected.clampLength(MAX_PUCK_SPEED)
            }

            if (target.hitCooldown <= 0f) {
                target.hitCooldown = 0.14f
                target.hitFlash = 1f
                target.hitCount++
                applyTargetScore(state, target, puck, modifiers, isTiltingActively, events)
            }
        }
    }

    private fun applyTargetScore(
        state: TableState,
        target: Target,
        puck: Puck,
        modifiers: RunModifiers,
        isTiltingActively: Boolean,
        events: MutableList<SoundEvent>
    ) {
        val echoTrigger = modifiers.hasRelic(RelicId.LUCKY_BOUNCE) && random.nextFloat() < 0.30f
        val triggers = if (echoTrigger) 2 else 1

        repeat(triggers) {
            val rapidCombo = state.shotScore.lastHitElapsed < 0.55f
            state.shotScore.comboCount = if (rapidCombo) state.shotScore.comboCount + 1 else 1
            state.shotScore.lastHitElapsed = 0f
            state.shotScore.targetsHit++

            var chips = target.chipValue
            if (puck.isOverdrive) chips = (chips * 1.5f).toInt()
            if (modifiers.hasRelic(RelicId.COMBO_ENGINE) && state.shotScore.comboCount > 1) {
                chips += (state.shotScore.comboCount - 1) * 15
            }
            if (modifiers.hasRelic(RelicId.TILT_MASTER) && isTiltingActively) {
                chips += 25
            }

            state.shotScore.chips += chips
            state.shotScore.mult += target.multValue

            if (target.coinValue > 0) {
                state.shotScore.coinsEarnedThisShot += target.coinValue
            }

            if (modifiers.hasRelic(RelicId.MIDAS_TOUCH) && state.shotScore.targetsHit % 4 == 0) {
                state.shotScore.coinsEarnedThisShot += 1
                addFloatingText(state, target.pos + Vec2(0f, -44f), "+\$1", 0xFFEAB308, scale = 1.0f)
            }

            if (modifiers.hasRelic(RelicId.PINBALL_WIZARD) &&
                !state.shotScore.pinballWizardTriggered &&
                state.shotScore.targetsHit >= 5
            ) {
                state.shotScore.pinballWizardTriggered = true
                state.shotScore.mult = max(state.shotScore.mult + 1, (state.shotScore.mult * 1.5f).toInt())
                addFloatingText(state, target.pos + Vec2(0f, -60f), "PINBALL x1.5!", 0xFF8B5CF6, scale = 1.2f)
            }

            val label = buildString {
                if (chips > 0) append("+$chips")
                if (target.multValue > 0) {
                    if (isNotEmpty()) append(" ")
                    append("+${target.multValue}x")
                }
                if (target.coinValue > 0) append(" +\$${target.coinValue}")
            }
            val color = when (target.type) {
                TargetType.CHIP_BUMPER -> 0xFFFF5A5F
                TargetType.MULT_BUMPER -> 0xFFF59E0B
                TargetType.SPINNER_RING -> 0xFF06B6D4
                TargetType.JACKPOT_STAR -> 0xFF7C3AED
            }
            addFloatingText(state, target.pos + Vec2(0f, -24f), label, color, scale = if (echoTrigger) 1.15f else 1.0f)
            spawnParticles(state, target.pos, 10, color, 110f..320f)
        }

        if (echoTrigger) {
            addFloatingText(state, target.pos + Vec2(0f, -52f), "ECHO x2!", 0xFFEC4899, scale = 1.05f)
        }

        events.add(
            when (target.type) {
                TargetType.CHIP_BUMPER -> SoundEvent.CHIP_HIT
                TargetType.MULT_BUMPER -> SoundEvent.MULT_HIT
                TargetType.SPINNER_RING -> SoundEvent.SPINNER_HIT
                TargetType.JACKPOT_STAR -> SoundEvent.JACKPOT_HIT
            }
        )
    }

    private fun resolveObstacles(
        state: TableState,
        puck: Puck,
        modifiers: RunModifiers,
        events: MutableList<SoundEvent>
    ) {
        val baseDamage = (1 + if (modifiers.hasRelic(RelicId.HEAVY_METAL_PUCK)) 1 else 0) *
            (if (puck.isOverdrive) 2 else 1)

        val barrelsToExplode = mutableListOf<Obstacle>()

        for (obs in state.obstacles) {
            if (obs.destroyed) continue

            val hitNormal: Vec2
            val penetration: Float

            if (obs.isCircular) {
                val delta = puck.pos - obs.pos
                val distSq = delta.lengthSquared()
                val minDist = puck.radius + obs.radius
                if (distSq >= minDist * minDist) continue
                val dist = sqrt(max(distSq, 1e-4f))
                hitNormal = delta / dist
                penetration = minDist - dist
            } else {
                // Circle vs AABB collision
                val closestX = puck.pos.x.coerceIn(obs.pos.x - obs.halfW, obs.pos.x + obs.halfW)
                val closestY = puck.pos.y.coerceIn(obs.pos.y - obs.halfH, obs.pos.y + obs.halfH)
                val delta = puck.pos - Vec2(closestX, closestY)
                val distSq = delta.lengthSquared()
                if (distSq >= puck.radius * puck.radius) continue

                if (distSq > 1e-4f) {
                    val dist = sqrt(distSq)
                    hitNormal = delta / dist
                    penetration = puck.radius - dist
                } else {
                    // Puck center inside box fallback
                    val dx = puck.pos.x - obs.pos.x
                    val dy = puck.pos.y - obs.pos.y
                    val penX = obs.halfW - abs(dx)
                    val penY = obs.halfH - abs(dy)
                    if (penX < penY) {
                        hitNormal = Vec2(if (dx >= 0f) 1f else -1f, 0f)
                        penetration = penX + puck.radius
                    } else {
                        hitNormal = Vec2(0f, if (dy >= 0f) 1f else -1f)
                        penetration = penY + puck.radius
                    }
                }
            }

            // Apply damage & check overdrive piercing
            val willDestroy = obs.hp - baseDamage <= 0
            val shouldPierce = puck.isOverdrive && willDestroy && obs.type == ObstacleType.ICE_BLOCK

            if (!shouldPierce) {
                puck.pos += hitNormal * (penetration + 0.5f)
                val vn = puck.vel.dot(hitNormal)
                if (vn < 0f) {
                    val bounce = when (obs.type) {
                        ObstacleType.STEEL_CRATE -> 1.02f
                        ObstacleType.ICE_BLOCK -> 0.88f
                        ObstacleType.CRYO_BARREL -> 0.95f
                    }
                    puck.vel = (puck.vel - hitNormal * ((1f + bounce) * vn)).clampLength(MAX_PUCK_SPEED)
                }
            } else if (shouldPierce) {
                puck.vel *= 0.92f
            }

            damageObstacle(state, obs, baseDamage, modifiers, barrelsToExplode, events)
        }

        // Resolve chain-reaction barrel explosions
        var idx = 0
        while (idx < barrelsToExplode.size) {
            val barrel = barrelsToExplode[idx++]
            detonateBarrel(state, barrel, modifiers, barrelsToExplode, events)
        }
    }

    private fun damageObstacle(
        state: TableState,
        obs: Obstacle,
        damage: Int,
        modifiers: RunModifiers,
        barrelsToExplode: MutableList<Obstacle>,
        events: MutableList<SoundEvent>
    ) {
        if (obs.destroyed) return
        obs.hp -= damage
        obs.hitFlash = 1f

        if (obs.hp > 0) {
            events.add(SoundEvent.OBSTACLE_CRACK)
            spawnParticles(state, obs.pos, 7, 0xFFBAE6FD, 90f..250f, isShard = true)
            return
        }

        obs.destroyed = true
        state.shotScore.obstaclesBroken++
        var chips = obs.chipBonus
        if (modifiers.hasRelic(RelicId.ICEBREAKER)) {
            chips += 60
            state.shotScore.mult += 1
        }
        state.shotScore.chips += chips

        if (obs.dropsCoin > 0) {
            state.shotScore.coinsEarnedThisShot += obs.dropsCoin
            addFloatingText(state, obs.pos + Vec2(0f, -38f), "+\$${obs.dropsCoin}", 0xFFEAB308, scale = 1.05f)
        }

        obs.dropsPowerUp?.let { dropType ->
            state.powerUps.add(
                PowerUpPickup(
                    id = state.allocId(),
                    type = dropType,
                    pos = obs.pos
                )
            )
            addFloatingText(state, obs.pos + Vec2(0f, 34f), "${dropType.shortTag}!", 0xFF06B6D4, scale = 0.95f)
        }

        if (obs.type == ObstacleType.CRYO_BARREL) {
            barrelsToExplode.add(obs)
        } else {
            val shardColor = if (obs.type == ObstacleType.ICE_BLOCK) 0xFF7DD3FC else 0xFF64748B
            spawnParticles(state, obs.pos, 18, shardColor, 140f..440f, isShard = true)
            addFloatingText(
                state,
                obs.pos,
                if (modifiers.hasRelic(RelicId.ICEBREAKER)) "SHATTER! +$chips +1x" else "BREAK! +$chips",
                0xFF0284C7,
                scale = 1.05f
            )
            state.screenShake = max(state.screenShake, 5f)
            events.add(SoundEvent.OBSTACLE_SHATTER)
        }
    }

    private fun detonateBarrel(
        state: TableState,
        barrel: Obstacle,
        modifiers: RunModifiers,
        barrelsToExplode: MutableList<Obstacle>,
        events: MutableList<SoundEvent>
    ) {
        val blastRadius = if (modifiers.hasRelic(RelicId.CHAIN_REACTION)) 330f else 225f
        val extraChips = if (modifiers.hasRelic(RelicId.CHAIN_REACTION)) 100 else 0
        if (extraChips > 0) state.shotScore.chips += extraChips
        state.shotScore.mult += 1

        state.screenShake = max(state.screenShake, 15f)
        spawnShockwave(state, barrel.pos, blastRadius, 0xFFF97316)
        spawnParticles(state, barrel.pos, 28, 0xFFF97316, 180f..640f, isShard = true)
        addFloatingText(
            state,
            barrel.pos,
            "BOOM! +${barrel.chipBonus + extraChips} +1x",
            0xFFEA580C,
            scale = 1.25f
        )
        events.add(SoundEvent.BARREL_EXPLODE)

        // Blast nearby obstacles
        for (other in state.obstacles) {
            if (!other.destroyed && other.pos.distanceTo(barrel.pos) <= blastRadius) {
                damageObstacle(state, other, damage = 2, modifiers, barrelsToExplode, events)
            }
        }

        // Trigger nearby targets for explosive combo value
        for (target in state.targets) {
            if (target.pos.distanceTo(barrel.pos) <= blastRadius) {
                target.hitFlash = 1f
                state.shotScore.chips += target.chipValue
                state.shotScore.mult += target.multValue
                spawnParticles(state, target.pos, 8, 0xFFF59E0B, 100f..280f)
            }
        }

        // Radial kick to pucks
        for (p in state.pucks) {
            val delta = p.pos - barrel.pos
            val dist = delta.length()
            if (dist in 1f..blastRadius) {
                val impulse = (1f - dist / blastRadius) * 620f + 160f
                p.vel = (p.vel + delta.normalized() * impulse).clampLength(MAX_PUCK_SPEED)
            }
        }
    }

    private fun resolvePuckToPuck(a: Puck, b: Puck) {
        val delta = b.pos - a.pos
        val distSq = delta.lengthSquared()
        val minDist = a.radius + b.radius
        if (distSq >= minDist * minDist || distSq < 1e-4f) return

        val dist = sqrt(distSq)
        val normal = delta / dist
        val overlap = (minDist - dist) * 0.5f
        a.pos -= normal * overlap
        b.pos += normal * overlap

        val relVel = b.vel - a.vel
        val sepVel = relVel.dot(normal)
        if (sepVel > 0f) return

        val restitution = 0.94f
        val invMassA = 1f / a.effectiveMass
        val invMassB = 1f / b.effectiveMass
        val j = -(1f + restitution) * sepVel / (invMassA + invMassB)
        val impulse = normal * j
        a.vel -= impulse * invMassA
        b.vel += impulse * invMassB
    }

    private fun spawnTwinPuck(state: TableState, source: Puck, angleOffsetRad: Float) {
        state.pucks.add(createSplitPuck(state, source, angleOffsetRad))
    }

    private fun createSplitPuck(state: TableState, source: Puck, angleOffsetRad: Float): Puck {
        val baseAngle = atan2(source.vel.y, source.vel.x)
        val speed = max(source.vel.length(), 650f)
        val newAngle = baseAngle + angleOffsetRad
        val dir = Vec2(cos(newAngle), sin(newAngle))
        return Puck(
            id = state.allocId(),
            pos = source.pos + dir * 24f,
            vel = dir * speed,
            baseRadius = source.baseRadius * 0.92f,
            mass = source.mass * 0.9f,
            isMainPuck = false,
            overdriveTimer = source.overdriveTimer,
            magnetTimer = source.magnetTimer
        )
    }

    private fun updateMovingTargets(state: TableState, dt: Float) {
        for (target in state.targets) {
            if (target.patrolAmplitude > 0f && target.patrolSpeed > 0f) {
                target.patrolPhase += dt * target.patrolSpeed
                val offset = sin(target.patrolPhase) * target.patrolAmplitude
                target.pos = target.patrolCenter + target.patrolAxis * offset
            }
        }
    }

    private fun updatePowerUpPulses(state: TableState, dt: Float) {
        for (pu in state.powerUps) {
            if (!pu.collected) {
                pu.pulsePhase += dt * 3.8f
            }
        }
    }

    private fun updateEffects(state: TableState, dt: Float) {
        val pIter = state.particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
            } else {
                p.pos += p.vel * dt
                p.vel *= (1f - 2.4f * dt).coerceAtLeast(0.5f)
                p.rotation += p.rotSpeed * dt
            }
        }

        val sIter = state.shockwaves.iterator()
        while (sIter.hasNext()) {
            val s = sIter.next()
            s.life -= dt
            if (s.life <= 0f) {
                sIter.remove()
            } else {
                val progress = 1f - (s.life / s.maxLife)
                s.radius = s.maxRadius * progress
            }
        }

        val tIter = state.floatingTexts.iterator()
        while (tIter.hasNext()) {
            val t = tIter.next()
            t.life -= dt
            if (t.life <= 0f) {
                tIter.remove()
            } else {
                t.pos += Vec2(0f, -68f * dt)
            }
        }
    }

    private fun spawnParticles(
        state: TableState,
        origin: Vec2,
        count: Int,
        colorHex: Long,
        speedRange: ClosedFloatingPointRange<Float>,
        isShard: Boolean = false
    ) {
        if (state.particles.size > 220) return
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = speedRange.start + random.nextFloat() * (speedRange.endInclusive - speedRange.start)
            val life = 0.28f + random.nextFloat() * 0.34f
            state.particles.add(
                Particle(
                    pos = origin,
                    vel = Vec2(cos(angle) * speed, sin(angle) * speed),
                    life = life,
                    maxLife = life,
                    size = if (isShard) 7f + random.nextFloat() * 9f else 4f + random.nextFloat() * 5f,
                    colorHex = colorHex,
                    isShard = isShard,
                    rotation = random.nextFloat() * 360f,
                    rotSpeed = (random.nextFloat() - 0.5f) * 540f
                )
            )
        }
    }

    private fun spawnShockwave(state: TableState, origin: Vec2, maxRadius: Float, colorHex: Long) {
        state.shockwaves.add(
            ShockwaveRing(
                pos = origin,
                radius = 12f,
                maxRadius = maxRadius,
                life = 0.36f,
                maxLife = 0.36f,
                colorHex = colorHex
            )
        )
    }

    private fun addFloatingText(
        state: TableState,
        pos: Vec2,
        text: String,
        colorHex: Long,
        scale: Float = 1f
    ) {
        if (state.floatingTexts.size > 26) state.floatingTexts.removeAt(0)
        state.floatingTexts.add(
            FloatingText(
                pos = pos,
                text = text,
                colorHex = colorHex,
                life = 0.85f,
                maxLife = 0.85f,
                scale = scale
            )
        )
    }

    /**
     * Computes a crisp 1-bounce aim preview polyline for slingshot / aim UI.
     */
    fun computeAimTrajectory(
        start: Vec2,
        direction: Vec2,
        puckRadius: Float,
        maxLength: Float = 720f
    ): List<Vec2> {
        val dir = direction.normalized()
        if (dir.lengthSquared() < 0.01f) return listOf(start)

        val minX = TableConstants.WALL_MARGIN + puckRadius
        val maxX = TableConstants.TABLE_WIDTH - TableConstants.WALL_MARGIN - puckRadius
        val minY = TableConstants.WALL_MARGIN + puckRadius
        val maxY = TableConstants.TABLE_HEIGHT - TableConstants.WALL_MARGIN - puckRadius

        var tBest = maxLength
        var hitNormal = Vec2.ZERO

        if (dir.x < -1e-4f) {
            val t = (minX - start.x) / dir.x
            if (t in 0f..tBest) {
                tBest = t
                hitNormal = Vec2(1f, 0f)
            }
        } else if (dir.x > 1e-4f) {
            val t = (maxX - start.x) / dir.x
            if (t in 0f..tBest) {
                tBest = t
                hitNormal = Vec2(-1f, 0f)
            }
        }

        if (dir.y < -1e-4f) {
            val t = (minY - start.y) / dir.y
            if (t in 0f..tBest) {
                tBest = t
                hitNormal = Vec2(0f, 1f)
            }
        } else if (dir.y > 1e-4f) {
            val t = (maxY - start.y) / dir.y
            if (t in 0f..tBest) {
                tBest = t
                hitNormal = Vec2(0f, -1f)
            }
        }

        val hitPoint = start + dir * tBest
        val points = mutableListOf(start, hitPoint)

        if (hitNormal.lengthSquared() > 0.5f && tBest < maxLength) {
            val remaining = min(240f, maxLength - tBest)
            val bounceDir = dir.reflect(hitNormal).normalized()
            points.add(hitPoint + bounceDir * remaining)
        }
        return points
    }
}
