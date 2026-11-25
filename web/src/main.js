import { RELICS, RunManager, TableConstants, Vec } from "./engine.js";
import { TableRenderer } from "./renderer.js";
import { SoundEngine } from "./sound.js";
import { TiltController } from "./tilt.js";

const $ = (id) => document.getElementById(id);

const run = new RunManager();
const sound = new SoundEngine();
const tilt = new TiltController();
const canvas = $("table-canvas");
const renderer = new TableRenderer(canvas);

tilt.start();
if (tilt.needsPermission) {
  $("btn-enable-sensors").classList.remove("hidden");
}

let inspectedRelicId = null;
let selectedShopRelicId = null;
let aimVisual = { active: false };

function emitSounds(events) {
  const combo = Math.max(1, run.tableState.shotScore.comboCount);
  for (const ev of events) sound.play(ev, combo);
}

function syncScreens() {
  $("screen-title").classList.toggle("hidden", run.screen !== "TITLE");
  $("screen-round-clear").classList.toggle("hidden", run.screen !== "ROUND_CLEAR");
  $("screen-shop").classList.toggle("hidden", run.screen !== "SHOP");
  $("screen-game-over").classList.toggle("hidden", run.screen !== "GAME_OVER");

  if (run.screen === "ROUND_CLEAR") renderRoundClear();
  if (run.screen === "SHOP") renderShop();
  if (run.screen === "GAME_OVER") renderGameOver();
  syncHud();
}

function syncHud() {
  const table = run.tableState;
  const isBoss = table.bossModifier.id !== "NONE";
  $("ante-badge").classList.toggle("is-boss", isBoss);
  $("ante-label").textContent = `ANTE ${run.ante}`;
  $("round-label").textContent = run.roundInAnte === 3 ? "BOSS" : `RND ${run.roundInAnte}/3`;

  $("round-score").textContent = String(run.roundScore);
  $("target-score").textContent = `TARGET ${table.targetScore}`;
  const pct = Math.min(100, (run.roundScore / Math.max(1, table.targetScore)) * 100);
  const fill = $("score-progress-fill");
  fill.style.width = `${pct}%`;
  fill.classList.toggle("cleared", pct >= 100);

  $("coins-pill").textContent = `$${run.coins}`;

  // Balatro-style Chips x Mult
  const ss = table.shotScore;
  $("shot-chips").textContent = String(ss.chips);
  $("shot-mult").textContent = String(ss.mult);
  const preview = $("shot-preview");
  if (ss.previewTotal > 0) {
    preview.textContent = `= ${ss.previewTotal}`;
    preview.classList.remove("hidden");
  } else {
    preview.classList.add("hidden");
  }

  // Flick pips
  const pipsEl = $("flick-pips");
  if (pipsEl.childElementCount !== run.modifiers.maxFlicks) {
    pipsEl.innerHTML = "";
    for (let i = 0; i < run.modifiers.maxFlicks; i++) {
      const pip = document.createElement("span");
      pip.className = "flick-pip";
      pipsEl.appendChild(pip);
    }
  }
  for (let i = 0; i < pipsEl.children.length; i++) {
    pipsEl.children[i].classList.toggle("active", i < run.flicksRemaining);
  }

  // Relics strip
  const relicsEl = $("hud-relics");
  const key = run.modifiers.relics.join(",");
  if (relicsEl.dataset.key !== key) {
    relicsEl.dataset.key = key;
    relicsEl.innerHTML = "";
    for (let slot = 0; slot < TableConstants.MAX_RELIC_SLOTS; slot++) {
      const rid = run.modifiers.relics[slot];
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = rid ? "relic-slot filled" : "relic-slot";
      if (rid) {
        const def = RELICS[rid];
        btn.textContent = def.badge;
        btn.style.background = def.rarity.accent;
        btn.title = `${def.title}: ${def.description}`;
        btn.addEventListener("click", () => {
          inspectedRelicId = inspectedRelicId === rid ? null : rid;
          syncBanners();
        });
      } else {
        btn.textContent = "·";
        btn.disabled = true;
      }
      relicsEl.appendChild(btn);
    }
  }

  syncBanners();

  // Bottom bar controls
  $("control-mode-value").textContent = run.controlMode;
  $("tilt-mode-value").textContent = run.tiltSensitivity.label.toUpperCase();
  const inPlay = table.shotPhase === "IN_PLAY";
  $("btn-bank-shot").classList.toggle("hidden", !inPlay);
  $("control-hint").classList.toggle("hidden", inPlay);
  $("control-hint").textContent =
    run.controlMode === "FLICK"
      ? "Swipe fast to flick puck • Tilt or hold WASD/Arrows to curve"
      : run.controlMode === "SLINGSHOT"
      ? "Pull back anywhere to aim • Tilt or hold WASD/Arrows to curve"
      : "Flick puck or pull back to aim • Tilt or hold WASD/Arrows to curve";
}

