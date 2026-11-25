package com.roguehockey.game.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.roguehockey.game.engine.BossModifier
import com.roguehockey.game.engine.ControlMode
import com.roguehockey.game.engine.Obstacle
import com.roguehockey.game.engine.ObstacleType
import com.roguehockey.game.engine.PhysicsEngine
import com.roguehockey.game.engine.PowerUpPickup
import com.roguehockey.game.engine.PowerUpType
import com.roguehockey.game.engine.Puck
import com.roguehockey.game.engine.ShotPhase
import com.roguehockey.game.engine.TableConstants
import com.roguehockey.game.engine.TableState
import com.roguehockey.game.engine.Target
import com.roguehockey.game.engine.TargetType
import com.roguehockey.game.engine.TiltSensitivity
import com.roguehockey.game.engine.Vec2
import com.roguehockey.game.ui.theme.TabletopColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class AimVisualState(
    val active: Boolean = false,
    val isSlingshot: Boolean = true,
    val anchorWorld: Vec2 = Vec2.ZERO,
    val currentWorld: Vec2 = Vec2.ZERO,
    val launchVelocity: Vec2 = Vec2.ZERO,
    val powerFraction: Float = 0f
)

private data class TableViewport(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun screenToWorld(offset: Offset): Vec2 = Vec2(
        x = (offset.x - offsetX) / scale,
        y = (offset.y - offsetY) / scale
    )

    fun worldToScreen(v: Vec2): Offset = Offset(
        x = offsetX + v.x * scale,
        y = offsetY + v.y * scale
    )
}

private class ViewportHolder(var current: TableViewport = TableViewport())

