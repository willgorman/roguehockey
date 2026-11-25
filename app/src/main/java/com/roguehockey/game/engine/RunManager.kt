package com.roguehockey.game.engine

import kotlin.math.min
import kotlin.random.Random

enum class GameScreen {
    TITLE,
    PLAYING,
    ROUND_CLEAR,
    SHOP,
    GAME_OVER
}

sealed class ShopOffer {
    abstract val id: String
    abstract val title: String
    abstract val badge: String
    abstract val description: String
    abstract val cost: Int
    abstract var purchased: Boolean

    data class RelicOffer(
        override val id: String,
        val relic: RelicId,
        override var purchased: Boolean = false
    ) : ShopOffer() {
        override val title: String get() = relic.title
        override val badge: String get() = relic.badge
        override val description: String get() = relic.description
        override val cost: Int get() = relic.cost
    }

    data class UpgradeOffer(
        override val id: String,
        val upgrade: TableUpgradeId,
        override val cost: Int,
        override var purchased: Boolean = false
    ) : ShopOffer() {
        override val title: String get() = upgrade.title
        override val badge: String get() = upgrade.badge
        override val description: String get() = upgrade.description
    }
}

data class RoundClearSummary(
    val ante: Int,
    val roundInAnte: Int,
    val finalScore: Int,
    val targetScore: Int,
    val baseReward: Int,
    val unusedFlicks: Int,
    val flickBonus: Int,
    val interestBonus: Int,
    val totalPayout: Int
)

data class RunStats(
    var totalScoreAllRounds: Long = 0L,
    var highestShotScore: Int = 0,
    var roundsCleared: Int = 0,
    var obstaclesDestroyed: Int = 0,
    var goalsScored: Int = 0
)