function syncBanners() {
  const tooltip = $("relic-tooltip");
  const bossBanner = $("boss-banner");
  const boss = run.tableState.bossModifier;

  if (inspectedRelicId && RELICS[inspectedRelicId]) {
    const r = RELICS[inspectedRelicId];
    tooltip.textContent = `${r.title}: ${r.description} (tap to dismiss)`;
    tooltip.classList.remove("hidden");
    bossBanner.classList.add("hidden");
  } else {
    tooltip.classList.add("hidden");
    if (boss.id !== "NONE") {
      bossBanner.textContent = `BOSS MODIFIER — ${boss.title}: ${boss.description}`;
      bossBanner.classList.remove("hidden");
    } else {
      bossBanner.classList.add("hidden");
    }
  }
}

function renderRoundClear() {
  const s = run.roundClearSummary;
  if (!s) return;
  $("clear-tag").textContent = s.roundInAnte === 3 ? "BOSS TABLE CLEARED" : "TABLE CLEARED";
  $("clear-score").textContent = `${s.finalScore} / ${s.targetScore}`;
  $("payout-base").textContent = `+$${s.baseReward}`;
  $("payout-flicks-label").textContent = `Remaining Flicks (${s.unusedFlicks})`;
  $("payout-flicks").textContent = `+$${s.flickBonus}`;
  $("payout-interest").textContent = `+$${s.interestBonus}`;
  $("payout-total").textContent = `+$${s.totalPayout}`;
}

