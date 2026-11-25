// Procedural Web Audio API synthesizer & browser haptics

export class SoundEngine {
  constructor() {
    this.ctx = null;
    this.muted = false;
  }

  unlock() {
    if (typeof window === "undefined") return;
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    if (!this.ctx) this.ctx = new AudioCtx();
    if (this.ctx.state === "suspended") {
      this.ctx.resume().catch(() => {});
    }
  }

  play(event, comboStep = 1) {
    this.vibrate(event);
    if (this.muted) return;
    this.unlock();
    if (!this.ctx) return;

    const boost = 1 + (Math.max(1, Math.min(10, comboStep)) - 1) * 0.06;
    switch (event) {
      case "FLICK_LAUNCH":
        this.sweep(210, 440, 0.085, 0.16);
        break;
      case "WALL_BOUNCE":
        this.percussive(260, 0.045, 0.11, 0.25);
        break;
      case "CHIP_HIT":
        this.tone(540 * boost, 0.08, 0.14);
        break;
      case "MULT_HIT":
        this.chord([660 * boost, 990 * boost], 0.12, 0.13);
        break;
      case "SPINNER_HIT":
        this.sweep(480, 920, 0.1, 0.13);
        break;
      case "JACKPOT_HIT":
        this.chord([784, 988, 1175], 0.18, 0.15);
        break;
      case "OBSTACLE_CRACK":
        this.percussive(380, 0.06, 0.15, 0.55);
        break;
      case "OBSTACLE_SHATTER":
        this.percussive(220, 0.14, 0.18, 0.72);
        break;
      case "BARREL_EXPLODE":
        this.explosion(0.26, 0.24);
        break;
      case "POWERUP_PICKUP":
        this.arpeggio([523, 659, 784, 1046], 0.2, 0.14);
        break;
      case "GOAL_SCORE":
        this.arpeggio([587, 880, 1175], 0.26, 0.18);
        break;
      case "SHOT_BANKED":
        this.chord([440, 554, 659], 0.16, 0.13);
        break;
      case "ROUND_CLEAR":
        this.arpeggio([523, 659, 784, 1046, 1318], 0.4, 0.17);
        break;
      case "SHOP_BUY":
        this.arpeggio([740, 988], 0.12, 0.13);
        break;
      case "GAME_OVER":
        this.arpeggio([392, 330, 262], 0.36, 0.15);
        break;
      default:
        break;
    }
  }

  vibrate(event) {
    if (typeof navigator === "undefined" || !navigator.vibrate) return;
    const ms =
      event === "BARREL_EXPLODE"
        ? 45
        : event === "GOAL_SCORE"
        ? 35
        : event === "OBSTACLE_SHATTER"
        ? 18
        : event === "FLICK_LAUNCH"
        ? 10
        : 0;
    if (ms > 0) {
      try {
        navigator.vibrate(ms);
      } catch {
        // Ignore browser vibration restrictions
      }
    }
  }

  tone(freq, dur, vol) {
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = "triangle";
    osc.frequency.setValueAtTime(freq, now);
    gain.gain.setValueAtTime(vol, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + dur);
    osc.connect(gain).connect(this.ctx.destination);
    osc.start(now);
    osc.stop(now + dur);
  }

  sweep(f1, f2, dur, vol) {
    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(f1, now);
    osc.frequency.exponentialRampToValueAtTime(f2, now + dur);
    gain.gain.setValueAtTime(vol, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + dur);
    osc.connect(gain).connect(this.ctx.destination);
    osc.start(now);
    osc.stop(now + dur);
  }

  chord(freqs, dur, vol) {
    const per = vol / freqs.length;
    for (const f of freqs) this.tone(f, dur, per);
  }

  arpeggio(freqs, totalDur, vol) {
    const step = totalDur / freqs.length;
    const now = this.ctx.currentTime;
    freqs.forEach((f, idx) => {
      const start = now + idx * step;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = "triangle";
      osc.frequency.setValueAtTime(f, start);
      gain.gain.setValueAtTime(vol, start);
      gain.gain.exponentialRampToValueAtTime(0.001, start + step * 1.35);
      osc.connect(gain).connect(this.ctx.destination);
      osc.start(start);
      osc.stop(start + step * 1.4);
    });
  }

  percussive(freq, dur, vol, noiseMix) {
    this.tone(freq, dur, vol * (1 - noiseMix));
    this.noiseBurst(dur, vol * noiseMix, 1200);
  }

  explosion(dur, vol) {
    this.sweep(150, 42, dur, vol * 0.7);
    this.noiseBurst(dur, vol * 0.65, 550);
  }

  noiseBurst(dur, vol, cutoffHz) {
    const len = Math.max(1, Math.floor(this.ctx.sampleRate * dur));
    const buf = this.ctx.createBuffer(1, len, this.ctx.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < len; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 1.8);
    }
    const src = this.ctx.createBufferSource();
    src.buffer = buf;
    const filter = this.ctx.createBiquadFilter();
    filter.type = "lowpass";
    filter.frequency.value = cutoffHz;
    const gain = this.ctx.createGain();
    gain.gain.value = vol;
    src.connect(filter).connect(gain).connect(this.ctx.destination);
    src.start();
  }
}