@Composable
fun TableCanvas(
    tableState: TableState,
    frameCounter: Long,
    physics: PhysicsEngine,
    controlMode: ControlMode,
    tiltInput: Vec2,
    tiltSensitivity: TiltSensitivity,
    launchSpeedMultiplier: Float,
    onLaunchPuck: (Vec2) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestState by rememberUpdatedState(tableState)
    val latestControlMode by rememberUpdatedState(controlMode)
    val latestLaunchMult by rememberUpdatedState(launchSpeedMultiplier)
    val latestOnLaunch by rememberUpdatedState(onLaunchPuck)

    val viewportHolder = remember { ViewportHolder() }
    var aimVisual by remember { mutableStateOf(AimVisualState()) }

    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.STROKE
            color = android.graphics.Color.WHITE
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val state = latestState
                        if (state.shotPhase != ShotPhase.AIMING) return@awaitEachGesture
                        val puck = state.pucks.firstOrNull() ?: return@awaitEachGesture

                        val vp = viewportHolder.current
                        if (vp.scale <= 0.01f) return@awaitEachGesture

                        val startWorld = vp.screenToWorld(down.position)
                        var currentWorld = startWorld
                        val startTimeMs = down.uptimeMillis
                        var lastTimeMs = startTimeMs
                        var peakFlickVel = Vec2.ZERO
                        var smoothedFlickVel = Vec2.ZERO
                        var movedDistance = 0f

                        aimVisual = AimVisualState(
                            active = true,
                            isSlingshot = latestControlMode != ControlMode.FLICK,
                            anchorWorld = puck.pos,
                            currentWorld = startWorld
                        )

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val nextWorld = vp.screenToWorld(change.position)
                            val deltaWorld = nextWorld - currentWorld
                            val dtSec = ((change.uptimeMillis - lastTimeMs).coerceAtLeast(1L)) / 1000f

                            if (deltaWorld.lengthSquared() > 0.25f) {
                                val instVel = deltaWorld / dtSec
                                smoothedFlickVel = smoothedFlickVel * 0.45f + instVel * 0.55f
                                if (smoothedFlickVel.lengthSquared() > peakFlickVel.lengthSquared()) {
                                    peakFlickVel = smoothedFlickVel
                                }
                                movedDistance += deltaWorld.length()
                            }

                            currentWorld = nextWorld
                            lastTimeMs = change.uptimeMillis

                            val maxSpeed = TableConstants.MAX_LAUNCH_SPEED * latestLaunchMult
                            val slingshotPull = startWorld - currentWorld
                            val elapsedMs = change.uptimeMillis - startTimeMs
                            val isQuickUpwardFlick = latestControlMode == ControlMode.FLICK ||
                                (latestControlMode == ControlMode.AUTO &&
                                    elapsedMs < 240L &&
                                    smoothedFlickVel.length() > 700f &&
                                    (currentWorld.y < startWorld.y - 35f))

                            val previewVel = if (isQuickUpwardFlick) {
                                (smoothedFlickVel * 1.18f).clampLength(maxSpeed)
                            } else {
                                // Slingshot: pull backward from touch anchor or puck
                                val pullVec = if (startWorld.distanceTo(puck.pos) < 150f) {
                                    puck.pos - currentWorld
                                } else {
                                    slingshotPull
                                }
                                val power = (pullVec.length() / 280f).coerceIn(0f, 1f)
                                pullVec.normalized() * (power * maxSpeed)
                            }

                            aimVisual = AimVisualState(
                                active = true,
                                isSlingshot = !isQuickUpwardFlick,
                                anchorWorld = puck.pos,
                                currentWorld = currentWorld,
                                launchVelocity = previewVel,
                                powerFraction = (previewVel.length() / maxSpeed).coerceIn(0f, 1f)
                            )

                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                            if (!change.pressed) {
                                // Determine final launch velocity
                                val finalElapsed = change.uptimeMillis - startTimeMs
                                val useFlick = when (latestControlMode) {
                                    ControlMode.FLICK -> true
                                    ControlMode.SLINGSHOT -> false
                                    ControlMode.AUTO ->
                                        finalElapsed < 250L &&
                                            peakFlickVel.length() > 620f &&
                                            (currentWorld.y < startWorld.y - 24f ||
                                                abs(currentWorld.x - startWorld.x) > 40f &&
                                                (puck.pos - currentWorld).length() < 90f)
                                }

                                val finalVel = if (useFlick) {
                                    val chosen = if (smoothedFlickVel.length() > 350f) smoothedFlickVel else peakFlickVel
                                    (chosen * 1.22f).clampLength(maxSpeed)
                                } else {
                                    val pullVec = if (startWorld.distanceTo(puck.pos) < 150f) {
                                        puck.pos - currentWorld
                                    } else {
                                        startWorld - currentWorld
                                    }
                                    val power = (pullVec.length() / 280f).coerceIn(0f, 1f)
                                    pullVec.normalized() * (power * maxSpeed)
                                }

                                aimVisual = AimVisualState(active = false)
                                if (movedDistance > 18f && finalVel.length() >= TableConstants.MIN_LAUNCH_SPEED) {
                                    latestOnLaunch(finalVel)
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            // Read frameCounter so Compose redraws every frame smoothly
            val tickUnused = frameCounter

            val pad = 10f
            val availW = (size.width - pad * 2f).coerceAtLeast(1f)
            val availH = (size.height - pad * 2f).coerceAtLeast(1f)
            val scale = min(availW / TableConstants.TABLE_WIDTH, availH / TableConstants.TABLE_HEIGHT)
            val tablePxW = TableConstants.TABLE_WIDTH * scale
            val tablePxH = TableConstants.TABLE_HEIGHT * scale
            val baseOffsetX = (size.width - tablePxW) * 0.5f
            val baseOffsetY = (size.height - tablePxH) * 0.5f

            viewportHolder.current = TableViewport(scale, baseOffsetX, baseOffsetY)

            // Apply subtle screen shake
            val shake = tableState.screenShake * scale * 0.85f
            val shakeX = if (shake > 0.2f) sin(tableState.tableTime * 75f) * shake else 0f
            val shakeY = if (shake > 0.2f) cos(tableState.tableTime * 91f) * shake else 0f
            val vp = TableViewport(scale, baseOffsetX + shakeX, baseOffsetY + shakeY)

            // 1. Table Frame & Frosted Ice Surface
            drawTableSurface(vp, tableState)

            // 2. Boss Crosswind Chevrons
            if (tableState.bossModifier == BossModifier.MAGNETIC_DRIFT) {
                drawCrosswindIndicator(vp, tableState)
            }

            // 3. Top Goal Gate
            drawGoalGate(vp, tableState)

            // 4. Power-Up Pickups
            for (pu in tableState.powerUps) {
                if (!pu.collected) drawPowerUp(vp, pu, textPaint)
            }

            // 5. Targets
            for (t in tableState.targets) {
                drawTarget(vp, t, textPaint)
            }

            // 6. Destructible Obstacles
            for (obs in tableState.obstacles) {
                if (!obs.destroyed) drawObstacle(vp, obs, textPaint)
            }

            // 7. Shockwaves & Particles (under/around puck)
            drawShockwaves(vp, tableState)
            drawParticles(vp, tableState)

            // 8. Aim Trajectory Preview (when aiming)
            if (aimVisual.active && tableState.shotPhase == ShotPhase.AIMING) {
                drawAimPreview(vp, aimVisual, tableState, physics)
            }

            // 9. Pucks
            for (puck in tableState.pucks) {
                drawPuck(vp, puck, tableState.shotPhase == ShotPhase.AIMING, tableState.tableTime)
            }

            // 10. Floating Score Texts
            drawFloatingTexts(vp, tableState, textPaint, strokePaint)

            // 11. Live Tilt Level Bubble on bottom-right corner rail
            if (tiltSensitivity != TiltSensitivity.OFF) {
                drawTiltBubble(vp, tiltInput)
            }

            // 12. Shot Banked Juicy Center Popup
            if (tableState.shotPhase == ShotPhase.SCORING_POPUP && tableState.lastBankedScore > 0) {
                drawShotBankedBanner(vp, tableState, textPaint, strokePaint)
            }
        }
    }
}