function renderShop() {
  const [nextAnte, nextRound] = run.computeNextRoundIndex();
  const boss = run.nextRoundBossPreview;
  $("shop-coins").textContent = `$${run.coins}`;

  const previewCard = $("shop-next-preview");
  previewCard.classList.toggle("is-boss", boss.id !== "NONE");
  $("shop-next-label").textContent =
    nextRound === 3
      ? `UPCOMING: ANTE ${nextAnte} BOSS — ${boss.title.toUpperCase()}`
      : `UPCOMING: ANTE ${nextAnte} • ROUND ${nextRound}/3`;

  const descEl = $("shop-next-boss-desc");
  if (boss.id !== "NONE") {
    descEl.textContent = boss.description;
    descEl.classList.remove("hidden");
  } else {
    descEl.classList.add("hidden");
  }
  $("shop-next-target").textContent = String(run.nextRoundTargetPreview());

  $("shop-relics-count").textContent = `ACTIVE RELICS (${run.modifiers.relics.length}/${TableConstants.MAX_RELIC_SLOTS})`;
  $("shop-upgrades-summary").textContent =
    `Flicks: ${run.modifiers.maxFlicks} • +${run.modifiers.chipBonusUpgrade} Chips • +${run.modifiers.multBonusUpgrade} Mult`;

  const ownedWrap = $("shop-owned-relics");
  ownedWrap.innerHTML = "";
  for (let slot = 0; slot < TableConstants.MAX_RELIC_SLOTS; slot++) {
    const rid = run.modifiers.relics[slot];
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "owned-slot-btn";
    if (rid) {
      const def = RELICS[rid];
      btn.classList.add("filled");
      if (selectedShopRelicId === rid) btn.classList.add("selected");
      btn.style.background = `${def.rarity.accent}44`;
      btn.innerHTML = `<span class="owned-slot-badge">${def.badge}</span><span class="owned-slot-name">${def.title}</span>`;
      btn.addEventListener("click", () => {
        selectedShopRelicId = selectedShopRelicId === rid ? null : rid;
        renderShop();
      });
    } else {
      btn.textContent = "EMPTY";
      btn.disabled = true;
    }
    ownedWrap.appendChild(btn);
  }

  const sellBar = $("shop-sell-bar");
  if (selectedShopRelicId && run.modifiers.relics.includes(selectedShopRelicId)) {
    const def = RELICS[selectedShopRelicId];
    const refund = Math.max(2, Math.floor(def.cost / 2));
    $("sell-relic-title").textContent = def.title;
    $("sell-relic-desc").textContent = def.description;
    $("btn-sell-relic").textContent = `SELL +$${refund}`;
    sellBar.classList.remove("hidden");
  } else {
    selectedShopRelicId = null;
    sellBar.classList.add("hidden");
  }

  const grid = $("shop-offers-grid");
  grid.innerHTML = "";
  for (const offer of run.shopOffers) {
    const accent = offer.kind === "RELIC" ? offer.relic.rarity.accent : "#D97706";
    const typeLabel = offer.kind === "RELIC" ? `${offer.relic.rarity.label.toUpperCase()} RELIC` : "TABLE UPGRADE";
    const fullSlots = offer.kind === "RELIC" && run.modifiers.relics.length >= TableConstants.MAX_RELIC_SLOTS;
    const canBuy = !offer.purchased && run.coins >= offer.cost && !fullSlots;

    const card = document.createElement("article");
    card.className = `offer-card${offer.purchased ? " sold" : ""}`;
    card.innerHTML = `
      <div class="offer-top">
        <div class="offer-badge" style="background: ${offer.purchased ? "#334155" : accent}">${offer.badge}</div>
        <div>
          <div class="offer-type" style="color: ${offer.purchased ? "#64748B" : accent}">${typeLabel}</div>
          <h3 class="offer-title">${offer.title}</h3>
        </div>
      </div>
      <p class="offer-desc">${offer.description}</p>
      <button class="offer-buy-btn" type="button" style="background: ${accent}" ${canBuy ? "" : "disabled"}>
        ${offer.purchased ? "SOLD" : fullSlots ? "SLOTS FULL" : `BUY $${offer.cost}`}
      </button>
    `;
    card.querySelector(".offer-buy-btn").addEventListener("click", () => {
      if (run.buyOffer(offer.id)) {
        sound.play("SHOP_BUY");
        renderShop();
        syncHud();
      }
    });
    grid.appendChild(card);
  }

  const rerollBtn = $("btn-reroll");
  rerollBtn.textContent = `REROLL ($${run.shopRerollCost})`;
  rerollBtn.disabled = run.coins < run.shopRerollCost;
}

function renderGameOver() {
  $("gameover-where").textContent =
    `Stopped at Ante ${run.ante} • Round ${run.roundInAnte} (${run.roundScore} / ${run.tableState.targetScore})`;
  $("stat-rounds").textContent = String(run.stats.roundsCleared);
  $("stat-best-shot").textContent = String(run.stats.highestShotScore);
  $("stat-total-score").textContent = String(run.stats.totalScoreAllRounds);
  $("stat-goals").textContent = String(run.stats.goalsScored);
  $("stat-obstacles").textContent = String(run.stats.obstaclesDestroyed);

  const relEl = $("gameover-relics");
  if (run.modifiers.relics.length > 0) {
    relEl.textContent = `RELICS: ${run.modifiers.relics.map((r) => RELICS[r].title).join(" • ")}`;
    relEl.classList.remove("hidden");
  } else {
    relEl.classList.add("hidden");
  }
}

// Pointer Input (Mouse + Touch Flick / Slingshot / Auto)
let activePointer = null;

