package com.roguehockey.game.engine

enum class RelicRarity(val label: String, val accentHex: Long) {
    COMMON("Common", 0xFF0EA5E9),
    UNCOMMON("Uncommon", 0xFF10B981),
    RARE("Rare", 0xFF8B5CF6)
}

enum class RelicId(
    val title: String,
    val badge: String,
    val description: String,
    val cost: Int,
    val rarity: RelicRarity
) {
    BANK_SHOT_PRO(
        title = "Bank Shot Pro",
        badge = "BK",
        description = "Every wall bounce adds +2 Mult to the current shot.",
        cost = 5,
        rarity = RelicRarity.COMMON
    ),
    ICEBREAKER(
        title = "Icebreaker",
        badge = "IB",
        description = "Shattering any obstacle grants +60 Chips and +1 Mult.",
        cost = 5,
        rarity = RelicRarity.COMMON
    ),
    TILT_MASTER(
        title = "Tilt Master",
        badge = "TM",
        description = "+60% tilt curve power. Scoring while tilting grants +25 Chips.",
        cost = 5,
        rarity = RelicRarity.COMMON
    ),
    PINBALL_WIZARD(
        title = "Pinball Wizard",
        badge = "PW",
        description = "5+ target hits in one shot multiplies Shot Mult by x1.5.",
        cost = 7,
        rarity = RelicRarity.UNCOMMON
    ),
    HEAVY_METAL_PUCK(
        title = "Tungsten Core",
        badge = "TC",
        description = "+35% puck mass and +1 bonus damage against all obstacles.",
        cost = 6,
        rarity = RelicRarity.COMMON
    ),
    AIR_CUSHION(
        title = "Turbo Air Jets",
        badge = "AJ",
        description = "35% lower table friction so pucks glide and chain much longer.",
        cost = 6,
        rarity = RelicRarity.UNCOMMON
    ),
    SPLIT_PERSONALITY(
        title = "Twin Launch",
        badge = "TW",
        description = "Your first flick each round launches an extra Twin Puck!",
        cost = 8,
        rarity = RelicRarity.RARE
    ),
    MIDAS_TOUCH(
        title = "Midas Bumper",
        badge = "MB",
        description = "Every 4th target hit in a shot awards +\$1 coin.",
        cost = 6,
        rarity = RelicRarity.UNCOMMON
    ),
    GOAL_POACHER(
        title = "Top Shelf",
        badge = "TS",
        description = "Scoring in the Top Goal Gate doubles your Shot Mult (x2)!",
        cost = 8,
        rarity = RelicRarity.RARE
    ),
    CHAIN_REACTION(
        title = "Cryo Blast",
        badge = "CB",
        description = "Cryo-barrels have +50% blast radius and grant +100 Chips.",
        cost = 6,
        rarity = RelicRarity.UNCOMMON
    ),
    COMBO_ENGINE(
        title = "Hot Streak",
        badge = "HS",
        description = "Rapid consecutive hits add escalating +15 Chips per combo step.",
        cost = 7,
        rarity = RelicRarity.UNCOMMON
    ),
    LUCKY_BOUNCE(
        title = "Echo Strike",
        badge = "ES",
        description = "30% chance any target hit triggers twice for double Chips & Mult.",
        cost = 8,
        rarity = RelicRarity.RARE
    )
}

enum class TableUpgradeId(
    val title: String,
    val badge: String,
    val description: String,
    val baseCost: Int
) {
    EXTRA_FLICK(
        title = "Extra Flick",
        badge = "+1",
        description = "Permanently gain +1 Flick per round (max 8).",
        baseCost = 7
    ),
    POLISH_BUMPERS(
        title = "Chrome Bumpers",
        badge = "+CH",
        description = "All Chip targets permanently award +20 base Chips.",
        baseCost = 4
    ),
    BOOST_MULT_TARGETS(
        title = "Prism Rings",
        badge = "+MU",
        description = "All Mult targets permanently award +1 extra Mult.",
        baseCost = 5
    ),
    EXTRA_POWERUP(
        title = "Supply Drop",
        badge = "+PU",
        description = "Spawn +1 extra Power-Up pickup on every table.",
        baseCost = 5
    ),
    POWER_LAUNCH(
        title = "Spring Wrist",
        badge = "+SP",
        description = "+15% max launch speed for explosive breaks.",
        baseCost = 4
    )
}

enum class BossModifier(
    val title: String,
    val description: String
) {
    NONE("Standard Table", "Clean frosted ice with balanced targets."),
    GLACIAL_WALL("Glacial Wall", "A fortified row of armored ice blocks guards the upper rink."),
    MAGNETIC_DRIFT("Crosswind Jets", "Alternating air currents push the puck sideways across mid-ice."),
    SPEED_TRAP("Narrow Goal", "Goal gate is narrower, while fast moving Jackpot stars patrol!"),
    HEAVY_ICE("Frost Drag", "Thicker frost slows the puck faster — tilt & strong flicks shine."),
    BUMPER_CHAOS("Hyper Bumpers", "Super-bouncy bumpers kick the puck at high speed for huge combos.")
}
