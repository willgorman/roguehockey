// DeviceOrientation tilt sensor controller + smooth keyboard tilt fallback for desktop

export class TiltController {
  constructor() {
    this.rawX = 0;
    this.rawY = 0;
    this.zeroX = 0;
    this.zeroY = 0;
    this.filteredX = 0;
    this.filteredY = 0;
    this.sensorActive = false;
    const isTouchDevice =
      typeof window !== "undefined" && ("ontouchstart" in window || (navigator.maxTouchPoints || 0) > 0);
    this.needsPermission =
      isTouchDevice &&
      typeof DeviceOrientationEvent !== "undefined" &&
      typeof DeviceOrientationEvent.requestPermission === "function";

    this.keys = new Set();
    this.onOrientation = this.onOrientation.bind(this);
    this.onKeyDown = (e) => {
      if (["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "KeyW", "KeyA", "KeyS", "KeyD"].includes(e.code)) {
        this.keys.add(e.code);
      }
    };
    this.onKeyUp = (e) => {
      this.keys.delete(e.code);
    };
  }

  start() {
    window.addEventListener("keydown", this.onKeyDown);
    window.addEventListener("keyup", this.onKeyUp);
    if (!this.needsPermission && typeof DeviceOrientationEvent !== "undefined") {
      window.addEventListener("deviceorientation", this.onOrientation);
      this.sensorActive = true;
    }
  }

  async requestPermission() {
    if (!this.needsPermission) return true;
    try {
      const state = await DeviceOrientationEvent.requestPermission();
      if (state === "granted") {
        window.addEventListener("deviceorientation", this.onOrientation);
        this.sensorActive = true;
        this.needsPermission = false;
        return true;
      }
    } catch {
      return false;
    }
    return false;
  }

  calibrateNeutral() {
    this.zeroX = this.rawX;
    this.zeroY = this.rawY;
    this.filteredX = 0;
    this.filteredY = 0;
  }

  onOrientation(ev) {
    if (ev.gamma === null || ev.beta === null) return;
    // gamma: left-right tilt (-90..90), beta: front-back tilt (-180..180)
    const gx = Math.max(-1.5, Math.min(1.5, ev.gamma / 32));
    const gy = Math.max(-1.5, Math.min(1.5, ev.beta / 32));
    this.rawX = gx;
    this.rawY = gy;

    const adjX = this.applyDeadzone(Math.max(-1, Math.min(1, gx - this.zeroX)));
    const adjY = this.applyDeadzone(Math.max(-1, Math.min(1, gy - this.zeroY)));
    const alpha = 0.22;
    this.filteredX += (adjX - this.filteredX) * alpha;
    this.filteredY += (adjY - this.filteredY) * alpha;
  }

  currentTilt() {
    let kx = 0;
    let ky = 0;
    if (this.keys.has("ArrowLeft") || this.keys.has("KeyA")) kx -= 0.85;
    if (this.keys.has("ArrowRight") || this.keys.has("KeyD")) kx += 0.85;
    if (this.keys.has("ArrowUp") || this.keys.has("KeyW")) ky -= 0.85;
    if (this.keys.has("ArrowDown") || this.keys.has("KeyS")) ky += 0.85;

    return {
      x: Math.max(-1, Math.min(1, this.filteredX + kx)),
      y: Math.max(-1, Math.min(1, this.filteredY + ky))
    };
  }

  applyDeadzone(v) {
    const dz = 0.06;
    if (Math.abs(v) < dz) return 0;
    return ((Math.abs(v) - dz) / (1 - dz)) * (v > 0 ? 1 : -1);
  }
}