private fun DrawScope.drawTableSurface(vp: TableViewport, state: TableState) {
    val outerTopLeft = vp.worldToScreen(Vec2(0f, 0f))
    val outerSize = Size(TableConstants.TABLE_WIDTH * vp.scale, TableConstants.TABLE_HEIGHT * vp.scale)
    val corner = CornerRadius(40f * vp.scale, 40f * vp.scale)

    // Outer tactile aluminum/slate bumper rail
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF334155), Color(0xFF1E293B))
        ),
        topLeft = outerTopLeft,
        size = outerSize,
        cornerRadius = corner
    )
    drawRoundRect(
        color = TabletopColors.AluminumRim.copy(alpha = 0.45f),
        topLeft = outerTopLeft,
        size = outerSize,
        cornerRadius = corner,
        style = Stroke(width = 4f * vp.scale)
    )

    // Inner frosted ice surface
    val m = TableConstants.WALL_MARGIN
    val innerTopLeft = vp.worldToScreen(Vec2(m, m))
    val innerSize = Size(
        (TableConstants.TABLE_WIDTH - m * 2f) * vp.scale,
        (TableConstants.TABLE_HEIGHT - m * 2f) * vp.scale
    )
    val innerCorner = CornerRadius(24f * vp.scale, 24f * vp.scale)

    val iceEdgeColor = if (state.bossModifier == BossModifier.HEAVY_ICE) {
        Color(0xFFCDE7F8)
    } else {
        TabletopColors.IceEdge
    }

    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(TabletopColors.IceCenter, iceEdgeColor),
            center = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH * 0.5f, TableConstants.TABLE_HEIGHT * 0.44f)),
            radius = TableConstants.TABLE_HEIGHT * 0.72f * vp.scale
        ),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = innerCorner
    )

    // Crisp subtle air-hockey rink markings
    val centerY = TableConstants.TABLE_HEIGHT * 0.5f
    val centerPt = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH * 0.5f, centerY))

    // Center red line
    drawLine(
        color = TabletopColors.IceLineRed.copy(alpha = 0.22f),
        start = vp.worldToScreen(Vec2(m + 8f, centerY)),
        end = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH - m - 8f, centerY)),
        strokeWidth = 6f * vp.scale
    )

    // Blue attack lines
    val upperBlueY = 470f
    val launchLineY = TableConstants.LAUNCH_LINE_Y
    drawLine(
        color = TabletopColors.IceLineBlue.copy(alpha = 0.22f),
        start = vp.worldToScreen(Vec2(m + 8f, upperBlueY)),
        end = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH - m - 8f, upperBlueY)),
        strokeWidth = 5f * vp.scale
    )
    drawLine(
        color = TabletopColors.IceLineBlue.copy(alpha = 0.30f),
        start = vp.worldToScreen(Vec2(m + 8f, launchLineY)),
        end = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH - m - 8f, launchLineY)),
        strokeWidth = 4f * vp.scale,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * vp.scale, 14f * vp.scale))
    )

    // Center faceoff circle
    drawCircle(
        color = TabletopColors.IceLineBlue.copy(alpha = 0.22f),
        radius = 128f * vp.scale,
        center = centerPt,
        style = Stroke(width = 5f * vp.scale)
    )
    drawCircle(
        color = TabletopColors.IceLineRed.copy(alpha = 0.22f),
        radius = 16f * vp.scale,
        center = centerPt
    )

    // Launch zone subtle tint
    drawRoundRect(
        color = TabletopColors.IceLineBlue.copy(alpha = 0.06f),
        topLeft = vp.worldToScreen(Vec2(m + 4f, launchLineY)),
        size = Size(
            (TableConstants.TABLE_WIDTH - m * 2f - 8f) * vp.scale,
            (TableConstants.TABLE_HEIGHT - m - launchLineY - 4f) * vp.scale
        ),
        cornerRadius = CornerRadius(18f * vp.scale, 18f * vp.scale)
    )

    // Subtle inner rail shadow border
    drawRoundRect(
        color = Color(0xFF94A3B8).copy(alpha = 0.45f),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = innerCorner,
        style = Stroke(width = 3f * vp.scale)
    )
}

