// Pure JS RogueHockey Simulation & Rogue-Lite Progression Engine (1:1 parity with Android Kotlin engine)

export const TableConstants = Object.freeze({
  TABLE_WIDTH: 1000,
  TABLE_HEIGHT: 1520,
  WALL_MARGIN: 32,
  LAUNCH_LINE_Y: 1190,
  DEFAULT_PUCK_X: 500,
  DEFAULT_PUCK_Y: 1315,
  DEFAULT_PUCK_RADIUS: 34,
  MAX_LAUNCH_SPEED: 1850,
  MIN_LAUNCH_SPEED: 240,
  STOP_SPEED_THRESHOLD: 38,
  MAX_RELIC_SLOTS: 5
});

export const ControlModes = ["AUTO", "FLICK", "SLINGSHOT"];

export const TiltSensitivities = [
  { id: "OFF", label: "Off", accelMultiplier: 0 },
  { id: "LOW", label: "Low", accelMultiplier: 420 },
  { id: "MEDIUM", label: "Med", accelMultiplier: 760 },
  { id: "HIGH", label: "High", accelMultiplier: 1120 }
];

export const RelicRarity = Object.freeze({
  COMMON: { label: "Common", accent: "#0284C7" },
  UNCOMMON: { label: "Uncommon", accent: "#059669" },
  RARE: { label: "Rare", accent: "#D97706" }
});

export const RELICS = Object.freeze({
  BANK_SHOT_PRO: {
    id: "BANK_SHOT_PRO",
    title: "Bank Shot Pro",
    badge: "BK",
    description: "Every wall bounce adds +2 Mult to the current shot.",
    cost: 5,
    rarity: RelicRarity.COMMON
  },
  ICEBREAKER: {
    id: "ICEBREAKER",
    title: "Icebreaker",
    badge: "IB",
    description: "Shattering any obstacle grants +60 Chips and +1 Mult.",
    cost: 5,
    rarity: RelicRarity.COMMON
  },
  TILT_MASTER: {
    id: "TILT_MASTER",
    title: "Tilt Master",
    badge: "TM",
    description: "+60% tilt curve power. Scoring while tilting grants +25 Chips.",
    cost: 5,
    rarity: RelicRarity.COMMON
  },
  PINBALL_WIZARD: {
    id: "PINBALL_WIZARD",
    title: "Pinball Wizard",
    badge: "PW",
    description: "5+ target hits in one shot multiplies Shot Mult by x1.5.",
    cost: 7,
    rarity: RelicRarity.UNCOMMON
  },
  HEAVY_METAL_PUCK: {
    id: "HEAVY_METAL_PUCK",
    title: "Tungsten Core",
    badge: "TC",
    description: "+35% puck mass and +1 bonus damage against all obstacles.",
    cost: 6,
    rarity: RelicRarity.COMMON
  },
  AIR_CUSHION: {
    id: "AIR_CUSHION",
    title: "Turbo Air Jets",
    badge: "AJ",
    description: "35% lower table friction so pucks glide and chain much longer.",
    cost: 6,
    rarity: RelicRarity.UNCOMMON
  },
  SPLIT_PERSONALITY: {
    id: "SPLIT_PERSONALITY",
    title: "Twin Launch",
    badge: "TW",
    description: "Your first flick each round launches an extra Twin Puck!",
    cost: 8,
    rarity: RelicRarity.RARE
  },
  MIDAS_TOUCH: {
    id: "MIDAS_TOUCH",
    title: "Midas Bumper",
    badge: "MB",
    description: "Every 4th target hit in a shot awards +$1 coin.",
    cost: 6,
    rarity: RelicRarity.UNCOMMON
  },
  GOAL_POACHER: {
    id: "GOAL_POACHER",
    title: "Top Shelf",
    badge: "TS",
    description: "Scoring in the Top Goal Gate doubles your Shot Mult (x2)!",
    cost: 8,
    rarity: RelicRarity.RARE
  },
  CHAIN_REACTION: {
    id: "CHAIN_REACTION",
    title: "Cryo Blast",
    badge: "CB",
    description: "Cryo-barrels have +50% blast radius and grant +100 Chips.",
    cost: 6,
    rarity: RelicRarity.UNCOMMON
  },
  COMBO_ENGINE: {
    id: "COMBO_ENGINE",
    title: "Hot Streak",
    badge: "HS",
    description: "Rapid consecutive hits add escalating +15 Chips per combo step.",
    cost: 7,
    rarity: RelicRarity.UNCOMMON
  },
  LUCKY_BOUNCE: {
    id: "LUCKY_BOUNCE",
    title: "Echo Strike",
    badge: "ES",
    description: "30% chance any target hit triggers twice for double Chips & Mult.",
    cost: 8,
    rarity: RelicRarity.RARE
  }
});

export const TABLE_UPGRADES = Object.freeze({
  EXTRA_FLICK: {
    id: "EXTRA_FLICK",
    title: "Extra Flick",
    badge: "+1",
    description: "Permanently gain +1 Flick per round (max 8).",
    baseCost: 7
  },
  POLISH_BUMPERS: {
    id: "POLISH_BUMPERS",
    title: "Chrome Bumpers",
    badge: "+CH",
    description: "All Chip targets permanently award +20 base Chips.",
    baseCost: 4
  },
  BOOST_MULT_TARGETS: {
    id: "BOOST_MULT_TARGETS",
    title: "Prism Rings",
    badge: "+MU",
    description: "All Mult targets permanently award +1 extra Mult.",
    baseCost: 5
  },
  EXTRA_POWERUP: {
    id: "EXTRA_POWERUP",
    title: "Supply Drop",
    badge: "+PU",
    description: "Spawn +1 extra Power-Up pickup on every table.",
    baseCost: 5
  },
  POWER_LAUNCH: {
    id: "POWER_LAUNCH",
    title: "Spring Wrist",
    badge: "+SP",
    description: "+15% max launch speed for explosive breaks.",
    baseCost: 4
  }
});

export const BOSS_MODIFIERS = Object.freeze({
  NONE: {
    id: "NONE",
    title: "Standard Table",
    description: "Clean frosted ice with balanced targets."
  },
  GLACIAL_WALL: {
    id: "GLACIAL_WALL",
    title: "Glacial Wall",
    description: "A fortified row of armored ice blocks guards the upper rink."
  },
  MAGNETIC_DRIFT: {
    id: "MAGNETIC_DRIFT",
    title: "Crosswind Jets",
    description: "Alternating air currents push the puck sideways across mid-ice."
  },
  SPEED_TRAP: {
    id: "SPEED_TRAP",
    title: "Narrow Goal",
    description: "Goal gate is narrower, while fast moving Jackpot stars patrol!"
  },
  HEAVY_ICE: {
    id: "HEAVY_ICE",
    title: "Frost Drag",
    description: "Thicker frost slows the puck faster — tilt & strong flicks shine."
  },
  BUMPER_CHAOS: {
    id: "BUMPER_CHAOS",
    title: "Hyper Bumpers",
    description: "Super-bouncy bumpers kick the puck at high speed for huge combos."
  }
});

export const POWERUP_TYPES = Object.freeze([
  { id: "MULTI_PUCK", label: "Multi-Puck", shortTag: "x2 PUCK", icon: "x2" },
  { id: "MAGNET_PULL", label: "Target Magnet", shortTag: "MAGNET", icon: "MAG" },
  { id: "OVERDRIVE_FIRE", label: "Overdrive", shortTag: "FIRE", icon: "FIRE" },
  { id: "GIANT_PUCK", label: "Giant Puck", shortTag: "GIANT", icon: "BIG" },
  { id: "SCORE_SURGE", label: "Score Surge", shortTag: "x2 MULT", icon: "2xM" }
]);

// Deterministic PRNG option for tests
export function createRng(seed = null) {
  if (seed === null || seed === undefined) {
    return () => Math.random();
  }
  let s = seed >>> 0;
  return () => {
    s = (s * 1664525 + 1013904223) >>> 0;
    return s / 4294967296;
  };
}