canvas.addEventListener("pointerdown", (e) => {
  sound.unlock();
  if (run.screen !== "PLAYING" || run.tableState.shotPhase !== "AIMING") return;
  const puck = run.tableState.pucks[0];
  if (!puck) return;

  canvas.setPointerCapture(e.pointerId);
  const startWorld = renderer.screenToWorld(e.clientX, e.clientY);
  activePointer = {
    id: e.pointerId,
    startWorld,
    currentWorld: startWorld,
    startTime: performance.now(),
    lastTime: performance.now(),
    smoothedVel: { x: 0, y: 0 },
    peakVel: { x: 0, y: 0 },
    movedDistance: 0
  };
  aimVisual = {
    active: true,
    isSlingshot: run.controlMode !== "FLICK",
    launchVelocity: { x: 0, y: 0 },
    powerFraction: 0
  };
});

canvas.addEventListener("pointermove", (e) => {
  if (!activePointer || e.pointerId !== activePointer.id) return;
  const puck = run.tableState.pucks[0];
  if (!puck) return;

  const now = performance.now();
  const nextWorld = renderer.screenToWorld(e.clientX, e.clientY);
  const delta = Vec.sub(nextWorld, activePointer.currentWorld);
  const dtSec = Math.max(1, now - activePointer.lastTime) / 1000;

  if (Vec.lenSq(delta) > 0.25) {
    const inst = { x: delta.x / dtSec, y: delta.y / dtSec };
    activePointer.smoothedVel = {
      x: activePointer.smoothedVel.x * 0.45 + inst.x * 0.55,
      y: activePointer.smoothedVel.y * 0.45 + inst.y * 0.55
    };
    if (Vec.lenSq(activePointer.smoothedVel) > Vec.lenSq(activePointer.peakVel)) {
      activePointer.peakVel = { ...activePointer.smoothedVel };
    }
    activePointer.movedDistance += Vec.len(delta);
  }

  activePointer.currentWorld = nextWorld;
  activePointer.lastTime = now;

  const maxSpeed = TableConstants.MAX_LAUNCH_SPEED * run.modifiers.launchSpeedMultiplier;
  const elapsed = now - activePointer.startTime;
  const isQuickUpwardFlick =
    run.controlMode === "FLICK" ||
    (run.controlMode === "AUTO" &&
      elapsed < 240 &&
      Vec.len(activePointer.smoothedVel) > 700 &&
      nextWorld.y < activePointer.startWorld.y - 35);

  let previewVel;
  if (isQuickUpwardFlick) {
    previewVel = Vec.clampLen(Vec.mul(activePointer.smoothedVel, 1.18), maxSpeed);
  } else {
    const pull =
      Vec.dist(activePointer.startWorld, puck.pos) < 150
        ? Vec.sub(puck.pos, nextWorld)
        : Vec.sub(activePointer.startWorld, nextWorld);
    const power = Math.max(0, Math.min(1, Vec.len(pull) / 280));
    previewVel = Vec.mul(Vec.norm(pull), power * maxSpeed);
  }

  aimVisual = {
    active: true,
    isSlingshot: !isQuickUpwardFlick,
    launchVelocity: previewVel,
    powerFraction: Math.max(0, Math.min(1, Vec.len(previewVel) / maxSpeed))
  };
});

function finishPointer(e) {
  if (!activePointer || e.pointerId !== activePointer.id) return;
  const puck = run.tableState.pucks[0];
  const p = activePointer;
  activePointer = null;
  aimVisual = { active: false };
  if (!puck || run.tableState.shotPhase !== "AIMING") return;

  const maxSpeed = TableConstants.MAX_LAUNCH_SPEED * run.modifiers.launchSpeedMultiplier;
  const elapsed = performance.now() - p.startTime;
  const useFlick =
    run.controlMode === "FLICK"
      ? true
      : run.controlMode === "SLINGSHOT"
      ? false
      : elapsed < 250 &&
        Vec.len(p.peakVel) > 620 &&
        (p.currentWorld.y < p.startWorld.y - 24 ||
          (Math.abs(p.currentWorld.x - p.startWorld.x) > 40 && Vec.dist(puck.pos, p.currentWorld) < 90));

  let finalVel;
  if (useFlick) {
    const chosen = Vec.len(p.smoothedVel) > 350 ? p.smoothedVel : p.peakVel;
    finalVel = Vec.clampLen(Vec.mul(chosen, 1.22), maxSpeed);
  } else {
    const pull =
      Vec.dist(p.startWorld, puck.pos) < 150
        ? Vec.sub(puck.pos, p.currentWorld)
        : Vec.sub(p.startWorld, p.currentWorld);
    const power = Math.max(0, Math.min(1, Vec.len(pull) / 280));
    finalVel = Vec.mul(Vec.norm(pull), power * maxSpeed);
  }

  if (p.movedDistance > 18 && Vec.len(finalVel) >= TableConstants.MIN_LAUNCH_SPEED) {
    const events = run.launchShot(finalVel);
    emitSounds(events);
    syncHud();
  }
}