private fun DrawScope.drawCrosswindIndicator(vp: TableViewport, state: TableState) {
    val dir = if (state.currentWindX >= 0f) 1f else -1f
    val alpha = (abs(state.currentWindX) / 520f).coerceIn(0.15f, 0.45f)
    val ys = listOf(520f, 700f, 880f)
    val drift = ((state.tableTime * 180f * dir) % 160f)

    for (y in ys) {
        for (i in 0..4) {
            val x = (160f + i * 170f + drift).coerceIn(80f, 920f)
            val c = vp.worldToScreen(Vec2(x, y))
            val len = 26f * vp.scale * dir
            val h = 14f * vp.scale
            drawLine(
                color = TabletopColors.Sky.copy(alpha = alpha),
                start = Offset(c.x - len, c.y - h),
                end = Offset(c.x + len * 0.4f, c.y),
                strokeWidth = 4f * vp.scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = TabletopColors.Sky.copy(alpha = alpha),
                start = Offset(c.x - len, c.y + h),
                end = Offset(c.x + len * 0.4f, c.y),
                strokeWidth = 4f * vp.scale,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawGoalGate(vp: TableViewport, state: TableState) {
    val gate = state.goalGate
    val topLeft = vp.worldToScreen(Vec2(gate.xMin, 6f))
    val gateW = gate.width * vp.scale
    val gateH = (TableConstants.WALL_MARGIN + 10f) * vp.scale
    val glowAlpha = 0.25f + gate.flashTimer * 0.65f

    // Crease semicircle on ice
    drawArc(
        color = TabletopColors.Emerald.copy(alpha = 0.14f + gate.flashTimer * 0.35f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = vp.worldToScreen(Vec2(gate.xMin - 24f, TableConstants.WALL_MARGIN - 88f)),
        size = Size((gate.width + 48f) * vp.scale, 176f * vp.scale)
    )

    // Recessed goal pocket
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = topLeft,
        size = Size(gateW, gateH),
        cornerRadius = CornerRadius(12f * vp.scale, 12f * vp.scale)
    )
    drawRoundRect(
        color = TabletopColors.Emerald.copy(alpha = glowAlpha),
        topLeft = topLeft,
        size = Size(gateW, gateH),
        cornerRadius = CornerRadius(12f * vp.scale, 12f * vp.scale),
        style = Stroke(width = 4f * vp.scale)
    )

    // Goal posts
    val postRadius = 11f * vp.scale
    drawCircle(
        color = TabletopColors.Emerald,
        radius = postRadius,
        center = vp.worldToScreen(Vec2(gate.xMin, TableConstants.WALL_MARGIN))
    )
    drawCircle(
        color = TabletopColors.Emerald,
        radius = postRadius,
        center = vp.worldToScreen(Vec2(gate.xMax, TableConstants.WALL_MARGIN))
    )
}

private fun DrawScope.drawTarget(vp: TableViewport, target: Target, textPaint: Paint) {
    val center = vp.worldToScreen(target.pos)
    val r = target.radius * vp.scale * (1f + target.hitFlash * 0.14f)

    // Soft tactile drop shadow
    drawCircle(
        color = Color(0xFF0F172A).copy(alpha = 0.14f),
        radius = r,
        center = center + Offset(3f * vp.scale, 6f * vp.scale)
    )

    when (target.type) {
        TargetType.CHIP_BUMPER -> {
            drawCircle(color = TabletopColors.Coral, radius = r, center = center)
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = r * 0.72f, center = center)
            drawCircle(color = TabletopColors.Coral.copy(alpha = 0.18f), radius = r * 0.56f, center = center)
            drawLabel(center, "+${target.chipValue}", 19f * vp.scale, 0xFFDC2626.toInt(), textPaint)
        }

        TargetType.MULT_BUMPER -> {
            drawCircle(color = TabletopColors.Amber, radius = r, center = center)
            drawCircle(color = Color(0xFFFFFBEB), radius = r * 0.74f, center = center)
            drawCircle(
                color = TabletopColors.Amber,
                radius = r * 0.85f,
                center = center,
                style = Stroke(width = 3f * vp.scale)
            )
            drawLabel(center, "+${target.multValue}x", 21f * vp.scale, 0xFFB45309.toInt(), textPaint)
        }

        TargetType.SPINNER_RING -> {
            drawCircle(
                color = TabletopColors.Cyan.copy(alpha = 0.16f + target.hitFlash * 0.3f),
                radius = r,
                center = center
            )
            rotate(degrees = target.spinAngle * 57.3f, pivot = center) {
                drawCircle(
                    color = TabletopColors.Cyan,
                    radius = r * 0.88f,
                    center = center,
                    style = Stroke(
                        width = 6f * vp.scale,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f * vp.scale, 14f * vp.scale))
                    )
                )
            }
            drawLabel(center, "BOOST", 13f * vp.scale, 0xFF0E7490.toInt(), textPaint)
        }

        TargetType.JACKPOT_STAR -> {
            drawCircle(color = TabletopColors.Violet, radius = r, center = center)
            drawCircle(color = Color(0xFFF5F3FF), radius = r * 0.76f, center = center)
            // Star polygon accent
            val starPath = Path()
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) r * 0.62f else r * 0.30f
                val ang = -PI.toFloat() / 2f + i * (PI.toFloat() / 5f)
                val px = center.x + cos(ang) * rad
                val py = center.y + sin(ang) * rad
                if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()
            drawPath(starPath, color = TabletopColors.GoldCoin.copy(alpha = 0.85f))
            drawLabel(center, "★\$${target.coinValue}", 16f * vp.scale, 0xFF4C1D95.toInt(), textPaint)
        }
    }

    if (target.hitFlash > 0.05f) {
        drawCircle(
            color = Color.White.copy(alpha = target.hitFlash * 0.55f),
            radius = r * (1.05f + target.hitFlash * 0.15f),
            center = center,
            style = Stroke(width = 4f * vp.scale)
        )
    }
}

