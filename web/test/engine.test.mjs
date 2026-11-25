import test from "node:test";
import assert from "node:assert/strict";
import {
  BOSS_MODIFIERS,
  PhysicsEngine,
  RunManager,
  TiltSensitivities,
  createPuck,
  createRng,
  createShotScore,
  targetScoreFor
} from "../src/engine.js";

function emptyTable() {
  return {
    ante: 1,
    roundInAnte: 1,
    bossModifier: BOSS_MODIFIERS.NONE,
    targetScore: 500,
    goalGate: { xMin: 375, xMax: 625, width: 250, chipReward: 130, multReward: 2, flashTimer: 0 },
    pucks: [createPuck(1, { x: 500, y: 1200 })],
    targets: [],
    obstacles: [],
    powerUps: [],
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
    nextEntityId: 1000
  };
}

test("targetScoreFor scales monotonically across Antes and Rounds", () => {
  assert.ok(targetScoreFor(1, 1) < targetScoreFor(1, 2));
  assert.ok(targetScoreFor(1, 2) < targetScoreFor(1, 3));
  assert.ok(targetScoreFor(1, 3) < targetScoreFor(2, 1));
  assert.ok(targetScoreFor(2, 1) < targetScoreFor(2, 3));
});

test("Cryo-barrel explosion shatters adjacent ice block and awards bonus", () => {
  const physics = new PhysicsEngine(createRng(2));
  const table = emptyTable();
  table.pucks[0].pos = { x: 500, y: 900 };
  const barrel = {
    id: 10,
    type: "CRYO_BARREL",
    isCircular: true,
    pos: { x: 500, y: 700 },
    halfW: 34,
    halfH: 34,
    radius: 34,
    hp: 1,
    maxHp: 1,
    chipBonus: 90,
    dropsCoin: 0,
    dropsPowerUp: null,
    hitFlash: 0,
    destroyed: false
  };
  const ice = {
    id: 11,
    type: "ICE_BLOCK",
    isCircular: false,
    pos: { x: 620, y: 700 },
    halfW: 50,
    halfH: 22,
    radius: 50,
    hp: 2,
    maxHp: 2,
    chipBonus: 50,
    dropsCoin: 0,
    dropsPowerUp: null,
    hitFlash: 0,
    destroyed: false
  };
  table.obstacles.push(barrel, ice);

  const mods = { relics: ["CHAIN_REACTION"], launchSpeedMultiplier: 1 };
  physics.launchPuck(table, { x: 0, y: -1400 }, mods);
  let exploded = false;
  for (let i = 0; i < 40; i++) {
    const res = physics.step(table, 1 / 60, { x: 0, y: 0 }, TiltSensitivities[0], mods);
    if (res.soundEvents.includes("BARREL_EXPLODE")) exploded = true;
  }

  assert.equal(exploded, true);
  assert.equal(barrel.destroyed, true);
  assert.equal(ice.destroyed, true);
  assert.ok(table.shotScore.chips >= 240);
});

test("Tilt input curves puck trajectory laterally", () => {
  const physics = new PhysicsEngine(createRng(4));
  const table = emptyTable();
  const mods = { relics: [], launchSpeedMultiplier: 1 };
  physics.launchPuck(table, { x: 0, y: -900 }, mods);
  for (let i = 0; i < 45; i++) {
    physics.step(table, 1 / 60, { x: 1, y: 0 }, TiltSensitivities[3], mods);
  }
  assert.ok(table.pucks[0].pos.x > 620);
});

test("RunManager clears round, pays out coins, and supports Pro Shop purchase/sell", () => {
  const rm = new RunManager(createRng(42));
  rm.startNewRun();
  assert.equal(rm.screen, "PLAYING");
  const startCoins = rm.coins;

  rm.launchShot({ x: 0, y: -1400 });
  rm.tableState.shotScore.chips = 300;
  rm.tableState.shotScore.mult = 4;
  rm.recallPucksEarly();

  assert.equal(rm.screen, "ROUND_CLEAR");
  assert.ok(rm.coins > startCoins);

  rm.openShop();
  assert.equal(rm.screen, "SHOP");
  assert.equal(rm.shopOffers.length, 4);

  const relicOffer = rm.shopOffers.find((o) => o.kind === "RELIC" && o.cost <= rm.coins);
  assert.ok(relicOffer);
  assert.equal(rm.buyOffer(relicOffer.id), true);
  assert.equal(rm.sellRelic(relicOffer.relic.id), true);

  rm.startNextRound();
  assert.equal(rm.screen, "PLAYING");
  assert.equal(rm.ante, 1);
  assert.equal(rm.roundInAnte, 2);
});
