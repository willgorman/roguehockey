import { TableConstants, Vec } from "./engine.js";

function roundRectPath(ctx, x, y, w, h, r) {
  const rad = Math.min(r, w * 0.5, h * 0.5);
  ctx.beginPath();
  ctx.moveTo(x + rad, y);
  ctx.lineTo(x + w - rad, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + rad);
  ctx.lineTo(x + w, y + h - rad);
  ctx.quadraticCurveTo(x + w, y + h, x + w - rad, y + h);
  ctx.lineTo(x + rad, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - rad);
  ctx.lineTo(x, y + rad);
  ctx.quadraticCurveTo(x, y, x + rad, y);
  ctx.closePath();
}

export class TableRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.viewport = { scale: 1, offsetX: 0, offsetY: 0 };
  }

  resize() {
    const rect = this.canvas.getBoundingClientRect();
    const dpr = Math.min(window.devicePixelRatio || 1, 2.5);
    const w = Math.max(1, Math.floor(rect.width * dpr));
    const h = Math.max(1, Math.floor(rect.height * dpr));
    if (this.canvas.width !== w || this.canvas.height !== h) {
      this.canvas.width = w;
      this.canvas.height = h;
    }
    this.dpr = dpr;
    this.cssWidth = rect.width;
    this.cssHeight = rect.height;

    const pad = 6;
    const availW = Math.max(1, rect.width - pad * 2);
    const availH = Math.max(1, rect.height - pad * 2);
    const scale = Math.min(availW / TableConstants.TABLE_WIDTH, availH / TableConstants.TABLE_HEIGHT);
    const offsetX = (rect.width - TableConstants.TABLE_WIDTH * scale) * 0.5;
    const offsetY = (rect.height - TableConstants.TABLE_HEIGHT * scale) * 0.5;
    this.viewport = { scale, offsetX, offsetY };
  }

  screenToWorld(clientX, clientY) {
    const rect = this.canvas.getBoundingClientRect();
    const x = clientX - rect.left;
    const y = clientY - rect.top;
    return {
      x: (x - this.viewport.offsetX) / this.viewport.scale,
      y: (y - this.viewport.offsetY) / this.viewport.scale
    };
  }

  render(state, physics, aimVisual, tiltInput, tiltSensitivity) {
    this.resize();
    const ctx = this.ctx;
    ctx.save();
    ctx.scale(this.dpr, this.dpr);
    ctx.clearRect(0, 0, this.cssWidth, this.cssHeight);

    const shake = state.screenShake * this.viewport.scale * 0.85;
    const shakeX = shake > 0.2 ? Math.sin(state.tableTime * 75) * shake : 0;
    const shakeY = shake > 0.2 ? Math.cos(state.tableTime * 91) * shake : 0;
    const vp = {
      scale: this.viewport.scale,
      offsetX: this.viewport.offsetX + shakeX,
      offsetY: this.viewport.offsetY + shakeY
    };

    const wx = (x) => vp.offsetX + x * vp.scale;
    const wy = (y) => vp.offsetY + y * vp.scale;
    const ws = (v) => v * vp.scale;

    this.drawTableSurface(ctx, state, wx, wy, ws);
    if (state.bossModifier.id === "MAGNETIC_DRIFT") {
      this.drawCrosswind(ctx, state, wx, wy, ws);
    }
    this.drawGoalGate(ctx, state, wx, wy, ws);

    for (const pu of state.powerUps) {
      if (!pu.collected) this.drawPowerUp(ctx, pu, wx, wy, ws);
    }
    for (const t of state.targets) {
      this.drawTarget(ctx, t, wx, wy, ws);
    }
    for (const o of state.obstacles) {
      if (!o.destroyed) this.drawObstacle(ctx, o, wx, wy, ws);
    }

    this.drawShockwaves(ctx, state, wx, wy, ws);
    this.drawParticles(ctx, state, wx, wy, ws);

    if (aimVisual && aimVisual.active && state.shotPhase === "AIMING") {
      this.drawAimPreview(ctx, aimVisual, state, physics, wx, wy, ws);
    }

    for (const puck of state.pucks) {
      this.drawPuck(ctx, puck, state.shotPhase === "AIMING", state.tableTime, wx, wy, ws);
    }

    this.drawFloatingTexts(ctx, state, wx, wy, ws);

    if (tiltSensitivity.id !== "OFF") {
      this.drawTiltBubble(ctx, tiltInput, wx, wy, ws);
    }

    if (state.shotPhase === "SCORING_POPUP" && state.lastBankedScore > 0) {
      this.drawShotBankedBanner(ctx, state, wx, wy, ws);
    }

    ctx.restore();
  }

  drawTableSurface(ctx, state, wx, wy, ws) {
    const outerX = wx(0);
    const outerY = wy(0);
    const outerW = ws(TableConstants.TABLE_WIDTH);
    const outerH = ws(TableConstants.TABLE_HEIGHT);

    // Outer tactile aluminum/slate chassis rail
    const railGrad = ctx.createLinearGradient(0, outerY, 0, outerY + outerH);
    railGrad.addColorStop(0, "#38485e");
    railGrad.addColorStop(1, "#1e293b");
    roundRectPath(ctx, outerX, outerY, outerW, outerH, ws(40));
    ctx.fillStyle = railGrad;
    ctx.fill();
    ctx.strokeStyle = "rgba(203, 213, 225, 0.45)";
    ctx.lineWidth = ws(4);
    ctx.stroke();

    // Inner frosted ice surface
    const m = TableConstants.WALL_MARGIN;
    const inX = wx(m);
    const inY = wy(m);
    const inW = ws(TableConstants.TABLE_WIDTH - m * 2);
    const inH = ws(TableConstants.TABLE_HEIGHT - m * 2);
    const iceGrad = ctx.createRadialGradient(
      wx(500),
      wy(TableConstants.TABLE_HEIGHT * 0.44),
      ws(40),
      wx(500),
      wy(TableConstants.TABLE_HEIGHT * 0.44),
      ws(TableConstants.TABLE_HEIGHT * 0.72)
    );
    iceGrad.addColorStop(0, "#F9FDFF");
    iceGrad.addColorStop(1, state.bossModifier.id === "HEAVY_ICE" ? "#CBE6F8" : "#E1EFF8");

    roundRectPath(ctx, inX, inY, inW, inH, ws(24));
    ctx.fillStyle = iceGrad;
    ctx.fill();

    // Subtle diagonal frosted ice sheen
    ctx.save();
    ctx.clip();
    ctx.fillStyle = "rgba(255, 255, 255, 0.35)";
    ctx.beginPath();
    ctx.moveTo(wx(140), wy(0));
    ctx.lineTo(wx(360), wy(0));
    ctx.lineTo(wx(80), wy(TableConstants.TABLE_HEIGHT));
    ctx.lineTo(wx(-140), wy(TableConstants.TABLE_HEIGHT));
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Rink markings
    const centerY = TableConstants.TABLE_HEIGHT * 0.5;
    ctx.strokeStyle = "rgba(251, 113, 133, 0.24)";
    ctx.lineWidth = ws(6);
    ctx.beginPath();
    ctx.moveTo(wx(m + 8), wy(centerY));
    ctx.lineTo(wx(TableConstants.TABLE_WIDTH - m - 8), wy(centerY));
    ctx.stroke();

    ctx.strokeStyle = "rgba(56, 189, 248, 0.24)";
    ctx.lineWidth = ws(5);
    ctx.beginPath();
    ctx.moveTo(wx(m + 8), wy(470));
    ctx.lineTo(wx(TableConstants.TABLE_WIDTH - m - 8), wy(470));
    ctx.stroke();

    // Dashed launch line
    ctx.save();
    ctx.setLineDash([ws(18), ws(14)]);
    ctx.strokeStyle = "rgba(56, 189, 248, 0.34)";
    ctx.lineWidth = ws(4);
    ctx.beginPath();
    ctx.moveTo(wx(m + 8), wy(TableConstants.LAUNCH_LINE_Y));
    ctx.lineTo(wx(TableConstants.TABLE_WIDTH - m - 8), wy(TableConstants.LAUNCH_LINE_Y));
    ctx.stroke();
    ctx.restore();

    // Center faceoff circle
    ctx.strokeStyle = "rgba(56, 189, 248, 0.22)";
    ctx.lineWidth = ws(5);
    ctx.beginPath();
    ctx.arc(wx(500), wy(centerY), ws(128), 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = "rgba(251, 113, 133, 0.25)";
    ctx.beginPath();
    ctx.arc(wx(500), wy(centerY), ws(16), 0, Math.PI * 2);
    ctx.fill();

    // Launch zone subtle ice tint
    roundRectPath(
      ctx,
      wx(m + 4),
      wy(TableConstants.LAUNCH_LINE_Y),
      ws(TableConstants.TABLE_WIDTH - m * 2 - 8),
      ws(TableConstants.TABLE_HEIGHT - m - TableConstants.LAUNCH_LINE_Y - 4),
      ws(18)
    );
    ctx.fillStyle = "rgba(56, 189, 248, 0.06)";
    ctx.fill();

    // Inner rail crisp border
    roundRectPath(ctx, inX, inY, inW, inH, ws(24));
    ctx.strokeStyle = "rgba(148, 163, 184, 0.48)";
    ctx.lineWidth = ws(3);
    ctx.stroke();
  }

  drawCrosswind(ctx, state, wx, wy, ws) {
    const dir = state.currentWindX >= 0 ? 1 : -1;
    const alpha = Math.max(0.15, Math.min(0.45, Math.abs(state.currentWindX) / 520));
    const drift = (state.tableTime * 180 * dir) % 160;
    ctx.strokeStyle = `rgba(2, 132, 199, ${alpha})`;
    ctx.lineWidth = ws(4);
    ctx.lineCap = "round";

    for (const y of [520, 700, 880]) {
      for (let i = 0; i <= 4; i++) {
        const x = Math.max(80, Math.min(920, 160 + i * 170 + drift));
        const cx = wx(x);
        const cy = wy(y);
        const len = ws(26) * dir;
        const h = ws(14);
        ctx.beginPath();
        ctx.moveTo(cx - len, cy - h);
        ctx.lineTo(cx + len * 0.4, cy);
        ctx.lineTo(cx - len, cy + h);
        ctx.stroke();
      }
    }
  }

  drawGoalGate(ctx, state, wx, wy, ws) {
    const gate = state.goalGate;
    const flash = gate.flashTimer;

    // Goal crease
    ctx.fillStyle = `rgba(16, 185, 129, ${0.13 + flash * 0.35})`;
    ctx.beginPath();
    ctx.arc(wx((gate.xMin + gate.xMax) * 0.5), wy(TableConstants.WALL_MARGIN), ws(gate.width * 0.56), 0, Math.PI);
    ctx.fill();

    // Recessed dark goal pocket
    const gx = wx(gate.xMin);
    const gy = wy(6);
    const gw = ws(gate.width);
    const gh = ws(TableConstants.WALL_MARGIN + 10);
    roundRectPath(ctx, gx, gy, gw, gh, ws(12));
    ctx.fillStyle = "#0F172A";
    ctx.fill();
    ctx.strokeStyle = `rgba(16, 185, 129, ${0.32 + flash * 0.65})`;
    ctx.lineWidth = ws(4);
    ctx.stroke();

    // Goal posts
    ctx.fillStyle = "#10B981";
    for (const px of [gate.xMin, gate.xMax]) {
      ctx.beginPath();
      ctx.arc(wx(px), wy(TableConstants.WALL_MARGIN), ws(11), 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawTarget(ctx, t, wx, wy, ws) {
    const cx = wx(t.pos.x);
    const cy = wy(t.pos.y);
    const r = ws(t.radius * (1 + t.hitFlash * 0.14));

    // Soft tactile drop shadow
    ctx.fillStyle = "rgba(15, 23, 42, 0.15)";
    ctx.beginPath();
    ctx.arc(cx + ws(3), cy + ws(6), r, 0, Math.PI * 2);
    ctx.fill();

    if (t.type === "CHIP_BUMPER") {
      ctx.fillStyle = "#F04E54";
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "rgba(255,255,255,0.92)";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.72, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "rgba(240,78,84,0.16)";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.56, 0, Math.PI * 2);
      ctx.fill();
      this.drawCenteredText(ctx, `+${t.chipValue}`, cx, cy, ws(19), "#DC2626");
    } else if (t.type === "MULT_BUMPER") {
      ctx.fillStyle = "#F59E0B";
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "#FFFBEB";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.74, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = "#F59E0B";
      ctx.lineWidth = ws(3);
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.85, 0, Math.PI * 2);
      ctx.stroke();
      this.drawCenteredText(ctx, `+${t.multValue}x`, cx, cy, ws(21), "#B45309");
    } else if (t.type === "SPINNER_RING") {
      ctx.fillStyle = `rgba(6, 182, 212, ${0.15 + t.hitFlash * 0.3})`;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();

      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(t.spinAngle);
      ctx.setLineDash([ws(22), ws(14)]);
      ctx.strokeStyle = "#0891B2";
      ctx.lineWidth = ws(6);
      ctx.beginPath();
      ctx.arc(0, 0, r * 0.88, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();
      this.drawCenteredText(ctx, "BOOST", cx, cy, ws(13), "#0E7490");
    } else {
      // JACKPOT_STAR
      ctx.fillStyle = "#B45309";
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = "#FEFCE8";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.76, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = "rgba(234, 179, 8, 0.85)";
      ctx.beginPath();
      for (let i = 0; i < 10; i++) {
        const rad = i % 2 === 0 ? r * 0.62 : r * 0.3;
        const ang = -Math.PI * 0.5 + (i * Math.PI) / 5;
        const px = cx + Math.cos(ang) * rad;
        const py = cy + Math.sin(ang) * rad;
        if (i === 0) ctx.moveTo(px, py);
        else ctx.lineTo(px, py);
      }
      ctx.closePath();
      ctx.fill();
      this.drawCenteredText(ctx, `★$${t.coinValue}`, cx, cy, ws(16), "#78350F");
    }

    if (t.hitFlash > 0.05) {
      ctx.strokeStyle = `rgba(255, 255, 255, ${t.hitFlash * 0.65})`;
      ctx.lineWidth = ws(4);
      ctx.beginPath();
      ctx.arc(cx, cy, r * (1.05 + t.hitFlash * 0.15), 0, Math.PI * 2);
      ctx.stroke();
    }
  }

  drawObstacle(ctx, obs, wx, wy, ws) {
    const cx = wx(obs.pos.x);
    const cy = wy(obs.pos.y);

    if (obs.isCircular) {
      const r = ws(obs.radius * (1 + obs.hitFlash * 0.12));
      ctx.fillStyle = "rgba(15, 23, 42, 0.18)";
      ctx.beginPath();
      ctx.arc(cx + ws(3), cy + ws(6), r, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = "#EA580C";
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = "#FFEDD5";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.68, 0, Math.PI * 2);
      ctx.fill();

      ctx.strokeStyle = "#C2410C";
      ctx.lineWidth = ws(4);
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.84, 0, Math.PI * 2);
      ctx.stroke();
      this.drawCenteredText(ctx, "TNT", cx, cy, ws(16), "#9A3412");
      return;
    }

    const w = ws(obs.halfW * 2);
    const h = ws(obs.halfH * 2);
    const x = cx - w * 0.5;
    const y = cy - h * 0.5;
    const cr = ws(12);

    // Shadow
    roundRectPath(ctx, x + ws(3), y + ws(6), w, h, cr);
    ctx.fillStyle = "rgba(15, 23, 42, 0.15)";
    ctx.fill();

    if (obs.type === "ICE_BLOCK") {
      const grad = ctx.createLinearGradient(0, y, 0, y + h);
      grad.addColorStop(0, "#BAE6FD");
      grad.addColorStop(1, "#7DD3FC");
      roundRectPath(ctx, x, y, w, h, cr);
      ctx.fillStyle = grad;
      ctx.fill();

      roundRectPath(ctx, x + ws(4), y + ws(4), Math.max(1, w - ws(8)), Math.max(1, h * 0.4), ws(8));
      ctx.fillStyle = "rgba(255, 255, 255, 0.55)";
      ctx.fill();

      roundRectPath(ctx, x, y, w, h, cr);
      ctx.strokeStyle = "#0284C7";
      ctx.lineWidth = ws(3);
      ctx.stroke();

      if (obs.hp < obs.maxHp) {
        ctx.strokeStyle = "#0369A1";
        ctx.lineWidth = ws(2.6);
        ctx.beginPath();
        ctx.moveTo(cx - w * 0.22, y + ws(4));
        ctx.lineTo(cx + w * 0.05, cy + h * 0.25);
        ctx.lineTo(cx + w * 0.26, y + h - ws(4));
        ctx.stroke();
      }
      this.drawCenteredText(ctx, `${obs.hp}`, cx, cy, ws(18), "#0C4A6E");
    } else {
      const grad = ctx.createLinearGradient(0, y, 0, y + h);
      grad.addColorStop(0, "#64748B");
      grad.addColorStop(1, "#475569");
      roundRectPath(ctx, x, y, w, h, cr);
      ctx.fillStyle = grad;
      ctx.fill();
      ctx.strokeStyle = "#1E293B";
      ctx.lineWidth = ws(3.5);
      ctx.stroke();

      // Rivets
      const ins = ws(9);
      ctx.fillStyle = "#CBD5E1";
      for (const [rx, ry] of [
        [x + ins, y + ins],
        [x + w - ins, y + ins],
        [x + ins, y + h - ins],
        [x + w - ins, y + h - ins]
      ]) {
        ctx.beginPath();
        ctx.arc(rx, ry, ws(3), 0, Math.PI * 2);
        ctx.fill();
      }

      const tag = `${obs.hp}${obs.dropsPowerUp ? " ★" : obs.dropsCoin > 0 ? " $" : ""}`;
      this.drawCenteredText(ctx, tag, cx, cy, ws(17), "#FFFFFF");
    }

    if (obs.hitFlash > 0.05) {
      roundRectPath(ctx, x, y, w, h, cr);
      ctx.fillStyle = `rgba(255, 255, 255, ${obs.hitFlash * 0.55})`;
      ctx.fill();
    }
  }

  drawPowerUp(ctx, pu, wx, wy, ws) {
    const cx = wx(pu.pos.x);
    const cy = wy(pu.pos.y);
    const pulse = 1 + Math.sin(pu.pulsePhase) * 0.09;
    const r = ws(pu.radius * pulse);

    const palette = {
      MULTI_PUCK: ["#0284C7", "x2"],
      MAGNET_PULL: ["#0891B2", "MAG"],
      OVERDRIVE_FIRE: ["#EA580C", "FIRE"],
      GIANT_PUCK: ["#059669", "BIG"],
      SCORE_SURGE: ["#D97706", "2xM"]
    };
    const [accent, label] = palette[pu.type] || ["#0284C7", "UP"];

    ctx.fillStyle = `${accent}33`;
    ctx.beginPath();
    ctx.arc(cx, cy, r * 1.28, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#FFFFFF";
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = accent;
    ctx.lineWidth = ws(4);
    ctx.stroke();

    this.drawCenteredText(ctx, label, cx, cy, ws(14), accent);
  }

  drawPuck(ctx, puck, isAiming, tableTime, wx, wy, ws) {
    if (puck.trail.length >= 2) {
      const trailRgb = puck.isOverdrive ? "234, 88, 12" : puck.isMagnet ? "8, 145, 178" : "2, 132, 199";
      ctx.lineCap = "round";
      for (let i = 1; i < puck.trail.length; i++) {
        const frac = i / puck.trail.length;
        ctx.strokeStyle = `rgba(${trailRgb}, ${frac * 0.34})`;
        ctx.lineWidth = ws(puck.radius * 1.12 * frac);
        ctx.beginPath();
        ctx.moveTo(wx(puck.trail[i - 1].x), wy(puck.trail[i - 1].y));
        ctx.lineTo(wx(puck.trail[i].x), wy(puck.trail[i].y));
        ctx.stroke();
      }
    }

    const cx = wx(puck.pos.x);
    const cy = wy(puck.pos.y);
    const r = ws(puck.radius);

    if (isAiming && puck.isMainPuck) {
      const ringScale = 1.25 + Math.sin(tableTime * 4.2) * 0.12;
      ctx.strokeStyle = "rgba(2, 132, 199, 0.28)";
      ctx.lineWidth = ws(3.5);
      ctx.beginPath();
      ctx.arc(cx, cy, r * ringScale, 0, Math.PI * 2);
      ctx.stroke();
    }

    if (puck.isMagnet) {
      ctx.save();
      ctx.setLineDash([ws(12), ws(10)]);
      ctx.strokeStyle = "rgba(8, 145, 178, 0.42)";
      ctx.lineWidth = ws(3);
      ctx.beginPath();
      ctx.arc(cx, cy, r * 1.55, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();
    }

    if (puck.isOverdrive) {
      ctx.fillStyle = "rgba(234, 88, 12, 0.32)";
      ctx.beginPath();
      ctx.arc(cx, cy, r * 1.32, 0, Math.PI * 2);
      ctx.fill();
    }

    // Tactile drop shadow
    ctx.fillStyle = "rgba(15, 23, 42, 0.24)";
    ctx.beginPath();
    ctx.arc(cx + ws(4), cy + ws(7), r, 0, Math.PI * 2);
    ctx.fill();

    const outer = puck.isMainPuck ? "#1E293B" : "#0369A1";
    const ring = puck.isOverdrive ? "#EA580C" : puck.isGiant ? "#10B981" : puck.isMainPuck ? "#F04E54" : "#06B6D4";

    ctx.fillStyle = outer;
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = ring;
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.74, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = "#FFFFFF";
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.36, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = "rgba(255, 255, 255, 0.36)";
    ctx.lineWidth = ws(2.5);
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.9, 0, Math.PI * 2);
    ctx.stroke();
  }

  drawAimPreview(ctx, aim, state, physics, wx, wy, ws) {
    const puck = state.pucks[0];
    if (!puck || Vec.len(aim.launchVelocity) < TableConstants.MIN_LAUNCH_SPEED * 0.65) return;

    const pts = physics.computeAimTrajectory(puck.pos, aim.launchVelocity, puck.radius, 420 + aim.powerFraction * 420);
    const color = aim.powerFraction > 0.78 ? "#F04E54" : aim.powerFraction > 0.42 ? "#D97706" : "#0284C7";

    if (pts.length >= 2) {
      ctx.save();
      ctx.setLineDash([ws(16), ws(14)]);
      ctx.strokeStyle = color;
      ctx.lineWidth = ws(5);
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(wx(pts[0].x), wy(pts[0].y));
      for (let i = 1; i < pts.length; i++) ctx.lineTo(wx(pts[i].x), wy(pts[i].y));
      ctx.stroke();
      ctx.restore();

      const last = pts[pts.length - 1];
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(wx(last.x), wy(last.y), ws(8), 0, Math.PI * 2);
      ctx.fill();
    }

    if (aim.isSlingshot) {
      const dir = Vec.norm(aim.launchVelocity);
      const len = 60 + aim.powerFraction * 165;
      const tx = puck.pos.x - dir.x * len;
      const ty = puck.pos.y - dir.y * len;

      ctx.strokeStyle = `${color}99`;
      ctx.lineWidth = ws(7);
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.moveTo(wx(puck.pos.x), wy(puck.pos.y));
      ctx.lineTo(wx(tx), wy(ty));
      ctx.stroke();

      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(wx(tx), wy(ty), ws(12), 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawShockwaves(ctx, state, wx, wy, ws) {
    for (const s of state.shockwaves) {
      const alpha = Math.max(0, Math.min(1, s.life / s.maxLife));
      ctx.save();
      ctx.globalAlpha = alpha * 0.65;
      ctx.strokeStyle = s.color;
      ctx.lineWidth = ws(8 * alpha + 2);
      ctx.beginPath();
      ctx.arc(wx(s.pos.x), wy(s.pos.y), ws(s.radius), 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();
    }
  }

  drawParticles(ctx, state, wx, wy, ws) {
    for (const p of state.particles) {
      const alpha = Math.max(0, Math.min(1, p.life / p.maxLife));
      const sz = ws(p.size * alpha);
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.fillStyle = p.color;
      const px = wx(p.pos.x);
      const py = wy(p.pos.y);
      if (p.isShard) {
        ctx.translate(px, py);
        ctx.rotate((p.rotation * Math.PI) / 180);
        ctx.fillRect(-sz, -sz * 0.6, sz * 2, sz * 1.2);
      } else {
        ctx.beginPath();
        ctx.arc(px, py, sz, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();
    }
  }

  drawFloatingTexts(ctx, state, wx, wy, ws) {
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    for (const ft of state.floatingTexts) {
      const alpha = Math.max(0, Math.min(1, (ft.life / ft.maxLife) * 1.4));
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.font = `800 ${ws(26 * ft.scale)}px "Space Grotesk", sans-serif`;
      ctx.lineWidth = ws(6);
      ctx.strokeStyle = "#FFFFFF";
      ctx.strokeText(ft.text, wx(ft.pos.x), wy(ft.pos.y));
      ctx.fillStyle = ft.color;
      ctx.fillText(ft.text, wx(ft.pos.x), wy(ft.pos.y));
      ctx.restore();
    }
  }

  drawTiltBubble(ctx, tiltInput, wx, wy, ws) {
    const cx = wx(TableConstants.TABLE_WIDTH - 96);
    const cy = wy(TableConstants.TABLE_HEIGHT - 96);
    const r = ws(36);

    ctx.fillStyle = "rgba(15, 23, 42, 0.08)";
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = "rgba(100, 116, 139, 0.45)";
    ctx.lineWidth = ws(2.5);
    ctx.stroke();

    ctx.strokeStyle = "rgba(148, 163, 184, 0.5)";
    ctx.lineWidth = ws(1.8);
    ctx.beginPath();
    ctx.moveTo(cx - r * 0.55, cy);
    ctx.lineTo(cx + r * 0.55, cy);
    ctx.moveTo(cx, cy - r * 0.55);
    ctx.lineTo(cx, cy + r * 0.55);
    ctx.stroke();

    const active = Vec.len(tiltInput) > 0.14;
    ctx.fillStyle = active ? "#0284C7" : "#64748B";
    ctx.beginPath();
    ctx.arc(cx + tiltInput.x * r * 0.72, cy + tiltInput.y * r * 0.72, ws(10), 0, Math.PI * 2);
    ctx.fill();
  }

  drawShotBankedBanner(ctx, state, wx, wy, ws) {
    const cx = wx(500);
    const cy = wy(TableConstants.TABLE_HEIGHT * 0.48);
    const bw = ws(540);
    const bh = ws(148);

    roundRectPath(ctx, cx - bw * 0.5, cy - bh * 0.5, bw, bh, ws(24));
    ctx.fillStyle = "rgba(15, 23, 42, 0.90)";
    ctx.fill();
    ctx.strokeStyle = "#F59E0B";
    ctx.lineWidth = ws(4);
    ctx.stroke();

    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.font = `700 ${ws(31)}px "Space Grotesk", sans-serif`;
    ctx.fillStyle = "#CBD5E1";
    ctx.fillText(`${state.lastBankedChips} × ${state.lastBankedMult}`, cx, cy - ws(26));

    ctx.font = `800 ${ws(52)}px "Space Grotesk", sans-serif`;
    ctx.fillStyle = "#FBBF24";
    ctx.fillText(`+${state.lastBankedScore}`, cx, cy + ws(28));
  }

  drawCenteredText(ctx, text, x, y, sizePx, color) {
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.font = `800 ${sizePx}px "Space Grotesk", sans-serif`;
    ctx.fillStyle = color;
    ctx.fillText(text, x, y + sizePx * 0.04);
  }
}