private fun DrawScope.drawObstacle(vp: TableViewport, obs: Obstacle, textPaint: Paint) {
    val center = vp.worldToScreen(obs.pos)
    val shadowOffset = Offset(3f * vp.scale, 6f * vp.scale)

    if (obs.isCircular) {
        // Cryo-Barrel
        val r = obs.radius * vp.scale * (1f + obs.hitFlash * 0.12f)
        drawCircle(color = Color(0xFF0F172A).copy(alpha = 0.18f), radius = r, center = center + shadowOffset)
        drawCircle(color = TabletopColors.Orange, radius = r, center = center)
        drawCircle(color = Color(0xFFFFEDD5), radius = r * 0.68f, center = center)
        drawCircle(
            color = Color(0xFFEA580C),
            radius = r * 0.84f,
            center = center,
            style = Stroke(width = 4f * vp.scale)
        )
        drawLabel(center, "TNT", 16f * vp.scale, 0xFF9A3412.toInt(), textPaint)
        return
    }

    val w = obs.halfW * 2f * vp.scale
    val h = obs.halfH * 2f * vp.scale
    val topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f)
    val cr = CornerRadius(12f * vp.scale, 12f * vp.scale)

    // Drop shadow
    drawRoundRect(
        color = Color(0xFF0F172A).copy(alpha = 0.15f),
        topLeft = topLeft + shadowOffset,
        size = Size(w, h),
        cornerRadius = cr
    )

    when (obs.type) {
        ObstacleType.ICE_BLOCK -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC))),
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = cr
            )
            // Inner frosted facet highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f),
                topLeft = topLeft + Offset(4f * vp.scale, 4f * vp.scale),
                size = Size((w - 8f * vp.scale).coerceAtLeast(1f), (h * 0.4f).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(8f * vp.scale, 8f * vp.scale)
            )
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = cr,
                style = Stroke(width = 3f * vp.scale)
            )
            // Progressive crack lines when damaged
            if (obs.hp < obs.maxHp) {
                val crackColor = Color(0xFF0369A1)
                drawLine(
                    color = crackColor,
                    start = Offset(center.x - w * 0.22f, topLeft.y + 4f),
                    end = Offset(center.x + w * 0.05f, center.y + h * 0.28f),
                    strokeWidth = 2.6f * vp.scale
                )
                drawLine(
                    color = crackColor,
                    start = Offset(center.x + w * 0.05f, center.y + h * 0.28f),
                    end = Offset(center.x + w * 0.26f, topLeft.y + h - 4f),
                    strokeWidth = 2.2f * vp.scale
                )
            }
            drawLabel(center, "${obs.hp}", 18f * vp.scale, 0xFF0C4A6E.toInt(), textPaint)
        }

        ObstacleType.STEEL_CRATE -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF475569))),
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = cr
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = cr,
                style = Stroke(width = 3.5f * vp.scale)
            )
            // Corner rivets
            val inset = 9f * vp.scale
            val rivetColor = Color(0xFFCBD5E1)
            val corners = listOf(
                Offset(topLeft.x + inset, topLeft.y + inset),
                Offset(topLeft.x + w - inset, topLeft.y + inset),
                Offset(topLeft.x + inset, topLeft.y + h - inset),
                Offset(topLeft.x + w - inset, topLeft.y + h - inset)
            )
            for (c in corners) drawCircle(rivetColor, 3f * vp.scale, c)

            val crateLabel = buildString {
                append("${obs.hp}")
                if (obs.dropsPowerUp != null) append(" ★")
                else if (obs.dropsCoin > 0) append(" \$")
            }
            drawLabel(center, crateLabel, 17f * vp.scale, android.graphics.Color.WHITE, textPaint)
        }

        ObstacleType.CRYO_BARREL -> Unit
    }

    if (obs.hitFlash > 0.05f) {
        drawRoundRect(
            color = Color.White.copy(alpha = obs.hitFlash * 0.6f),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = cr
        )
    }
}

