package com.roguehockey.game.engine

object TableConstants {
    const val TABLE_WIDTH = 1000f
    const val TABLE_HEIGHT = 1520f
    const val WALL_MARGIN = 32f
    const val LAUNCH_LINE_Y = 1190f
    const val DEFAULT_PUCK_X = TABLE_WIDTH / 2f
    const val DEFAULT_PUCK_Y = 1315f
    const val DEFAULT_PUCK_RADIUS = 34f
    const val MAX_LAUNCH_SPEED = 1850f
    const val MIN_LAUNCH_SPEED = 240f
    const val STOP_SPEED_THRESHOLD = 38f
    const val MAX_RELIC_SLOTS = 5
}

enum class TargetType {
    CHIP_BUMPER,
    MULT_BUMPER,
    SPINNER_RING,
    JACKPOT_STAR
}

enum class ObstacleType {
    ICE_BLOCK,
    STEEL_CRATE,
    CRYO_BARREL
}

enum class PowerUpType(val label: String, val shortTag: String) {
    MULTI_PUCK("Multi-Puck", "x2 PUCK"),
    MAGNET_PULL("Target Magnet", "MAGNET"),
    OVERDRIVE_FIRE("Overdrive", "FIRE"),
    GIANT_PUCK("Giant Puck", "GIANT"),
    SCORE_SURGE("Score Surge", "x2 MULT")
}

enum class ControlMode(val label: String) {
    AUTO("Auto"),
    FLICK("Flick"),
    SLINGSHOT("Slingshot")
}

enum class TiltSensitivity(val label: String, val accelMultiplier: Float) {
    OFF("Off", 0f),
    LOW("Low", 420f),
    MEDIUM("Med", 760f),
    HIGH("High", 1120f)
}

enum class SoundEvent {
    FLICK_LAUNCH,
    WALL_BOUNCE,
    CHIP_HIT,
    MULT_HIT,
    SPINNER_HIT,
    JACKPOT_HIT,
    OBSTACLE_CRACK,
    OBSTACLE_SHATTER,
    BARREL_EXPLODE,
    POWERUP_PICKUP,
    GOAL_SCORE,
    SHOT_BANKED,
    ROUND_CLEAR,
    SHOP_BUY,
    GAME_OVER
}

data class Puck(
    val id: Int,
    var pos: Vec2,
    var vel: Vec2 = Vec2.ZERO,
    var baseRadius: Float = TableConstants.DEFAULT_PUCK_RADIUS,
    var mass: Float = 1f,
    val isMainPuck: Boolean = true,
    var overdriveTimer: Float = 0f,
    var giantTimer: Float = 0f,
    var magnetTimer: Float = 0f,
    var goalCooldown: Float = 0f,
    var slowTimer: Float = 0f,
    val trail: MutableList<Vec2> = mutableListOf()
) {
    val radius: Float
        get() = if (giantTimer > 0f) baseRadius * 1.48f else baseRadius

    val effectiveMass: Float
        get() = if (giantTimer > 0f) mass * 1.8f else mass

    val isOverdrive: Boolean get() = overdriveTimer > 0f
    val isGiant: Boolean get() = giantTimer > 0f
    val isMagnet: Boolean get() = magnetTimer > 0f
}

data class Target(
    val id: Int,
    val type: TargetType,
    var pos: Vec2,
    val radius: Float,
    val chipValue: Int,
    val multValue: Int = 0,
    val coinValue: Int = 0,
    val restitution: Float = 1.08f,
    val patrolCenter: Vec2 = pos,
    val patrolAxis: Vec2 = Vec2.ZERO,
    val patrolAmplitude: Float = 0f,
    val patrolSpeed: Float = 0f,
    var patrolPhase: Float = 0f,
    var hitCooldown: Float = 0f,
    var hitFlash: Float = 0f,
    var hitCount: Int = 0,
    var spinAngle: Float = 0f
)