canvas.addEventListener("pointerup", finishPointer);
canvas.addEventListener("pointercancel", finishPointer);

// UI Button Handlers
$("btn-start-run").addEventListener("click", () => {
  sound.unlock();
  tilt.calibrateNeutral();
  inspectedRelicId = null;
  run.startNewRun();
  sound.play("FLICK_LAUNCH");
  syncScreens();
});

$("btn-open-shop").addEventListener("click", () => {
  selectedShopRelicId = null;
  run.openShop();
  syncScreens();
});

$("btn-sell-relic").addEventListener("click", () => {
  if (selectedShopRelicId && run.sellRelic(selectedShopRelicId)) {
    selectedShopRelicId = null;
    sound.play("SHOP_BUY");
    renderShop();
    syncHud();
  }
});

$("btn-reroll").addEventListener("click", () => {
  if (run.rerollShop()) {
    sound.play("SPINNER_HIT");
    renderShop();
  }
});

$("btn-next-round").addEventListener("click", () => {
  inspectedRelicId = null;
  run.startNextRound();
  sound.play("FLICK_LAUNCH");
  syncScreens();
});

$("btn-new-run").addEventListener("click", () => {
  tilt.calibrateNeutral();
  inspectedRelicId = null;
  run.startNewRun();
  sound.play("FLICK_LAUNCH");
  syncScreens();
});

$("btn-to-title").addEventListener("click", () => {
  run.returnToTitle();
  syncScreens();
});

$("btn-control-mode").addEventListener("click", () => {
  run.cycleControlMode();
  syncHud();
});

$("btn-tilt-mode").addEventListener("click", () => {
  run.cycleTiltSensitivity();
  syncHud();
});

$("btn-enable-sensors").addEventListener("click", async () => {
  const ok = await tilt.requestPermission();
  if (ok) {
    tilt.calibrateNeutral();
    $("btn-enable-sensors").classList.add("hidden");
  }
});

$("btn-calibrate").addEventListener("click", () => {
  tilt.calibrateNeutral();
  const tip = $("relic-tooltip");
  tip.textContent = "Tilt neutral angle calibrated!";
  tip.classList.remove("hidden");
  setTimeout(() => {
    if (!inspectedRelicId) tip.classList.add("hidden");
  }, 1400);
});

$("btn-mute").addEventListener("click", () => {
  sound.muted = !sound.muted;
  $("icon-sound-on").classList.toggle("hidden", sound.muted);
  $("icon-sound-off").classList.toggle("hidden", !sound.muted);
});

$("btn-bank-shot").addEventListener("click", () => {
  const prevScreen = run.screen;
  const events = run.recallPucksEarly();
  emitSounds(events);
  if (run.screen !== prevScreen) syncScreens();
  else syncHud();
});

$("relic-tooltip").addEventListener("click", () => {
  inspectedRelicId = null;
  syncBanners();
});

// Main rAF Game Loop
let lastFrameMs = performance.now();
function loop(nowMs) {
  const dt = Math.min(0.05, Math.max(0, (nowMs - lastFrameMs) / 1000));
  lastFrameMs = nowMs;

  const tiltVec = tilt.currentTilt();
  if (run.screen === "PLAYING") {
    const prevScreen = run.screen;
    const events = run.tick(dt, tiltVec);
    if (events.length > 0) emitSounds(events);
    if (run.screen !== prevScreen) {
      syncScreens();
    } else {
      syncHud();
    }
  }

  renderer.render(run.tableState, run.physics, aimVisual, tiltVec, run.tiltSensitivity);
  requestAnimationFrame(loop);
}

syncScreens();
requestAnimationFrame(loop);