private fun DrawScope.drawPowerUp(vp: TableViewport, pu: PowerUpPickup, textPaint: Paint) {
    val center = vp.worldToScreen(pu.pos)
    val pulse = 1f + sin(pu.pulsePhase) * 0.09f
    val r = pu.radius * vp.scale * pulse

    val accent = when (pu.type) {
        PowerUpType.MULTI_PUCK -> TabletopColors.Sky
        PowerUpType.MAGNET_PULL -> TabletopColors.Violet
        PowerUpType.OVERDRIVE_FIRE -> TabletopColors.Orange
        PowerUpType.GIANT_PUCK -> TabletopColors.Emerald
        PowerUpType.SCORE_SURGE -> TabletopColors.Amber
    }

    // Pulsing outer ring
    drawCircle(
        color = accent.copy(alpha = 0.22f),
        radius = r * 1.28f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = r,
        center = center
    )
    drawCircle(
        color = accent,
        radius = r,
        center = center,
        style = Stroke(width = 4f * vp.scale)
    )
    val iconText = when (pu.type) {
        PowerUpType.MULTI_PUCK -> "x2"
        PowerUpType.MAGNET_PULL -> "MAG"
        PowerUpType.OVERDRIVE_FIRE -> "FIRE"
        PowerUpType.GIANT_PUCK -> "BIG"
        PowerUpType.SCORE_SURGE -> "2xM"
    }
    val textColorInt = when (pu.type) {
        PowerUpType.MULTI_PUCK -> 0xFF0369A1.toInt()
        PowerUpType.MAGNET_PULL -> 0xFF6D28D9.toInt()
        PowerUpType.OVERDRIVE_FIRE -> 0xFFC2410C.toInt()
        PowerUpType.GIANT_PUCK -> 0xFF047857.toInt()
        PowerUpType.SCORE_SURGE -> 0xFFB45309.toInt()
    }
    drawLabel(center, iconText, 14f * vp.scale, textColorInt, textPaint)
}

