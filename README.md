# RogueHockey

A **native Android portrait tablet rogue-lite air hockey game** built with Kotlin, Jetpack Compose, and a custom fixed-timestep 2D physics engine.

## Highlights

- **Tactile Modern Tabletop Aesthetic**: Bright frosted-ice rink with subtle markings, soft drop shadows, bold arcade targets, cracked ice blocks, riveted steel crates, glowing power-ups, and procedural audio + haptics.
- **Touch Flick + Tilt Controls**:
  - **Auto / Flick / Slingshot Input**: Flick directly to launch like real air hockey, or pull back for a crisp 1-bounce wall-reflection aim preview.
  - **Tablet Tilt Physics**: Tilt the tablet mid-shot (`Gravity` / `Accelerometer` sensor) to bend shots around glacial walls and into the top goal pocket. Includes a live tilt level bubble and one-tap **Neutral Angle Calibration** for playing flat on a desk or angled in your lap.
- **Balatro-Style Score Attack Loop (`Chips × Mult`)**:
  - Clear escalating Target Scores across 3-round **Antes** culminating in **Boss Tables** (*Glacial Wall*, *Crosswind Jets*, *Narrow Goal*, *Frost Drag*, *Hyper Bumpers*).
  - Earn **Coins (`$`)** from clearing tables, saving flicks, jackpot stars, crates, and holding **Interest** (`+$1` per `$5` held).
  - Visit the **Pro Shop** between rounds to draft up to 5 **Relics** (12 distinct build-defining synergies like *Bank Shot Pro*, *Top Shelf*, *Twin Launch*, *Icebreaker*, *Cryo Blast*) and permanent **Table Upgrades**.
- **Rich Table Elements**:
  - **Targets**: Chip Bumpers, Mult Bumpers, kinetic Spinner Boost Rings, moving Jackpot Stars, and the recessed Top Goal Gate.
  - **Destructible Obstacles**: Multi-HP Ice Blocks, loot-dropping Steel Crates, and chain-reacting Explosive Cryo-Barrels.
  - **On-Table Power-Ups**: Multi-Puck, Target Magnet, Overdrive Fire (pierces 1-HP ice blocks), Giant Puck, and Score Surge (`x2 Mult`).

## Building & Testing

```bash
# Run physics & rogue-lite progression unit tests
./gradlew testDebugUnitTest

# Build installable Android Debug APK
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and can be sideloaded onto an Android tablet via `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Play in Browser (Web Edition)

A full-featured HTML5 Canvas web edition lives in `web/` and is automatically deployed to **GitHub Pages** via `.github/workflows/deploy-pages.yml`:

- **Play Online**: https://willgorman.github.io/roguehockey/
- Supports touch flicking, mouse slingshot aiming, mobile/tablet `DeviceOrientation` tilt steering, desktop `WASD`/Arrow tilt steering, and Web Audio procedural sound effects.
- Run web engine unit tests locally with `node --test web/test/engine.test.mjs`.

## Automated APK Releases

Every push to `main` (when Android files change), any `v*` tag, or manual workflow dispatch runs `.github/workflows/release-apk.yml`, which executes the unit tests, builds the Android APK, and publishes it to GitHub Releases.
