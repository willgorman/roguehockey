package com.roguehockey.game

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.roguehockey.game.engine.ControlMode
import com.roguehockey.game.engine.GameScreen
import com.roguehockey.game.engine.RunManager
import com.roguehockey.game.engine.SoundEvent
import com.roguehockey.game.engine.TiltSensitivity
import com.roguehockey.game.engine.Vec2
import com.roguehockey.game.platform.SoundEngine
import com.roguehockey.game.platform.TiltSensorController
import com.roguehockey.game.ui.BottomControlBar
import com.roguehockey.game.ui.GameOverScreen
import com.roguehockey.game.ui.RoundClearOverlay
import com.roguehockey.game.ui.ShopScreen
import com.roguehockey.game.ui.TableCanvas
import com.roguehockey.game.ui.TitleScreen
import com.roguehockey.game.ui.TopGameHud
import com.roguehockey.game.ui.theme.RogueHockeyTheme
import com.roguehockey.game.ui.theme.TabletopColors

class MainActivity : ComponentActivity() {

    private lateinit var tiltController: TiltSensorController
    private lateinit var soundEngine: SoundEngine
    private val runManager = RunManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tiltController = TiltSensorController(this)
        soundEngine = SoundEngine(this)

        setContent {
            RogueHockeyTheme {
                RogueHockeyApp(
                    runManager = runManager,
                    tiltController = tiltController,
                    soundEngine = soundEngine
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tiltController.start()
    }

    override fun onPause() {
        super.onPause()
        tiltController.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEngine.release()
    }
}

@Composable
private fun RogueHockeyApp(
    runManager: RunManager,
    tiltController: TiltSensorController,
    soundEngine: SoundEngine
) {
    val context = LocalContext.current

    var frameCounter by remember { mutableLongStateOf(0L) }
    var uiRevision by remember { mutableLongStateOf(0L) }
    var muted by remember { mutableStateOf(false) }
    var liveTilt by remember { mutableStateOf(Vec2.ZERO) }

    fun emitSounds(events: List<SoundEvent>) {
        val combo = runManager.tableState.shotScore.comboCount.coerceAtLeast(1)
        for (ev in events) {
            soundEngine.play(ev, comboStep = combo)
        }
    }

    // 60-120Hz Choreographer-synced game loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                val dt = if (lastNanos == 0L) 1f / 60f else ((nowNanos - lastNanos) / 1_000_000_000f)
                lastNanos = nowNanos

                val tilt = tiltController.currentTilt()
                liveTilt = tilt

                if (runManager.screen == GameScreen.PLAYING) {
                    val prevScreen = runManager.screen
                    val events = runManager.tick(dt, tilt)
                    if (events.isNotEmpty()) emitSounds(events)
                    if (runManager.screen != prevScreen) {
                        uiRevision++
                    }
                    frameCounter++
                }
            }
        }
    }

    // Read uiRevision so screen/shop mutations recompose reliably
    val rev = uiRevision

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TabletopColors.ChassisDark)
            .safeDrawingPadding()
    ) {
        when (runManager.screen) {
            GameScreen.TITLE -> {
                TitleScreen(
                    tiltAvailable = tiltController.isAvailable,
                    onStartRun = {
                        tiltController.calibrateNeutral()
                        runManager.startNewRun()
                        soundEngine.play(SoundEvent.FLICK_LAUNCH)
                        uiRevision++
                    }
                )
            }

            GameScreen.PLAYING, GameScreen.ROUND_CLEAR, GameScreen.GAME_OVER -> {
                // Keep table rendered behind Round Clear / Game Over overlays
                val table = runManager.tableState
                Column(modifier = Modifier.fillMaxSize()) {
                    TopGameHud(
                        ante = runManager.ante,
                        roundInAnte = runManager.roundInAnte,
                        bossModifier = table.bossModifier,
                        roundScore = runManager.roundScore,
                        targetScore = table.targetScore,
                        coins = runManager.coins,
                        flicksRemaining = runManager.flicksRemaining,
                        maxFlicks = runManager.modifiers.maxFlicks,
                        shotScore = table.shotScore,
                        relics = runManager.modifiers.relics,
                        muted = muted,
                        onToggleMute = {
                            muted = !muted
                            soundEngine.muted = muted
                        },
                        onCalibrateTilt = {
                            tiltController.calibrateNeutral()
                            Toast.makeText(context, "Tilt neutral angle calibrated!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    TableCanvas(
                        tableState = table,
                        frameCounter = frameCounter + rev,
                        physics = runManager.physics,
                        controlMode = runManager.controlMode,
                        tiltInput = liveTilt,
                        tiltSensitivity = runManager.tiltSensitivity,
                        launchSpeedMultiplier = runManager.modifiers.launchSpeedMultiplier,
                        onLaunchPuck = { vel ->
                            val events = runManager.launchShot(vel)
                            emitSounds(events)
                            uiRevision++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    BottomControlBar(
                        controlMode = runManager.controlMode,
                        tiltSensitivity = runManager.tiltSensitivity,
                        shotPhase = table.shotPhase,
                        onCycleControlMode = {
                            val all = ControlMode.entries
                            runManager.controlMode = all[(all.indexOf(runManager.controlMode) + 1) % all.size]
                            uiRevision++
                        },
                        onCycleTiltSensitivity = {
                            val all = TiltSensitivity.entries
                            runManager.tiltSensitivity = all[(all.indexOf(runManager.tiltSensitivity) + 1) % all.size]
                            uiRevision++
                        },
                        onRecallPuck = {
                            val events = runManager.recallPucksEarly()
                            emitSounds(events)
                            uiRevision++
                        }
                    )
                }

                if (runManager.screen == GameScreen.ROUND_CLEAR) {
                    runManager.roundClearSummary?.let { summary ->
                        RoundClearOverlay(
                            summary = summary,
                            onOpenShop = {
                                runManager.openShop()
                                uiRevision++
                            }
                        )
                    }
                } else if (runManager.screen == GameScreen.GAME_OVER) {
                    GameOverScreen(
                        anteReached = runManager.ante,
                        roundReached = runManager.roundInAnte,
                        finalRoundScore = runManager.roundScore,
                        targetScore = table.targetScore,
                        stats = runManager.stats,
                        relics = runManager.modifiers.relics,
                        onNewRun = {
                            tiltController.calibrateNeutral()
                            runManager.startNewRun()
                            soundEngine.play(SoundEvent.FLICK_LAUNCH)
                            uiRevision++
                        },
                        onTitle = {
                            runManager.returnToTitle()
                            uiRevision++
                        }
                    )
                }
            }

            GameScreen.SHOP -> {
                val (nextAnte, nextRound) = runManager.computeNextRoundIndex()
                ShopScreen(
                    coins = runManager.coins,
                    nextAnte = nextAnte,
                    nextRoundInAnte = nextRound,
                    nextTargetScore = runManager.nextRoundTargetPreview(),
                    nextBossModifier = runManager.nextRoundBossPreview,
                    modifiers = runManager.modifiers,
                    offers = runManager.shopOffers.toList(),
                    rerollCost = runManager.shopRerollCost,
                    onBuyOffer = { id ->
                        if (runManager.buyOffer(id)) {
                            soundEngine.play(SoundEvent.SHOP_BUY)
                            uiRevision++
                        }
                    },
                    onSellRelic = { relic ->
                        if (runManager.sellRelic(relic)) {
                            soundEngine.play(SoundEvent.SHOP_BUY)
                            uiRevision++
                        }
                    },
                    onReroll = {
                        if (runManager.rerollShop()) {
                            soundEngine.play(SoundEvent.SPINNER_HIT)
                            uiRevision++
                        }
                    },
                    onNextRound = {
                        runManager.startNextRound()
                        soundEngine.play(SoundEvent.FLICK_LAUNCH)
                        uiRevision++
                    }
                )
            }
        }
    }
}