private fun DrawScope.drawPuck(
    vp: TableViewport,
    puck: Puck,
    isAiming: Boolean,
    tableTime: Float
) {
    // Sleek motion ribbon trail
    if (puck.trail.size >= 2) {
        val trailColor = when {
            puck.isOverdrive -> TabletopColors.Orange
            puck.isMagnet -> TabletopColors.Violet
            else -> TabletopColors.Sky
        }
        for (i in 1 until puck.trail.size) {
            val frac = i.toFloat() / puck.trail.size
            drawLine(
                color = trailColor.copy(alpha = frac * 0.36f),
                start = vp.worldToScreen(puck.trail[i - 1]),
                end = vp.worldToScreen(puck.trail[i]),
                strokeWidth = puck.radius * 1.15f * frac * vp.scale,
                cap = StrokeCap.Round
            )
        }
    }

    val center = vp.worldToScreen(puck.pos)
    val r = puck.radius * vp.scale

    // Ready-to-flick pulse ring
    if (isAiming && puck.isMainPuck) {
        val ringScale = 1.25f + sin(tableTime * 4.2f) * 0.12f
        drawCircle(
            color = TabletopColors.Sky.copy(alpha = 0.25f),
            radius = r * ringScale,
            center = center,
            style = Stroke(width = 3.5f * vp.scale)
        )
    }

    // Active power-up auras
    if (puck.isMagnet) {
        drawCircle(
            color = TabletopColors.Violet.copy(alpha = 0.22f),
            radius = r * 1.55f,
            center = center,
            style = Stroke(
                width = 3f * vp.scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * vp.scale, 10f * vp.scale))
            )
        )
    }
    if (puck.isOverdrive) {
        drawCircle(
            color = TabletopColors.Orange.copy(alpha = 0.32f),
            radius = r * 1.32f,
            center = center
        )
    }

    // Tactile puck drop shadow
    drawCircle(
        color = Color(0xFF0F172A).copy(alpha = 0.24f),
        radius = r,
        center = center + Offset(4f * vp.scale, 7f * vp.scale)
    )

    // Puck outer dark shell
    val outerColor = if (puck.isMainPuck) Color(0xFF1E293B) else Color(0xFF0369A1)
    val ringColor = when {
        puck.isOverdrive -> TabletopColors.Orange
        puck.isGiant -> TabletopColors.Emerald
        puck.isMainPuck -> TabletopColors.Coral
        else -> TabletopColors.Cyan
    }

    drawCircle(color = outerColor, radius = r, center = center)
    drawCircle(color = ringColor, radius = r * 0.74f, center = center)
    drawCircle(color = Color.White, radius = r * 0.36f, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = r * 0.90f,
        center = center,
        style = Stroke(width = 2.5f * vp.scale)
    )
}

private fun DrawScope.drawAimPreview(
    vp: TableViewport,
    aim: AimVisualState,
    state: TableState,
    physics: PhysicsEngine
) {
    val puck = state.pucks.firstOrNull() ?: return
    if (aim.launchVelocity.length() < TableConstants.MIN_LAUNCH_SPEED * 0.65f) return

    val trajectory = physics.computeAimTrajectory(
        start = puck.pos,
        direction = aim.launchVelocity,
        puckRadius = puck.radius,
        maxLength = 420f + aim.powerFraction * 420f
    )

    val powerColor = when {
        aim.powerFraction > 0.78f -> TabletopColors.Coral
        aim.powerFraction > 0.42f -> TabletopColors.Amber
        else -> TabletopColors.Sky
    }

    // Dotted raycast bounce trajectory
    if (trajectory.size >= 2) {
        val path = Path().apply {
            val first = vp.worldToScreen(trajectory.first())
            moveTo(first.x, first.y)
            for (i in 1 until trajectory.size) {
                val p = vp.worldToScreen(trajectory[i])
                lineTo(p.x, p.y)
            }
        }
        drawPath(
            path = path,
            color = powerColor.copy(alpha = 0.85f),
            style = Stroke(
                width = 5f * vp.scale,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f * vp.scale, 14f * vp.scale))
            )
        )
        // Bounce target dot
        val lastPt = vp.worldToScreen(trajectory.last())
        drawCircle(color = powerColor, radius = 8f * vp.scale, center = lastPt)
    }

    // Slingshot tension band behind puck
    if (aim.isSlingshot) {
        val puckScreen = vp.worldToScreen(puck.pos)
        val tailWorld = puck.pos - aim.launchVelocity.normalized() * (60f + aim.powerFraction * 165f)
        val tailScreen = vp.worldToScreen(tailWorld)
        drawLine(
            color = powerColor.copy(alpha = 0.6f),
            start = puckScreen,
            end = tailScreen,
            strokeWidth = 7f * vp.scale,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = powerColor,
            radius = 12f * vp.scale,
            center = tailScreen
        )
    }
}

private fun DrawScope.drawShockwaves(vp: TableViewport, state: TableState) {
    for (s in state.shockwaves) {
        val alpha = (s.life / s.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = Color(s.colorHex).copy(alpha = alpha * 0.65f),
            radius = s.radius * vp.scale,
            center = vp.worldToScreen(s.pos),
            style = Stroke(width = (8f * alpha + 2f) * vp.scale)
        )
    }
}

private fun DrawScope.drawParticles(vp: TableViewport, state: TableState) {
    for (p in state.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val pos = vp.worldToScreen(p.pos)
        val sz = p.size * vp.scale * alpha
        val color = Color(p.colorHex).copy(alpha = alpha)
        if (p.isShard) {
            rotate(degrees = p.rotation, pivot = pos) {
                drawRect(
                    color = color,
                    topLeft = Offset(pos.x - sz, pos.y - sz * 0.6f),
                    size = Size(sz * 2f, sz * 1.2f)
                )
            }
        } else {
            drawCircle(color = color, radius = sz, center = pos)
        }
    }
}