data class Obstacle(
    val id: Int,
    val type: ObstacleType,
    val pos: Vec2,
    val halfW: Float,
    val halfH: Float,
    var hp: Int,
    val maxHp: Int,
    val chipBonus: Int,
    val dropsCoin: Int = 0,
    val dropsPowerUp: PowerUpType? = null,
    var hitFlash: Float = 0f,
    var destroyed: Boolean = false
) {
    val isCircular: Boolean get() = type == ObstacleType.CRYO_BARREL
    val radius: Float get() = halfW
}

data class PowerUpPickup(
    val id: Int,
    val type: PowerUpType,
    val pos: Vec2,
    val radius: Float = 30f,
    var pulsePhase: Float = 0f,
    var collected: Boolean = false
)

data class GoalGate(
    val xMin: Float,
    val xMax: Float,
    val yDepth: Float = TableConstants.WALL_MARGIN + 22f,
    val chipReward: Int = 120,
    val multReward: Int = 2,
    var flashTimer: Float = 0f
) {
    val centerX: Float get() = (xMin + xMax) * 0.5f
    val width: Float get() = xMax - xMin
}

data class Particle(
    var pos: Vec2,
    var vel: Vec2,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val colorHex: Long,
    val isShard: Boolean = false,
    var rotation: Float = 0f,
    val rotSpeed: Float = 0f
)

data class ShockwaveRing(
    val pos: Vec2,
    var radius: Float,
    val maxRadius: Float,
    var life: Float,
    val maxLife: Float,
    val colorHex: Long
)

data class FloatingText(
    var pos: Vec2,
    val text: String,
    val colorHex: Long,
    var life: Float,
    val maxLife: Float,
    val scale: Float = 1f
)

data class ShotScoreState(
    var chips: Int = 0,
    var mult: Int = 1,
    var comboCount: Int = 0,
    var targetsHit: Int = 0,
    var wallBounces: Int = 0,
    var obstaclesBroken: Int = 0,
    var goalsScored: Int = 0,
    var coinsEarnedThisShot: Int = 0,
    var lastHitElapsed: Float = 99f,
    var shotElapsed: Float = 0f,
    var pinballWizardTriggered: Boolean = false
) {
    val previewTotal: Int get() = if (chips <= 0) 0 else chips * mult

    fun reset() {
        chips = 0
        mult = 1
        comboCount = 0
        targetsHit = 0
        wallBounces = 0
        obstaclesBroken = 0
        goalsScored = 0
        coinsEarnedThisShot = 0
        lastHitElapsed = 99f
        shotElapsed = 0f
        pinballWizardTriggered = false
    }
}

enum class ShotPhase {
    AIMING,
    IN_PLAY,
    SCORING_POPUP
}

data class TableState(
    val ante: Int,
    val roundInAnte: Int,
    val bossModifier: BossModifier,
    val targetScore: Int,
    val goalGate: GoalGate,
    val pucks: MutableList<Puck>,
    val targets: MutableList<Target>,
    val obstacles: MutableList<Obstacle>,
    val powerUps: MutableList<PowerUpPickup>,
    val particles: MutableList<Particle> = mutableListOf(),
    val shockwaves: MutableList<ShockwaveRing> = mutableListOf(),
    val floatingTexts: MutableList<FloatingText> = mutableListOf(),
    val shotScore: ShotScoreState = ShotScoreState(),
    var shotPhase: ShotPhase = ShotPhase.AIMING,
    var scoringPopupTimer: Float = 0f,
    var lastBankedScore: Int = 0,
    var lastBankedChips: Int = 0,
    var lastBankedMult: Int = 1,
    var screenShake: Float = 0f,
    var tableTime: Float = 0f,
    var currentWindX: Float = 0f,
    var firstFlickUsed: Boolean = false,
    var nextEntityId: Int = 1000
) {
    fun allocId(): Int = nextEntityId++
}