function shuffle(arr, rng) {
  const out = arr.slice();
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

export const Vec = {
  add: (a, b) => ({ x: a.x + b.x, y: a.y + b.y }),
  sub: (a, b) => ({ x: a.x - b.x, y: a.y - b.y }),
  mul: (a, s) => ({ x: a.x * s, y: a.y * s }),
  dot: (a, b) => a.x * b.x + a.y * b.y,
  lenSq: (a) => a.x * a.x + a.y * a.y,
  len: (a) => Math.hypot(a.x, a.y),
  dist: (a, b) => Math.hypot(a.x - b.x, a.y - b.y),
  norm(a) {
    const l = Math.hypot(a.x, a.y);
    return l > 1e-5 ? { x: a.x / l, y: a.y / l } : { x: 0, y: 0 };
  },
  clampLen(a, maxLen) {
    const lSq = a.x * a.x + a.y * a.y;
    if (lSq > maxLen * maxLen && lSq > 1e-6) {
      const s = maxLen / Math.sqrt(lSq);
      return { x: a.x * s, y: a.y * s };
    }
    return a;
  },
  reflect(v, n) {
    const d = 2 * (v.x * n.x + v.y * n.y);
    return { x: v.x - d * n.x, y: v.y - d * n.y };
  }
};

export function createShotScore() {
  return {
    chips: 0,
    mult: 1,
    comboCount: 0,
    targetsHit: 0,
    wallBounces: 0,
    obstaclesBroken: 0,
    goalsScored: 0,
    coinsEarnedThisShot: 0,
    lastHitElapsed: 99,
    shotElapsed: 0,
    pinballWizardTriggered: false,
    get previewTotal() {
      return this.chips <= 0 ? 0 : this.chips * this.mult;
    }
  };
}

export function createPuck(id, pos, vel = { x: 0, y: 0 }, mass = 1, isMainPuck = true, baseRadius = TableConstants.DEFAULT_PUCK_RADIUS) {
  return {
    id,
    pos: { x: pos.x, y: pos.y },
    vel: { x: vel.x, y: vel.y },
    baseRadius,
    mass,
    isMainPuck,
    overdriveTimer: 0,
    giantTimer: 0,
    magnetTimer: 0,
    goalCooldown: 0,
    slowTimer: 0,
    trail: [],
    get radius() {
      return this.giantTimer > 0 ? this.baseRadius * 1.48 : this.baseRadius;
    },
    get effectiveMass() {
      return this.giantTimer > 0 ? this.mass * 1.8 : this.mass;
    },
    get isOverdrive() {
      return this.overdriveTimer > 0;
    },
    get isGiant() {
      return this.giantTimer > 0;
    },
    get isMagnet() {
      return this.magnetTimer > 0;
    }
  };
}

export function targetScoreFor(ante, roundInAnte) {
  const baseAnte = 450 * Math.pow(2.55, ante - 1);
  const roundFactor = roundInAnte === 1 ? 1.0 : roundInAnte === 2 ? 1.45 : 2.0;
  const raw = Math.round(baseAnte * roundFactor);
  return Math.floor((raw + 25) / 50) * 50;
}

export function bossModifierFor(ante, roundInAnte, rng = Math.random) {
  if (roundInAnte < 3) return BOSS_MODIFIERS.NONE;
  const pool = Object.values(BOSS_MODIFIERS).filter((m) => m.id !== "NONE");
  return pool[(ante - 1 + Math.floor(rng() * pool.length)) % pool.length];
}

export function generateTable(ante, roundInAnte, modifiers, forcedBoss = null, rng = Math.random) {
  const bossMod = forcedBoss || bossModifierFor(ante, roundInAnte, rng);
  const targetScore = targetScoreFor(ante, roundInAnte);
  const hasRelic = (id) => modifiers.relics.includes(id);

  const goalWidth = bossMod.id === "SPEED_TRAP" ? 185 : 255;
  const goalHalf = goalWidth * 0.5;
  const goalGate = {
    xMin: TableConstants.TABLE_WIDTH * 0.5 - goalHalf,
    xMax: TableConstants.TABLE_WIDTH * 0.5 + goalHalf,
    width: goalWidth,
    chipReward: 130 + ante * 25 + modifiers.chipBonusUpgrade,
    multReward: 2 + modifiers.multBonusUpgrade,
    flashTimer: 0
  };

  const targets = [];
  const obstacles = [];
  const powerUps = [];
  let nextId = 100;

  const chipBase = 35 + (ante - 1) * 8 + modifiers.chipBonusUpgrade;
  const multBase = 1 + modifiers.multBonusUpgrade;
  const bumperRestitution = bossMod.id === "BUMPER_CHAOS" ? 1.32 : 1.1;

  const randomPowerUpType = () => POWERUP_TYPES[Math.floor(rng() * POWERUP_TYPES.length)].id;

  const addTarget = (type, x, y, patrolAxis = { x: 0, y: 0 }, patrolAmp = 0, patrolSpeed = 0) => {
    const radius = type === "CHIP_BUMPER" ? 38 : type === "MULT_BUMPER" ? 36 : type === "SPINNER_RING" ? 44 : 40;
    const chipValue =
      type === "CHIP_BUMPER"
        ? chipBase
        : type === "MULT_BUMPER"
        ? 15 + Math.floor(modifiers.chipBonusUpgrade / 2)
        : type === "SPINNER_RING"
        ? 25 + Math.floor(modifiers.chipBonusUpgrade / 2)
        : chipBase * 2 + 40;
    const multValue = type === "CHIP_BUMPER" ? 0 : type === "SPINNER_RING" ? 1 : multBase;
    const coinValue = type === "JACKPOT_STAR" ? 2 : 0;
    const speedBoost = bossMod.id === "SPEED_TRAP" ? 1.65 : 1;

    targets.push({
      id: nextId++,
      type,
      pos: { x, y },
      radius,
      chipValue,
      multValue,
      coinValue,
      restitution: bumperRestitution,
      patrolCenter: { x, y },
      patrolAxis,
      patrolAmplitude: patrolAmp,
      patrolSpeed: patrolSpeed * speedBoost,
      patrolPhase: rng() * Math.PI * 2,
      hitCooldown: 0,
      hitFlash: 0,
      hitCount: 0,
      spinAngle: 0
    });
  };

  const addObstacle = (type, x, y, halfW = 56, halfH = 24, hp = null, dropsCoin = 0, dropsPowerUp = null) => {
    const resolvedHp =
      hp ??
      (type === "ICE_BLOCK"
        ? Math.min(3, 1 + Math.floor(ante / 2))
        : type === "STEEL_CRATE"
        ? Math.min(4, 2 + Math.floor(ante / 2))
        : 1);
    const resolvedW = type === "CRYO_BARREL" ? 34 : halfW;
    const resolvedH = type === "CRYO_BARREL" ? 34 : halfH;
    const chipBonus = type === "ICE_BLOCK" ? 45 + ante * 10 : type === "STEEL_CRATE" ? 70 + ante * 15 : 90 + ante * 15;

    obstacles.push({
      id: nextId++,
      type,
      isCircular: type === "CRYO_BARREL",
      pos: { x, y },
      halfW: resolvedW,
      halfH: resolvedH,
      radius: resolvedW,
      hp: resolvedHp,
      maxHp: resolvedHp,
      chipBonus,
      dropsCoin,
      dropsPowerUp,
      hitFlash: 0,
      destroyed: false
    });
  };

  const archetype = (ante * 3 + roundInAnte + Math.floor(rng() * 5)) % 5;
  if (archetype === 0) {
    addTarget("JACKPOT_STAR", 500, 340, { x: 1, y: 0 }, 110, 1.3);
    addTarget("MULT_BUMPER", 240, 330);
    addTarget("MULT_BUMPER", 760, 330);
    addTarget("CHIP_BUMPER", 355, 565);
    addTarget("CHIP_BUMPER", 645, 565);
    addTarget("SPINNER_RING", 500, 700);
    addTarget("CHIP_BUMPER", 225, 835);
    addTarget("CHIP_BUMPER", 775, 835);
    addTarget("MULT_BUMPER", 500, 945);

    addObstacle("ICE_BLOCK", 500, 470, 76, 22);
    addObstacle("ICE_BLOCK", 360, 415, 52, 20);
    addObstacle("ICE_BLOCK", 640, 415, 52, 20);
    addObstacle("CRYO_BARREL", 210, 575);
    addObstacle("CRYO_BARREL", 790, 575);
    addObstacle("STEEL_CRATE", 500, 825, 54, 24, null, 2, randomPowerUpType());
  } else if (archetype === 1) {
    addTarget("MULT_BUMPER", 500, 265);
    addTarget("CHIP_BUMPER", 320, 355);
    addTarget("CHIP_BUMPER", 680, 355);
    addTarget("JACKPOT_STAR", 500, 495, { x: 1, y: 0 }, 150, 1.45);
    addTarget("SPINNER_RING", 215, 650);
    addTarget("SPINNER_RING", 785, 650);
    addTarget("CHIP_BUMPER", 375, 775);
    addTarget("CHIP_BUMPER", 625, 775);
    addTarget("MULT_BUMPER", 500, 920);

    addObstacle("ICE_BLOCK", 270, 520, 62, 22);
    addObstacle("ICE_BLOCK", 730, 520, 62, 22);
    addObstacle("CRYO_BARREL", 500, 655);
    addObstacle("STEEL_CRATE", 250, 890, 48, 24, null, 1);
    addObstacle("STEEL_CRATE", 750, 890, 48, 24, null, 0, randomPowerUpType());
  } else if (archetype === 2) {
    addTarget("JACKPOT_STAR", 250, 310);
    addTarget("JACKPOT_STAR", 750, 310);
    addTarget("MULT_BUMPER", 500, 340, { x: 0, y: 1 }, 65, 1.5);
    addTarget("CHIP_BUMPER", 240, 560);
    addTarget("CHIP_BUMPER", 760, 560);
    addTarget("SPINNER_RING", 500, 625);
    addTarget("MULT_BUMPER", 350, 825);
    addTarget("MULT_BUMPER", 650, 825);
    addTarget("CHIP_BUMPER", 500, 975);

    addObstacle("ICE_BLOCK", 250, 430, 74, 22);
    addObstacle("ICE_BLOCK", 750, 430, 74, 22);
    addObstacle("CRYO_BARREL", 380, 460);
    addObstacle("CRYO_BARREL", 620, 460);
    addObstacle("STEEL_CRATE", 500, 765, 60, 24, null, 2);
    addObstacle("ICE_BLOCK", 210, 745, 48, 22);
    addObstacle("ICE_BLOCK", 790, 745, 48, 22);
  } else if (archetype === 3) {
    addTarget("JACKPOT_STAR", 500, 285, { x: 1, y: 0 }, 200, 1.55);
    addTarget("MULT_BUMPER", 245, 445);
    addTarget("MULT_BUMPER", 755, 445);
    addTarget("CHIP_BUMPER", 500, 515);
    addTarget("CHIP_BUMPER", 290, 690, { x: 1, y: 0 }, 70, 1.2);
    addTarget("CHIP_BUMPER", 710, 690, { x: -1, y: 0 }, 70, 1.2);
    addTarget("SPINNER_RING", 500, 815);
    addTarget("MULT_BUMPER", 230, 920);
    addTarget("MULT_BUMPER", 770, 920);

    addObstacle("CRYO_BARREL", 385, 385);
    addObstacle("CRYO_BARREL", 615, 385);
    addObstacle("ICE_BLOCK", 500, 635, 78, 22);
    addObstacle("STEEL_CRATE", 365, 890, 52, 24, null, 1, randomPowerUpType());
    addObstacle("STEEL_CRATE", 635, 890, 52, 24, null, 2);
  } else {
    const cx = 500;
    const cy = 580;
    addTarget("JACKPOT_STAR", cx, cy);
    for (let i = 0; i < 6; i++) {
      const ang = (i * Math.PI) / 3;
      addTarget(i % 2 === 0 ? "CHIP_BUMPER" : "MULT_BUMPER", cx + Math.cos(ang) * 245, cy + Math.sin(ang) * 245);
    }
    addTarget("SPINNER_RING", 250, 285);
    addTarget("SPINNER_RING", 750, 285);
    addTarget("CHIP_BUMPER", 500, 960);

    addObstacle("ICE_BLOCK", 375, 455, 46, 20);
    addObstacle("ICE_BLOCK", 625, 455, 46, 20);
    addObstacle("ICE_BLOCK", 375, 705, 46, 20);
    addObstacle("ICE_BLOCK", 625, 705, 46, 20);
    addObstacle("CRYO_BARREL", 500, 430);
    addObstacle("STEEL_CRATE", 500, 740, 56, 22, null, 2, randomPowerUpType());
  }

  if (bossMod.id === "GLACIAL_WALL") {
    for (const wx of [190, 345, 500, 655, 810]) {
      addObstacle("ICE_BLOCK", wx, 1045, 58, 22, 2 + Math.floor(ante / 2));
    }
    addObstacle("CRYO_BARREL", 500, 965);
  } else if (bossMod.id === "SPEED_TRAP") {
    addTarget("JACKPOT_STAR", 500, 215, { x: 1, y: 0 }, 260, 2.2);
  }

  const spots = shuffle(
    [
      { x: 175, y: 1020 },
      { x: 825, y: 1020 },
      { x: 500, y: 1065 },
      { x: 165, y: 420 },
      { x: 835, y: 420 },
      { x: 500, y: 210 }
    ],
    rng
  );
  const desiredPowerUps = Math.min(spots.length, 2 + modifiers.extraPowerUps);
  const availableTypes = shuffle(POWERUP_TYPES, rng);
  let placed = 0;
  for (const spot of spots) {
    if (placed >= desiredPowerUps) break;
    const blocked =
      obstacles.some((o) => Vec.dist(spot, o.pos) < o.halfW + 48) ||
      targets.some((t) => Vec.dist(spot, t.pos) < t.radius + 46);
    if (!blocked) {
      powerUps.push({
        id: nextId++,
        type: availableTypes[placed % availableTypes.length].id,
        pos: spot,
        radius: 30,
        pulsePhase: rng() * Math.PI * 2,
        collected: false
      });
      placed++;
    }
  }
  if (powerUps.length === 0) {
    powerUps.push({
      id: nextId++,
      type: availableTypes[0].id,
      pos: { x: 165, y: 1110 },
      radius: 30,
      pulsePhase: 0,
      collected: false
    });
  }

  const puckMass = hasRelic("HEAVY_METAL_PUCK") ? 1.35 : 1;
  return {
    ante,
    roundInAnte,
    bossModifier: bossMod,
    targetScore,
    goalGate,
    pucks: [createPuck(1, { x: TableConstants.DEFAULT_PUCK_X, y: TableConstants.DEFAULT_PUCK_Y }, { x: 0, y: 0 }, puckMass, true)],
    targets,
    obstacles,
    powerUps,
    particles: [],
    shockwaves: [],
    floatingTexts: [],
    shotScore: createShotScore(),
    shotPhase: "AIMING",
    scoringPopupTimer: 0,
    lastBankedScore: 0,
    lastBankedChips: 0,
    lastBankedMult: 1,
    screenShake: 0,
    tableTime: 0,
    currentWindX: 0,
    firstFlickUsed: false,
    nextEntityId: nextId + 10
  };
}

export class PhysicsEngine {
  constructor(rng = Math.random) {
    this.rng = rng;
    this.SUBSTEP_DT = 1 / 120;
    this.MAX_FRAME_DT = 0.05;
    this.MAX_PUCK_SPEED = 2450;
    this.SETTLE_TIME = 0.35;
    this.MAX_SHOT_SECONDS = 11.5;
    this.SCORING_POPUP_DURATION = 1.05;
  }

  launchPuck(state, velocity, modifiers) {
    if (state.shotPhase !== "AIMING") return [];
    const mainPuck = state.pucks[0];
    if (!mainPuck) return [];

    const maxSpeed = TableConstants.MAX_LAUNCH_SPEED * modifiers.launchSpeedMultiplier;
    const rawSpeed = Vec.len(velocity);
    if (rawSpeed < 1e-4) return [];
    const speed = Math.max(TableConstants.MIN_LAUNCH_SPEED, Math.min(maxSpeed, rawSpeed));
    const dir = Vec.norm(velocity);

    mainPuck.vel = Vec.mul(dir, speed);
    mainPuck.slowTimer = 0;
    mainPuck.trail = [];
    state.shotScore = createShotScore();
    state.shotPhase = "IN_PLAY";

    this.spawnParticles(state, mainPuck.pos, 12, "#38BDF8", 120, 360);

    if (!state.firstFlickUsed && modifiers.relics.includes("SPLIT_PERSONALITY")) {
      state.pucks.push(this.createSplitPuck(state, mainPuck, 0.24));
      this.addFloatingText(state, { x: mainPuck.pos.x, y: mainPuck.pos.y - 55 }, "TWIN LAUNCH!", "#0284C7", 1.1);
    }
    state.firstFlickUsed = true;
    return ["FLICK_LAUNCH"];
  }

  recallPucksEarly(state) {
    if (state.shotPhase !== "IN_PLAY") return { soundEvents: [], shotBankedScore: null, shotBankedCoins: 0 };
    return this.finishShot(state);
  }

  step(state, frameDt, tiltInput, tiltSensitivity, modifiers) {
    const dt = Math.max(0, Math.min(this.MAX_FRAME_DT, frameDt));
    state.tableTime += dt;
    const events = [];

    state.screenShake = Math.max(0, state.screenShake - dt * 28);
    this.updateEffects(state, dt);
    this.updateMovingTargets(state, dt);
    for (const pu of state.powerUps) {
      if (!pu.collected) pu.pulsePhase += dt * 3.8;
    }

    state.currentWindX = state.bossModifier.id === "MAGNETIC_DRIFT" ? Math.sin(state.tableTime * 1.45) * 520 : 0;

    if (state.shotPhase === "SCORING_POPUP") {
      state.scoringPopupTimer -= dt;
      if (state.scoringPopupTimer <= 0) {
        this.resetPuckForNextFlick(state, modifiers);
        state.shotPhase = "AIMING";
      }
      return { soundEvents: events, shotBankedScore: null, shotBankedCoins: 0 };
    }

    if (state.shotPhase !== "IN_PLAY") {
      return { soundEvents: events, shotBankedScore: null, shotBankedCoins: 0 };
    }

    let remaining = dt;
    while (remaining > 1e-5) {
      const subDt = Math.min(remaining, this.SUBSTEP_DT);
      this.stepSubstep(state, subDt, tiltInput, tiltSensitivity, modifiers, events);
      remaining -= subDt;
    }

    state.shotScore.shotElapsed += dt;
    state.shotScore.lastHitElapsed += dt;

    let allSlow = true;
    for (const p of state.pucks) {
      if (Vec.len(p.vel) < TableConstants.STOP_SPEED_THRESHOLD) {
        p.slowTimer += dt;
      } else {
        p.slowTimer = 0;
        allSlow = false;
      }
    }

    const timedOut = state.shotScore.shotElapsed >= this.MAX_SHOT_SECONDS;
    const settled = allSlow && state.pucks.every((p) => p.slowTimer >= this.SETTLE_TIME);
    if (settled || timedOut) {
      const finish = this.finishShot(state);
      return {
        soundEvents: events.concat(finish.soundEvents),
        shotBankedScore: finish.shotBankedScore,
        shotBankedCoins: finish.shotBankedCoins
      };
    }

    return { soundEvents: events, shotBankedScore: null, shotBankedCoins: 0 };
  }

  finishShot(state) {
    const total = state.shotScore.previewTotal;
    const coins = state.shotScore.coinsEarnedThisShot;
    state.lastBankedChips = state.shotScore.chips;
    state.lastBankedMult = state.shotScore.mult;
    state.lastBankedScore = total;

    for (const p of state.pucks) {
      p.vel = { x: 0, y: 0 };
    }
    state.shotPhase = "SCORING_POPUP";
    state.scoringPopupTimer = total > 0 ? this.SCORING_POPUP_DURATION : 0.42;

    return {
      soundEvents: total > 0 ? ["SHOT_BANKED"] : [],
      shotBankedScore: total,
      shotBankedCoins: coins
    };
  }

  resetPuckForNextFlick(state, modifiers) {
    const mass = modifiers.relics.includes("HEAVY_METAL_PUCK") ? 1.35 : 1;
    state.pucks = [
      createPuck(1, { x: TableConstants.DEFAULT_PUCK_X, y: TableConstants.DEFAULT_PUCK_Y }, { x: 0, y: 0 }, mass, true)
    ];
    state.shotScore = createShotScore();
  }

  stepSubstep(state, dt, tiltInput, tiltSensitivity, modifiers, events) {
    const hasRelic = (id) => modifiers.relics.includes(id);
    const baseFriction =
      state.bossModifier.id === "HEAVY_ICE" ? 0.88 : hasRelic("AIR_CUSHION") ? 0.34 : 0.52;

    const tiltPower = tiltSensitivity.accelMultiplier * (hasRelic("TILT_MASTER") ? 1.6 : 1);
    const tiltAccel = { x: tiltInput.x * tiltPower, y: tiltInput.y * tiltPower };
    const isTiltingActively = tiltSensitivity.id !== "OFF" && Vec.len(tiltInput) > 0.18;

    for (const t of state.targets) {
      if (t.hitCooldown > 0) t.hitCooldown = Math.max(0, t.hitCooldown - dt);
      if (t.hitFlash > 0) t.hitFlash = Math.max(0, t.hitFlash - dt * 3.5);
      if (t.type === "SPINNER_RING") t.spinAngle += dt * (2.2 + t.hitFlash * 10);
    }
    for (const o of state.obstacles) {
      if (o.hitFlash > 0) o.hitFlash = Math.max(0, o.hitFlash - dt * 4);
    }
    if (state.goalGate.flashTimer > 0) {
      state.goalGate.flashTimer = Math.max(0, state.goalGate.flashTimer - dt * 2.2);
    }

    const spawned = [];
    for (const puck of state.pucks) {
      if (puck.overdriveTimer > 0) puck.overdriveTimer = Math.max(0, puck.overdriveTimer - dt);
      if (puck.giantTimer > 0) puck.giantTimer = Math.max(0, puck.giantTimer - dt);
      if (puck.magnetTimer > 0) puck.magnetTimer = Math.max(0, puck.magnetTimer - dt);
      if (puck.goalCooldown > 0) puck.goalCooldown = Math.max(0, puck.goalCooldown - dt);

      const speed = Vec.len(puck.vel);
      const tiltFactor = speed > 30 ? 1 : 0.35;
      puck.vel.x += tiltAccel.x * tiltFactor * dt;
      puck.vel.y += tiltAccel.y * tiltFactor * dt;

      if (state.currentWindX !== 0 && puck.pos.y >= 360 && puck.pos.y <= 1040) {
        puck.vel.x += state.currentWindX * dt;
      }

      if (puck.isMagnet && speed > 60 && state.targets.length > 0) {
        let nearest = null;
        let bestDist = Infinity;
        for (const t of state.targets) {
          const d = Vec.dist(t.pos, puck.pos);
          if (d < bestDist) {
            bestDist = d;
            nearest = t;
          }
        }
        if (nearest && bestDist >= 40 && bestDist <= 360) {
          const toT = Vec.norm(Vec.sub(nearest.pos, puck.pos));
          puck.vel.x += toT.x * 980 * dt;
          puck.vel.y += toT.y * 980 * dt;
        }
      }

      const damp = Math.max(0.85, Math.min(1, 1 - baseFriction * dt));
      puck.vel = Vec.clampLen(Vec.mul(puck.vel, damp), this.MAX_PUCK_SPEED);
      puck.pos.x += puck.vel.x * dt;
      puck.pos.y += puck.vel.y * dt;

      if (puck.trail.length === 0 || Vec.dist(puck.trail[puck.trail.length - 1], puck.pos) > 14) {
        puck.trail.push({ x: puck.pos.x, y: puck.pos.y });
        if (puck.trail.length > 16) puck.trail.shift();
      }

      this.resolveWallsAndGoal(state, puck, modifiers, events);
      this.resolvePowerUps(state, puck, spawned, events);
      this.resolveTargets(state, puck, modifiers, isTiltingActively, events);
      this.resolveObstacles(state, puck, modifiers, events);
    }

    if (spawned.length > 0) state.pucks.push(...spawned);

    for (let i = 0; i < state.pucks.length; i++) {
      for (let j = i + 1; j < state.pucks.length; j++) {
        this.resolvePuckToPuck(state.pucks[i], state.pucks[j]);
      }
    }
  }

  resolveWallsAndGoal(state, puck, modifiers, events) {
    const r = puck.radius;
    const minX = TableConstants.WALL_MARGIN + r;
    const maxX = TableConstants.TABLE_WIDTH - TableConstants.WALL_MARGIN - r;
    const minY = TableConstants.WALL_MARGIN + r;
    const maxY = TableConstants.TABLE_HEIGHT - TableConstants.WALL_MARGIN - r;
    const rest = 0.9;
    let bounced = false;

    if (puck.pos.x < minX) {
      puck.pos.x = minX;
      if (puck.vel.x < 0) {
        puck.vel.x = -puck.vel.x * rest;
        bounced = true;
      }
    } else if (puck.pos.x > maxX) {
      puck.pos.x = maxX;
      if (puck.vel.x > 0) {
        puck.vel.x = -puck.vel.x * rest;
        bounced = true;
      }
    }

    if (puck.pos.y < minY) {
      const inGoal = puck.pos.x >= state.goalGate.xMin + r * 0.35 && puck.pos.x <= state.goalGate.xMax - r * 0.35;
      puck.pos.y = minY;
      if (puck.vel.y < 0) {
        if (inGoal && puck.goalCooldown <= 0) {
          puck.goalCooldown = 0.75;
          puck.vel = { x: puck.vel.x * 0.85, y: Math.abs(puck.vel.y) * 0.72 + 220 };
          state.goalGate.flashTimer = 1;
          state.screenShake = Math.max(state.screenShake, 12);
          state.shotScore.goalsScored++;
          state.shotScore.chips += state.goalGate.chipReward;
          state.shotScore.mult += state.goalGate.multReward;
          if (modifiers.relics.includes("GOAL_POACHER")) {
            state.shotScore.mult *= 2;
            this.addFloatingText(state, { x: puck.pos.x, y: minY + 58 }, "TOP SHELF! x2 MULT", "#059669", 1.25);
          } else {
            this.addFloatingText(state, { x: puck.pos.x, y: minY + 52 }, `GOAL! +${state.goalGate.chipReward}`, "#059669", 1.2);
          }
          this.spawnShockwave(state, { x: puck.pos.x, y: TableConstants.WALL_MARGIN + 8 }, 190, "#10B981");
          this.spawnParticles(state, { x: puck.pos.x, y: minY }, 22, "#10B981", 180, 560);
          events.push("GOAL_SCORE");
        } else {
          puck.vel.y = -puck.vel.y * rest;
          bounced = true;
        }
      }
    } else if (puck.pos.y > maxY) {
      puck.pos.y = maxY;
      if (puck.vel.y > 0) {
        puck.vel.y = -puck.vel.y * rest;
        bounced = true;
      }
    }

    if (bounced) {
      state.shotScore.wallBounces++;
      events.push("WALL_BOUNCE");
      this.spawnParticles(state, puck.pos, 5, "#94A3B8", 70, 210);
      if (modifiers.relics.includes("BANK_SHOT_PRO")) {
        state.shotScore.mult += 2;
        this.addFloatingText(state, { x: puck.pos.x, y: puck.pos.y - 24 }, "BANK +2x", "#0284C7", 0.92);
      }
    }
  }

  resolvePowerUps(state, puck, newlySpawned, events) {
    for (const pu of state.powerUps) {
      if (pu.collected) continue;
      if (Vec.dist(puck.pos, pu.pos) < puck.radius + pu.radius) {
        pu.collected = true;
        events.push("POWERUP_PICKUP");
        this.spawnShockwave(state, pu.pos, 120, "#0891B2");
        this.spawnParticles(state, pu.pos, 16, "#06B6D4", 140, 400);

        if (pu.type === "MULTI_PUCK") {
          newlySpawned.push(this.createSplitPuck(state, puck, 0.35));
          this.addFloatingText(state, pu.pos, "MULTI-PUCK!", "#0284C7", 1.15);
        } else if (pu.type === "MAGNET_PULL") {
          puck.magnetTimer = 5.5;
          this.addFloatingText(state, pu.pos, "MAGNET ON!", "#0891B2", 1.15);
        } else if (pu.type === "OVERDRIVE_FIRE") {
          puck.overdriveTimer = 6.0;
          puck.vel = Vec.clampLen(Vec.mul(puck.vel, 1.18), this.MAX_PUCK_SPEED);
          this.addFloatingText(state, pu.pos, "OVERDRIVE!", "#EA580C", 1.15);
        } else if (pu.type === "GIANT_PUCK") {
          puck.giantTimer = 6.0;
          this.addFloatingText(state, pu.pos, "GIANT PUCK!", "#059669", 1.15);
        } else if (pu.type === "SCORE_SURGE") {
          state.shotScore.mult = Math.max(2, state.shotScore.mult * 2);
          this.addFloatingText(state, pu.pos, "SURGE x2 MULT!", "#D97706", 1.25);
        }
      }
    }
  }

  resolveTargets(state, puck, modifiers, isTiltingActively, events) {
    for (const target of state.targets) {
      const delta = Vec.sub(puck.pos, target.pos);
      const distSq = Vec.lenSq(delta);
      const minDist = puck.radius + target.radius;
      if (distSq >= minDist * minDist) continue;

      if (target.type === "SPINNER_RING") {
        if (target.hitCooldown <= 0) {
          target.hitCooldown = 0.28;
          target.hitFlash = 1;
          target.hitCount++;
          puck.vel = Vec.clampLen(Vec.mul(puck.vel, 1.16), this.MAX_PUCK_SPEED);
          this.applyTargetScore(state, target, puck, modifiers, isTiltingActively, events);
        }
        continue;
      }

      const dist = Math.sqrt(Math.max(distSq, 1e-4));
      const normal = { x: delta.x / dist, y: delta.y / dist };
      const overlap = minDist - dist;
      puck.pos.x += normal.x * (overlap + 0.5);
      puck.pos.y += normal.y * (overlap + 0.5);

      const vn = Vec.dot(puck.vel, normal);
      if (vn < 0) {
        const kick = state.bossModifier.id === "BUMPER_CHAOS" ? 240 : 110;
        const factor = (1 + target.restitution) * vn;
        puck.vel = Vec.clampLen(
          {
            x: puck.vel.x - normal.x * factor + normal.x * kick,
            y: puck.vel.y - normal.y * factor + normal.y * kick
          },
          this.MAX_PUCK_SPEED
        );
      }

      if (target.hitCooldown <= 0) {
        target.hitCooldown = 0.14;
        target.hitFlash = 1;
        target.hitCount++;
        this.applyTargetScore(state, target, puck, modifiers, isTiltingActively, events);
      }
    }
  }

  applyTargetScore(state, target, puck, modifiers, isTiltingActively, events) {
    const hasRelic = (id) => modifiers.relics.includes(id);
    const echo = hasRelic("LUCKY_BOUNCE") && this.rng() < 0.3;
    const count = echo ? 2 : 1;

    for (let k = 0; k < count; k++) {
      const rapid = state.shotScore.lastHitElapsed < 0.55;
      state.shotScore.comboCount = rapid ? state.shotScore.comboCount + 1 : 1;
      state.shotScore.lastHitElapsed = 0;
      state.shotScore.targetsHit++;

      let chips = target.chipValue;
      if (puck.isOverdrive) chips = Math.floor(chips * 1.5);
      if (hasRelic("COMBO_ENGINE") && state.shotScore.comboCount > 1) {
        chips += (state.shotScore.comboCount - 1) * 15;
      }
      if (hasRelic("TILT_MASTER") && isTiltingActively) {
        chips += 25;
      }

      state.shotScore.chips += chips;
      state.shotScore.mult += target.multValue;
      if (target.coinValue > 0) state.shotScore.coinsEarnedThisShot += target.coinValue;

      if (hasRelic("MIDAS_TOUCH") && state.shotScore.targetsHit % 4 === 0) {
        state.shotScore.coinsEarnedThisShot += 1;
        this.addFloatingText(state, { x: target.pos.x, y: target.pos.y - 44 }, "+$1", "#CA8A04", 1.0);
      }

      if (hasRelic("PINBALL_WIZARD") && !state.shotScore.pinballWizardTriggered && state.shotScore.targetsHit >= 5) {
        state.shotScore.pinballWizardTriggered = true;
        state.shotScore.mult = Math.max(state.shotScore.mult + 1, Math.floor(state.shotScore.mult * 1.5));
        this.addFloatingText(state, { x: target.pos.x, y: target.pos.y - 60 }, "PINBALL x1.5!", "#D97706", 1.2);
      }

      const parts = [];
      if (chips > 0) parts.push(`+${chips}`);
      if (target.multValue > 0) parts.push(`+${target.multValue}x`);
      if (target.coinValue > 0) parts.push(`+$${target.coinValue}`);

      const color =
        target.type === "CHIP_BUMPER"
          ? "#F04E54"
          : target.type === "MULT_BUMPER"
          ? "#D97706"
          : target.type === "SPINNER_RING"
          ? "#0891B2"
          : "#B45309";
      this.addFloatingText(state, { x: target.pos.x, y: target.pos.y - 24 }, parts.join(" "), color, echo ? 1.15 : 1.0);
      this.spawnParticles(state, target.pos, 10, color, 110, 320);
    }

    if (echo) {
      this.addFloatingText(state, { x: target.pos.x, y: target.pos.y - 52 }, "ECHO x2!", "#EC4899", 1.05);
    }

    events.push(
      target.type === "CHIP_BUMPER"
        ? "CHIP_HIT"
        : target.type === "MULT_BUMPER"
        ? "MULT_HIT"
        : target.type === "SPINNER_RING"
        ? "SPINNER_HIT"
        : "JACKPOT_HIT"
    );
  }

  resolveObstacles(state, puck, modifiers, events) {
    const baseDamage =
      (1 + (modifiers.relics.includes("HEAVY_METAL_PUCK") ? 1 : 0)) * (puck.isOverdrive ? 2 : 1);
    const barrelsToExplode = [];

    for (const obs of state.obstacles) {
      if (obs.destroyed) continue;
      let hitNormal;
      let penetration;

      if (obs.isCircular) {
        const delta = Vec.sub(puck.pos, obs.pos);
        const distSq = Vec.lenSq(delta);
        const minDist = puck.radius + obs.radius;
        if (distSq >= minDist * minDist) continue;
        const dist = Math.sqrt(Math.max(distSq, 1e-4));
        hitNormal = { x: delta.x / dist, y: delta.y / dist };
        penetration = minDist - dist;
      } else {
        const cx = Math.max(obs.pos.x - obs.halfW, Math.min(obs.pos.x + obs.halfW, puck.pos.x));
        const cy = Math.max(obs.pos.y - obs.halfH, Math.min(obs.pos.y + obs.halfH, puck.pos.y));
        const delta = { x: puck.pos.x - cx, y: puck.pos.y - cy };
        const distSq = Vec.lenSq(delta);
        if (distSq >= puck.radius * puck.radius) continue;
        if (distSq > 1e-4) {
          const dist = Math.sqrt(distSq);
          hitNormal = { x: delta.x / dist, y: delta.y / dist };
          penetration = puck.radius - dist;
        } else {
          const dx = puck.pos.x - obs.pos.x;
          const dy = puck.pos.y - obs.pos.y;
          const penX = obs.halfW - Math.abs(dx);
          const penY = obs.halfH - Math.abs(dy);
          if (penX < penY) {
            hitNormal = { x: dx >= 0 ? 1 : -1, y: 0 };
            penetration = penX + puck.radius;
          } else {
            hitNormal = { x: 0, y: dy >= 0 ? 1 : -1 };
            penetration = penY + puck.radius;
          }
        }
      }

      const willDestroy = obs.hp - baseDamage <= 0;
      const shouldPierce = puck.isOverdrive && willDestroy && obs.type === "ICE_BLOCK";

      if (!shouldPierce) {
        puck.pos.x += hitNormal.x * (penetration + 0.5);
        puck.pos.y += hitNormal.y * (penetration + 0.5);
        const vn = Vec.dot(puck.vel, hitNormal);
        if (vn < 0) {
          const bounce = obs.type === "STEEL_CRATE" ? 1.02 : obs.type === "ICE_BLOCK" ? 0.88 : 0.95;
          puck.vel = Vec.clampLen(
            {
              x: puck.vel.x - hitNormal.x * (1 + bounce) * vn,
              y: puck.vel.y - hitNormal.y * (1 + bounce) * vn
            },
            this.MAX_PUCK_SPEED
          );
        }
      } else {
        puck.vel = Vec.mul(puck.vel, 0.92);
      }

      this.damageObstacle(state, obs, baseDamage, modifiers, barrelsToExplode, events);
    }

    for (let i = 0; i < barrelsToExplode.length; i++) {
      this.detonateBarrel(state, barrelsToExplode[i], modifiers, barrelsToExplode, events);
    }
  }

  damageObstacle(state, obs, damage, modifiers, barrelsToExplode, events) {
    if (obs.destroyed) return;
    obs.hp -= damage;
    obs.hitFlash = 1;

    if (obs.hp > 0) {
      events.push("OBSTACLE_CRACK");
      this.spawnParticles(state, obs.pos, 7, "#BAE6FD", 90, 250, true);
      return;
    }

    obs.destroyed = true;
    state.shotScore.obstaclesBroken++;
    let chips = obs.chipBonus;
    const hasIcebreaker = modifiers.relics.includes("ICEBREAKER");
    if (hasIcebreaker) {
      chips += 60;
      state.shotScore.mult += 1;
    }
    state.shotScore.chips += chips;

    if (obs.dropsCoin > 0) {
      state.shotScore.coinsEarnedThisShot += obs.dropsCoin;
      this.addFloatingText(state, { x: obs.pos.x, y: obs.pos.y - 38 }, `+$${obs.dropsCoin}`, "#CA8A04", 1.05);
    }
    if (obs.dropsPowerUp) {
      state.powerUps.push({
        id: state.nextEntityId++,
        type: obs.dropsPowerUp,
        pos: { x: obs.pos.x, y: obs.pos.y },
        radius: 30,
        pulsePhase: 0,
        collected: false
      });
      const def = POWERUP_TYPES.find((p) => p.id === obs.dropsPowerUp);
      this.addFloatingText(state, { x: obs.pos.x, y: obs.pos.y + 34 }, `${def ? def.shortTag : "POWER"}!`, "#0891B2", 0.95);
    }

    if (obs.type === "CRYO_BARREL") {
      barrelsToExplode.push(obs);
    } else {
      const color = obs.type === "ICE_BLOCK" ? "#7DD3FC" : "#64748B";
      this.spawnParticles(state, obs.pos, 18, color, 140, 440, true);
      this.addFloatingText(state, obs.pos, hasIcebreaker ? `SHATTER! +${chips} +1x` : `BREAK! +${chips}`, "#0284C7", 1.05);
      state.screenShake = Math.max(state.screenShake, 5);
      events.push("OBSTACLE_SHATTER");
    }
  }

  detonateBarrel(state, barrel, modifiers, barrelsToExplode, events) {
    const chain = modifiers.relics.includes("CHAIN_REACTION");
    const blastRadius = chain ? 330 : 225;
    const extraChips = chain ? 100 : 0;
    if (extraChips > 0) state.shotScore.chips += extraChips;
    state.shotScore.mult += 1;

    state.screenShake = Math.max(state.screenShake, 15);
    this.spawnShockwave(state, barrel.pos, blastRadius, "#EA580C");
    this.spawnParticles(state, barrel.pos, 28, "#F97316", 180, 640, true);
    this.addFloatingText(state, barrel.pos, `BOOM! +${barrel.chipBonus + extraChips} +1x`, "#EA580C", 1.25);
    events.push("BARREL_EXPLODE");

    for (const other of state.obstacles) {
      if (!other.destroyed && Vec.dist(other.pos, barrel.pos) <= blastRadius) {
        this.damageObstacle(state, other, 2, modifiers, barrelsToExplode, events);
      }
    }

    for (const target of state.targets) {
      if (Vec.dist(target.pos, barrel.pos) <= blastRadius) {
        target.hitFlash = 1;
        state.shotScore.chips += target.chipValue;
        state.shotScore.mult += target.multValue;
        this.spawnParticles(state, target.pos, 8, "#F59E0B", 100, 280);
      }
    }

    for (const p of state.pucks) {
      const delta = Vec.sub(p.pos, barrel.pos);
      const dist = Vec.len(delta);
      if (dist >= 1 && dist <= blastRadius) {
        const impulse = (1 - dist / blastRadius) * 620 + 160;
        const n = Vec.norm(delta);
        p.vel = Vec.clampLen({ x: p.vel.x + n.x * impulse, y: p.vel.y + n.y * impulse }, this.MAX_PUCK_SPEED);
      }
    }
  }

  resolvePuckToPuck(a, b) {
    const delta = Vec.sub(b.pos, a.pos);
    const distSq = Vec.lenSq(delta);
    const minDist = a.radius + b.radius;
    if (distSq >= minDist * minDist || distSq < 1e-4) return;
    const dist = Math.sqrt(distSq);
    const n = { x: delta.x / dist, y: delta.y / dist };
    const overlap = (minDist - dist) * 0.5;
    a.pos.x -= n.x * overlap;
    a.pos.y -= n.y * overlap;
    b.pos.x += n.x * overlap;
    b.pos.y += n.y * overlap;

    const sepVel = (b.vel.x - a.vel.x) * n.x + (b.vel.y - a.vel.y) * n.y;
    if (sepVel > 0) return;
    const invA = 1 / a.effectiveMass;
    const invB = 1 / b.effectiveMass;
    const j = (-(1 + 0.94) * sepVel) / (invA + invB);
    a.vel.x -= n.x * j * invA;
    a.vel.y -= n.y * j * invA;
    b.vel.x += n.x * j * invB;
    b.vel.y += n.y * j * invB;
  }

  createSplitPuck(state, source, angleOffset) {
    const baseAngle = Math.atan2(source.vel.y, source.vel.x);
    const speed = Math.max(Vec.len(source.vel), 650);
    const ang = baseAngle + angleOffset;
    const dir = { x: Math.cos(ang), y: Math.sin(ang) };
    const copy = createPuck(
      state.nextEntityId++,
      { x: source.pos.x + dir.x * 24, y: source.pos.y + dir.y * 24 },
      Vec.mul(dir, speed),
      source.mass * 0.9,
      false,
      source.baseRadius * 0.92
    );
    copy.overdriveTimer = source.overdriveTimer;
    copy.magnetTimer = source.magnetTimer;
    return copy;
  }

  updateMovingTargets(state, dt) {
    for (const t of state.targets) {
      if (t.patrolAmplitude > 0 && t.patrolSpeed > 0) {
        t.patrolPhase += dt * t.patrolSpeed;
        const off = Math.sin(t.patrolPhase) * t.patrolAmplitude;
        t.pos.x = t.patrolCenter.x + t.patrolAxis.x * off;
        t.pos.y = t.patrolCenter.y + t.patrolAxis.y * off;
      }
    }
  }

  updateEffects(state, dt) {
    for (let i = state.particles.length - 1; i >= 0; i--) {
      const p = state.particles[i];
      p.life -= dt;
      if (p.life <= 0) {
        state.particles.splice(i, 1);
      } else {
        p.pos.x += p.vel.x * dt;
        p.pos.y += p.vel.y * dt;
        const d = Math.max(0.5, 1 - 2.4 * dt);
        p.vel.x *= d;
        p.vel.y *= d;
        p.rotation += p.rotSpeed * dt;
      }
    }
    for (let i = state.shockwaves.length - 1; i >= 0; i--) {
      const s = state.shockwaves[i];
      s.life -= dt;
      if (s.life <= 0) state.shockwaves.splice(i, 1);
      else s.radius = s.maxRadius * (1 - s.life / s.maxLife);
    }
    for (let i = state.floatingTexts.length - 1; i >= 0; i--) {
      const ft = state.floatingTexts[i];
      ft.life -= dt;
      if (ft.life <= 0) state.floatingTexts.splice(i, 1);
      else ft.pos.y -= 68 * dt;
    }
  }

  spawnParticles(state, origin, count, color, minSpeed, maxSpeed, isShard = false) {
    if (state.particles.length > 220) return;
    for (let i = 0; i < count; i++) {
      const ang = this.rng() * Math.PI * 2;
      const spd = minSpeed + this.rng() * (maxSpeed - minSpeed);
      const life = 0.28 + this.rng() * 0.34;
      state.particles.push({
        pos: { x: origin.x, y: origin.y },
        vel: { x: Math.cos(ang) * spd, y: Math.sin(ang) * spd },
        life,
        maxLife: life,
        size: isShard ? 7 + this.rng() * 9 : 4 + this.rng() * 5,
        color,
        isShard,
        rotation: this.rng() * 360,
        rotSpeed: (this.rng() - 0.5) * 540
      });
    }
  }

  spawnShockwave(state, origin, maxRadius, color) {
    state.shockwaves.push({
      pos: { x: origin.x, y: origin.y },
      radius: 12,
      maxRadius,
      life: 0.36,
      maxLife: 0.36,
      color
    });
  }

  addFloatingText(state, pos, text, color, scale = 1) {
    if (state.floatingTexts.length > 26) state.floatingTexts.shift();
    state.floatingTexts.push({
      pos: { x: pos.x, y: pos.y },
      text,
      color,
      life: 0.85,
      maxLife: 0.85,
      scale
    });
  }

  computeAimTrajectory(start, direction, puckRadius, maxLength = 720) {
    const dir = Vec.norm(direction);
    if (Vec.lenSq(dir) < 0.01) return [start];

    const minX = TableConstants.WALL_MARGIN + puckRadius;
    const maxX = TableConstants.TABLE_WIDTH - TableConstants.WALL_MARGIN - puckRadius;
    const minY = TableConstants.WALL_MARGIN + puckRadius;
    const maxY = TableConstants.TABLE_HEIGHT - TableConstants.WALL_MARGIN - puckRadius;

    let tBest = maxLength;
    let hitNormal = { x: 0, y: 0 };

    if (dir.x < -1e-4) {
      const t = (minX - start.x) / dir.x;
      if (t >= 0 && t <= tBest) {
        tBest = t;
        hitNormal = { x: 1, y: 0 };
      }
    } else if (dir.x > 1e-4) {
      const t = (maxX - start.x) / dir.x;
      if (t >= 0 && t <= tBest) {
        tBest = t;
        hitNormal = { x: -1, y: 0 };
      }
    }

    if (dir.y < -1e-4) {
      const t = (minY - start.y) / dir.y;
      if (t >= 0 && t <= tBest) {
        tBest = t;
        hitNormal = { x: 0, y: 1 };
      }
    } else if (dir.y > 1e-4) {
      const t = (maxY - start.y) / dir.y;
      if (t >= 0 && t <= tBest) {
        tBest = t;
        hitNormal = { x: 0, y: -1 };
      }
    }

    const hitPt = { x: start.x + dir.x * tBest, y: start.y + dir.y * tBest };
    const pts = [start, hitPt];
    if (Vec.lenSq(hitNormal) > 0.5 && tBest < maxLength) {
      const rem = Math.min(240, maxLength - tBest);
      const bounceDir = Vec.norm(Vec.reflect(dir, hitNormal));
      pts.push({ x: hitPt.x + bounceDir.x * rem, y: hitPt.y + bounceDir.y * rem });
    }
    return pts;
  }
}

export class RunManager {
  constructor(rng = Math.random) {
    this.rng = rng;
    this.physics = new PhysicsEngine(rng);
    this.screen = "TITLE";
    this.ante = 1;
    this.roundInAnte = 1;
    this.roundScore = 0;
    this.coins = 4;
    this.flicksRemaining = 5;
    this.modifiers = this.defaultModifiers();
    this.tableState = generateTable(1, 1, this.modifiers, null, rng);
    this.shopOffers = [];
    this.shopRerollCost = 3;
    this.nextRoundBossPreview = BOSS_MODIFIERS.NONE;
    this.roundClearSummary = null;
    this.stats = this.defaultStats();
    this.controlModeIdx = 0;
    this.tiltSensitivityIdx = 2; // MEDIUM
  }

  defaultModifiers() {
    return {
      relics: [],
      maxFlicks: 5,
      chipBonusUpgrade: 0,
      multBonusUpgrade: 0,
      extraPowerUps: 0,
      launchSpeedMultiplier: 1
    };
  }

  defaultStats() {
    return {
      totalScoreAllRounds: 0,
      highestShotScore: 0,
      roundsCleared: 0,
      obstaclesDestroyed: 0,
      goalsScored: 0
    };
  }

  get controlMode() {
    return ControlModes[this.controlModeIdx];
  }

  get tiltSensitivity() {
    return TiltSensitivities[this.tiltSensitivityIdx];
  }

  cycleControlMode() {
    this.controlModeIdx = (this.controlModeIdx + 1) % ControlModes.length;
  }

  cycleTiltSensitivity() {
    this.tiltSensitivityIdx = (this.tiltSensitivityIdx + 1) % TiltSensitivities.length;
  }

  startNewRun() {
    this.ante = 1;
    this.roundInAnte = 1;
    this.roundScore = 0;
    this.coins = 4;
    this.modifiers = this.defaultModifiers();
    this.flicksRemaining = this.modifiers.maxFlicks;
    this.stats = this.defaultStats();
    this.roundClearSummary = null;
    this.shopOffers = [];
    this.tableState = generateTable(this.ante, this.roundInAnte, this.modifiers, null, this.rng);
    this.screen = "PLAYING";
  }

  launchShot(velocity) {
    if (this.screen !== "PLAYING") return [];
    if (this.tableState.shotPhase !== "AIMING" || this.flicksRemaining <= 0) return [];
    const events = this.physics.launchPuck(this.tableState, velocity, this.modifiers);
    if (events.length > 0) this.flicksRemaining--;
    return events;
  }

  recallPucksEarly() {
    if (this.screen !== "PLAYING" || this.tableState.shotPhase !== "IN_PLAY") return [];
    const res = this.physics.recallPucksEarly(this.tableState);
    return this.handleShotCompletion(res);
  }

  tick(dt, tiltInput) {
    if (this.screen !== "PLAYING") return [];
    const res = this.physics.step(this.tableState, dt, tiltInput, this.tiltSensitivity, this.modifiers);
    return this.handleShotCompletion(res);
  }

  handleShotCompletion(res) {
    const events = res.soundEvents.slice();
    if (res.shotBankedScore !== null && res.shotBankedScore !== undefined) {
      const banked = res.shotBankedScore;
      this.roundScore += banked;
      this.coins += res.shotBankedCoins;
      this.stats.totalScoreAllRounds += banked;
      if (banked > this.stats.highestShotScore) this.stats.highestShotScore = banked;
      this.stats.obstaclesDestroyed += this.tableState.shotScore.obstaclesBroken;
      this.stats.goalsScored += this.tableState.shotScore.goalsScored;

      if (this.roundScore >= this.tableState.targetScore) {
        this.completeRound();
        events.push("ROUND_CLEAR");
      } else if (this.flicksRemaining <= 0) {
        this.screen = "GAME_OVER";
        events.push("GAME_OVER");
      }
    }
    return events;
  }

  completeRound() {
    this.stats.roundsCleared++;
    const baseReward = 3 + this.roundInAnte + (this.ante - 1);
    const flickBonus = this.flicksRemaining;
    const interestBonus = Math.min(5, Math.floor(this.coins / 5));
    const totalPayout = baseReward + flickBonus + interestBonus;
    this.coins += totalPayout;

    this.roundClearSummary = {
      ante: this.ante,
      roundInAnte: this.roundInAnte,
      finalScore: this.roundScore,
      targetScore: this.tableState.targetScore,
      baseReward,
      unusedFlicks: this.flicksRemaining,
      flickBonus,
      interestBonus,
      totalPayout
    };

    const [nextAnte, nextRound] = this.computeNextRoundIndex();
    this.nextRoundBossPreview = bossModifierFor(nextAnte, nextRound, this.rng);
    this.screen = "ROUND_CLEAR";
  }

  openShop() {
    if (this.screen !== "ROUND_CLEAR") return;
    this.shopRerollCost = 3;
    this.rollShopOffers();
    this.screen = "SHOP";
  }

  rollShopOffers() {
    this.shopOffers = [];
    const owned = new Set(this.modifiers.relics);
    const availRelics = shuffle(Object.values(RELICS).filter((r) => !owned.has(r.id)), this.rng);
    availRelics.slice(0, 2).forEach((relic, i) => {
      this.shopOffers.push({
        id: `relic_${relic.id}_${i}`,
        kind: "RELIC",
        relic,
        title: relic.title,
        badge: relic.badge,
        description: relic.description,
        cost: relic.cost,
        purchased: false
      });
    });

    const availUpgrades = shuffle(
      Object.values(TABLE_UPGRADES).filter((u) => !(u.id === "EXTRA_FLICK" && this.modifiers.maxFlicks >= 8)),
      this.rng
    );
    availUpgrades.slice(0, 2).forEach((upg, i) => {
      this.shopOffers.push({
        id: `upg_${upg.id}_${i}`,
        kind: "UPGRADE",
        upgrade: upg,
        title: upg.title,
        badge: upg.badge,
        description: upg.description,
        cost: upg.baseCost + (this.ante - 1),
        purchased: false
      });
    });
  }

  rerollShop() {
    if (this.screen !== "SHOP" || this.coins < this.shopRerollCost) return false;
    this.coins -= this.shopRerollCost;
    this.shopRerollCost += 2;
    this.rollShopOffers();
    return true;
  }

  buyOffer(offerId) {
    if (this.screen !== "SHOP") return false;
    const offer = this.shopOffers.find((o) => o.id === offerId);
    if (!offer || offer.purchased || this.coins < offer.cost) return false;

    if (offer.kind === "RELIC") {
      if (this.modifiers.relics.length >= TableConstants.MAX_RELIC_SLOTS) return false;
      this.coins -= offer.cost;
      offer.purchased = true;
      this.modifiers.relics.push(offer.relic.id);
    } else {
      this.coins -= offer.cost;
      offer.purchased = true;
      const id = offer.upgrade.id;
      if (id === "EXTRA_FLICK") this.modifiers.maxFlicks = Math.min(8, this.modifiers.maxFlicks + 1);
      else if (id === "POLISH_BUMPERS") this.modifiers.chipBonusUpgrade += 20;
      else if (id === "BOOST_MULT_TARGETS") this.modifiers.multBonusUpgrade += 1;
      else if (id === "EXTRA_POWERUP") this.modifiers.extraPowerUps = Math.min(4, this.modifiers.extraPowerUps + 1);
      else if (id === "POWER_LAUNCH") this.modifiers.launchSpeedMultiplier += 0.15;
    }
    return true;
  }

  sellRelic(relicId) {
    if (this.screen !== "SHOP" || !this.modifiers.relics.includes(relicId)) return false;
    const relic = RELICS[relicId];
    const refund = Math.max(2, Math.floor(relic.cost / 2));
    this.coins += refund;
    this.modifiers.relics = this.modifiers.relics.filter((r) => r !== relicId);
    return true;
  }

  startNextRound() {
    const [nextAnte, nextRound] = this.computeNextRoundIndex();
    this.ante = nextAnte;
    this.roundInAnte = nextRound;
    this.roundScore = 0;
    this.flicksRemaining = this.modifiers.maxFlicks;
    this.roundClearSummary = null;
    this.tableState = generateTable(this.ante, this.roundInAnte, this.modifiers, this.nextRoundBossPreview, this.rng);
    this.screen = "PLAYING";
  }

  returnToTitle() {
    this.screen = "TITLE";
  }

  computeNextRoundIndex() {
    return this.roundInAnte < 3 ? [this.ante, this.roundInAnte + 1] : [this.ante + 1, 1];
  }

  nextRoundTargetPreview() {
    const [a, r] = this.computeNextRoundIndex();
    return targetScoreFor(a, r);
  }
}