private fun DrawScope.drawFloatingTexts(
    vp: TableViewport,
    state: TableState,
    fillPaint: Paint,
    strokePaint: Paint
) {
    for (ft in state.floatingTexts) {
        val alpha = ((ft.life / ft.maxLife) * 1.4f).coerceIn(0f, 1f)
        val pos = vp.worldToScreen(ft.pos)
        val pxSize = 27f * ft.scale * vp.scale
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)

        strokePaint.textSize = pxSize
        strokePaint.strokeWidth = 6f * vp.scale
        strokePaint.alpha = alphaInt

        fillPaint.textSize = pxSize
        fillPaint.color = ft.colorHex.toInt()
        fillPaint.alpha = alphaInt

        drawContext.canvas.nativeCanvas.drawText(ft.text, pos.x, pos.y, strokePaint)
        drawContext.canvas.nativeCanvas.drawText(ft.text, pos.x, pos.y, fillPaint)
    }
}

private fun DrawScope.drawTiltBubble(vp: TableViewport, tiltInput: Vec2) {
    val center = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH - 96f, TableConstants.TABLE_HEIGHT - 96f))
    val r = 36f * vp.scale

    drawCircle(color = Color(0xFF0F172A).copy(alpha = 0.08f), radius = r, center = center)
    drawCircle(
        color = Color(0xFF64748B).copy(alpha = 0.45f),
        radius = r,
        center = center,
        style = Stroke(width = 2.5f * vp.scale)
    )
    // Crosshair
    drawLine(
        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
        start = Offset(center.x - r * 0.55f, center.y),
        end = Offset(center.x + r * 0.55f, center.y),
        strokeWidth = 1.8f * vp.scale
    )
    drawLine(
        color = Color(0xFF94A3B8).copy(alpha = 0.5f),
        start = Offset(center.x, center.y - r * 0.55f),
        end = Offset(center.x, center.y + r * 0.55f),
        strokeWidth = 1.8f * vp.scale
    )

    val dotOffset = Offset(
        x = (tiltInput.x.coerceIn(-1f, 1f)) * r * 0.72f,
        y = (tiltInput.y.coerceIn(-1f, 1f)) * r * 0.72f
    )
    val active = tiltInput.length() > 0.14f
    drawCircle(
        color = if (active) TabletopColors.Sky else Color(0xFF64748B),
        radius = 10f * vp.scale,
        center = center + dotOffset
    )
}

private fun DrawScope.drawShotBankedBanner(
    vp: TableViewport,
    state: TableState,
    fillPaint: Paint,
    strokePaint: Paint
) {
    val center = vp.worldToScreen(Vec2(TableConstants.TABLE_WIDTH * 0.5f, TableConstants.TABLE_HEIGHT * 0.48f))
    val boxW = 540f * vp.scale
    val boxH = 148f * vp.scale
    val topLeft = Offset(center.x - boxW * 0.5f, center.y - boxH * 0.5f)

    drawRoundRect(
        color = Color(0xFF0F172A).copy(alpha = 0.88f),
        topLeft = topLeft,
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(24f * vp.scale, 24f * vp.scale)
    )
    drawRoundRect(
        color = TabletopColors.Amber,
        topLeft = topLeft,
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(24f * vp.scale, 24f * vp.scale),
        style = Stroke(width = 4f * vp.scale)
    )

    val formula = "${state.lastBankedChips} × ${state.lastBankedMult}"
    val total = "+${state.lastBankedScore}"

    fillPaint.alpha = 255
    fillPaint.textSize = 32f * vp.scale
    fillPaint.color = 0xFFCBD5E1.toInt()
    drawContext.canvas.nativeCanvas.drawText(formula, center.x, center.y - 16f * vp.scale, fillPaint)

    fillPaint.textSize = 52f * vp.scale
    fillPaint.color = 0xFFFBBF24.toInt()
    strokePaint.textSize = 52f * vp.scale
    strokePaint.strokeWidth = 3f * vp.scale
    strokePaint.alpha = 120
    drawContext.canvas.nativeCanvas.drawText(total, center.x, center.y + 44f * vp.scale, fillPaint)
}

private fun DrawScope.drawLabel(
    center: Offset,
    text: String,
    textSizePx: Float,
    colorInt: Int,
    paint: Paint
) {
    paint.textSize = textSizePx
    paint.color = colorInt
    paint.alpha = 255
    val yOffset = -(paint.descent() + paint.ascent()) * 0.5f
    drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y + yOffset, paint)
}