class RunManager(
    val physics: PhysicsEngine = PhysicsEngine(),
    private val random: Random = Random.Default
) {
    var screen: GameScreen = GameScreen.TITLE
        private set

    var ante: Int = 1
        private set
    var roundInAnte: Int = 1
        private set
    var roundScore: Int = 0
        private set
    var coins: Int = 4
        private set
    var flicksRemaining: Int = 5
        private set

    var modifiers: RunModifiers = RunModifiers()
        private set

    var tableState: TableState = TableGenerator.generateTable(1, 1, modifiers, random = random)
        private set

    var shopOffers: MutableList<ShopOffer> = mutableListOf()
        private set
    var shopRerollCost: Int = 3
        private set
    var nextRoundBossPreview: BossModifier = BossModifier.NONE
        private set

    var roundClearSummary: RoundClearSummary? = null
        private set
    var stats: RunStats = RunStats()
        private set

    var controlMode: ControlMode = ControlMode.AUTO
    var tiltSensitivity: TiltSensitivity = TiltSensitivity.MEDIUM

    fun startNewRun() {
        ante = 1
        roundInAnte = 1
        roundScore = 0
        coins = 4
        modifiers = RunModifiers()
        flicksRemaining = modifiers.maxFlicks
        stats = RunStats()
        roundClearSummary = null
        shopOffers.clear()
        tableState = TableGenerator.generateTable(ante, roundInAnte, modifiers, random = random)
        screen = GameScreen.PLAYING
    }

    fun launchShot(velocity: Vec2): List<SoundEvent> {
        if (screen != GameScreen.PLAYING) return emptyList()
        if (tableState.shotPhase != ShotPhase.AIMING || flicksRemaining <= 0) return emptyList()

        val events = physics.launchPuck(tableState, velocity, modifiers)
        if (events.isNotEmpty()) {
            flicksRemaining--
        }
        return events
    }

    fun recallPucksEarly(): List<SoundEvent> {
        if (screen != GameScreen.PLAYING || tableState.shotPhase != ShotPhase.IN_PLAY) return emptyList()
        val result = physics.recallPucksEarly(tableState)
        return handleShotCompletion(result)
    }

    fun tick(dt: Float, tiltInput: Vec2): List<SoundEvent> {
        if (screen != GameScreen.PLAYING) return emptyList()
        val result = physics.step(tableState, dt, tiltInput, tiltSensitivity, modifiers)
        return handleShotCompletion(result)
    }

    private fun handleShotCompletion(result: StepResult): List<SoundEvent> {
        val events = result.soundEvents.toMutableList()

        val banked = result.shotBankedScore
        if (banked != null) {
            roundScore += banked
            coins += result.shotBankedCoins
            stats.totalScoreAllRounds += banked
            if (banked > stats.highestShotScore) stats.highestShotScore = banked
            stats.obstaclesDestroyed += tableState.shotScore.obstaclesBroken
            stats.goalsScored += tableState.shotScore.goalsScored

            if (roundScore >= tableState.targetScore) {
                completeRound()
                events.add(SoundEvent.ROUND_CLEAR)
            } else if (flicksRemaining <= 0) {
                screen = GameScreen.GAME_OVER
                events.add(SoundEvent.GAME_OVER)
            }
        }
        return events
    }

    private fun completeRound() {
        stats.roundsCleared++
        val baseReward = 3 + roundInAnte + (ante - 1)
        val flickBonus = flicksRemaining * 1
        val interestBonus = min(5, coins / 5)
        val totalPayout = baseReward + flickBonus + interestBonus

        coins += totalPayout
        roundClearSummary = RoundClearSummary(
            ante = ante,
            roundInAnte = roundInAnte,
            finalScore = roundScore,
            targetScore = tableState.targetScore,
            baseReward = baseReward,
            unusedFlicks = flicksRemaining,
            flickBonus = flickBonus,
            interestBonus = interestBonus,
            totalPayout = totalPayout
        )

        // Pre-compute upcoming round's boss modifier so Shop can preview it
        val (nextAnte, nextRound) = computeNextRoundIndex()
        nextRoundBossPreview = TableGenerator.bossModifierFor(nextAnte, nextRound, random)
        screen = GameScreen.ROUND_CLEAR
    }

    fun openShop() {
        if (screen != GameScreen.ROUND_CLEAR) return
        shopRerollCost = 3
        rollShopOffers()
        screen = GameScreen.SHOP
    }

    fun rollShopOffers() {
        shopOffers.clear()
        val owned = modifiers.relics.toSet()
        val availableRelics = RelicId.entries.filter { it !in owned }.shuffled(random)
        availableRelics.take(2).forEachIndexed { i, relic ->
            shopOffers.add(ShopOffer.RelicOffer(id = "relic_${relic.name}_$i", relic = relic))
        }

        val upgrades = TableUpgradeId.entries
            .filter { !(it == TableUpgradeId.EXTRA_FLICK && modifiers.maxFlicks >= 8) }
            .shuffled(random)
        upgrades.take(2).forEachIndexed { i, upg ->
            val scaledCost = upg.baseCost + (ante - 1)
            shopOffers.add(
                ShopOffer.UpgradeOffer(
                    id = "upg_${upg.name}_$i",
                    upgrade = upg,
                    cost = scaledCost
                )
            )
        }
    }

    fun rerollShop(): Boolean {
        if (screen != GameScreen.SHOP || coins < shopRerollCost) return false
        coins -= shopRerollCost
        shopRerollCost += 2
        rollShopOffers()
        return true
    }

    fun buyOffer(offerId: String): Boolean {
        if (screen != GameScreen.SHOP) return false
        val offer = shopOffers.firstOrNull { it.id == offerId } ?: return false
        if (offer.purchased || coins < offer.cost) return false

        when (offer) {
            is ShopOffer.RelicOffer -> {
                if (modifiers.relics.size >= TableConstants.MAX_RELIC_SLOTS) return false
                coins -= offer.cost
                offer.purchased = true
                modifiers = modifiers.copy(relics = modifiers.relics + offer.relic)
            }
            is ShopOffer.UpgradeOffer -> {
                coins -= offer.cost
                offer.purchased = true
                modifiers = when (offer.upgrade) {
                    TableUpgradeId.EXTRA_FLICK ->
                        modifiers.copy(maxFlicks = min(8, modifiers.maxFlicks + 1))
                    TableUpgradeId.POLISH_BUMPERS ->
                        modifiers.copy(chipBonusUpgrade = modifiers.chipBonusUpgrade + 20)
                    TableUpgradeId.BOOST_MULT_TARGETS ->
                        modifiers.copy(multBonusUpgrade = modifiers.multBonusUpgrade + 1)
                    TableUpgradeId.EXTRA_POWERUP ->
                        modifiers.copy(extraPowerUps = min(4, modifiers.extraPowerUps + 1))
                    TableUpgradeId.POWER_LAUNCH ->
                        modifiers.copy(launchSpeedMultiplier = modifiers.launchSpeedMultiplier + 0.15f)
                }
            }
        }
        return true
    }

    fun sellRelic(relic: RelicId): Boolean {
        if (screen != GameScreen.SHOP || relic !in modifiers.relics) return false
        val refund = (relic.cost / 2).coerceAtLeast(2)
        coins += refund
        modifiers = modifiers.copy(relics = modifiers.relics - relic)
        return true
    }

    fun startNextRound() {
        val (nextAnte, nextRound) = computeNextRoundIndex()
        ante = nextAnte
        roundInAnte = nextRound
        roundScore = 0
        flicksRemaining = modifiers.maxFlicks
        roundClearSummary = null
        tableState = TableGenerator.generateTable(
            ante = ante,
            roundInAnte = roundInAnte,
            modifiers = modifiers,
            forcedBoss = nextRoundBossPreview,
            random = random
        )
        screen = GameScreen.PLAYING
    }

    fun returnToTitle() {
        screen = GameScreen.TITLE
    }

    fun nextRoundTargetPreview(): Int {
        val (nextAnte, nextRound) = computeNextRoundIndex()
        return TableGenerator.targetScoreFor(nextAnte, nextRound)
    }

    fun computeNextRoundIndex(): Pair<Int, Int> {
        return if (roundInAnte < 3) {
            ante to (roundInAnte + 1)
        } else {
            (ante + 1) to 1
        }
    }
}
