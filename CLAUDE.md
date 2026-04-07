# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run the game (desktop, from project root)
./gradlew lwjgl3:run

# Build a runnable JAR
./gradlew lwjgl3:jar
# Output: lwjgl3/build/libs/Penitent-<version>.jar

# Build only
./gradlew build

# Clean
./gradlew clean
```

The working directory when running is `assets/` (set in `lwjgl3/build.gradle`). All asset paths in code are relative to `assets/`.

There are no automated tests in this project (`./gradlew test` will run but find nothing).

## Architecture Overview

**Penitent** is a 2D side-scrolling action game built with [libGDX](https://libgdx.com/). It is inspired by Blasphemous. The project follows the standard libGDX multi-module Gradle layout:

- `core/` — all game logic (shared across platforms)
- `lwjgl3/` — desktop launcher (LWJGL3 backend)
- `assets/` — all game assets (tilemaps, sprites, audio, fonts), served as the working directory at runtime

### Screen flow

```
PenitentGame (extends Game)
  └─ MenuScreen   ←→   GameScreen
```

`PenitentGame` is the entry point. It holds the active save slot and global volume settings that persist between screens. Screen transitions go through `PenitentGame.showMenu()`, `startNewGame(slot)`, `continueGame(slot)`, and `reloadActiveSlot()`.

### GameScreen

The main game loop. Key responsibilities:
- Loads all three Tiled maps (`mapa.tmx`, `mapa_2.tmx`, `mapa_3.tmx`) upfront in `show()`.
- Tracks `currentMap` (1/2/3) and switches between them via `checkMapTransitions()` wrapped in a black fade (`FadeState` enum: `NONE → FADE_OUT → FADE_IN`).
- Map 1 ↔ Map 2: horizontal border crossing. Map 2 → Map 3: player falls through a hole (`player.y < -20`). Map 3 → Map 2: any border exit.
- Maintains separate enemy lists per map: `enemiesMap1`/`enemiesMap2` (`Enemy` or `SkeletonEnemy`) and `batsMap3` (`BatEnemy`). Enemies reset lazily: a `pendingResetMapX` flag triggers reset on the next map departure, so the current map's enemies persist until you leave.
- Combat uses a `hitThisAttack` set to avoid hitting the same enemy multiple times per swing.
- Checkpoint activation heals the player to full, saves to `SaveManager`, and immediately resets enemies on all *other* maps.
- HUD uses a separate `hudCam` (fixed to screen space) for hearts and text overlays.

### Player controls

| Key | Action |
|-----|--------|
| A / D | Move left / right |
| Space | Jump (only on ground) |
| J | Attack (press again during first attack to queue combo) |
| K | Dash (only on ground) |
| B | Interact with checkpoint (when in range) |
| ESC | Pause |

### Enemy types

- **`Enemy`** — ground patrol enemy with a 4-state AI (`PATROL → CHASE → ATTACK → RETURN`). Patrols between `leftLimit`/`rightLimit`. Chases when player is within 250 px, leashes at 380 px. Health: 2 hits.
- **`BatEnemy`** — flying enemy. Starts sleeping (`SLEEP` state), wakes when player is within 180 px, chases freely in 2D. Alternates between two attack animations. Health: 2 hits. No gravity/collision layer needed.
- **`SkeletonEnemy`** — ground patrol enemy with the same 4-state AI. Chases at 180 px, leashes at 260 px. Attack uses a chain with a precise damage window (frames 10–14 of a 25-frame animation). Health: 4 hits, deals 2 hearts of damage per hit. Uses variable-width sprite sheets (64 px idle/move/hurt, 146 px attack, 118 px die).

All enemy types expose public boolean event flags (`eventDeath`, `eventAttackStart`, `eventMoving`) that `GameScreen` reads each frame to trigger audio, then resets to `false`. `Enemy` also has `eventHit`.

### Save system

`SaveManager` writes JSON files (`save_slot_0.json`, `save_slot_1.json`, `save_slot_2.json`) to the libGDX local storage directory (next to the JAR / in the working directory during development). `SaveData` stores: slot, map number, player X/Y, health, zone name, and timestamp. Max 3 slots. Zone names: Map 1 = "Las Entrañas", Map 2 = "El Osario", Map 3 = "Las Catacumbas".

### Key singleton-style managers

- **`FontManager`** — static fields (`title`, `menu`, `small`) holding `BitmapFont` instances loaded from `assets/fonts/`. Call `FontManager.load()` once at startup (in `PenitentGame.create()`) and `FontManager.dispose()` on exit.
- **`SoundManager`** — instantiated per-screen (both `MenuScreen` and `GameScreen` own one). Manages looping music tracks per map and one-shot SFX. `musicMap2` reuses the same `Music` object as `musicMap1` — do not dispose it twice.

### Checkpoint

`Checkpoint` represents a save altar (totem). It has two states: inactive (greyscale) and activated (golden glow). The player must be within 48 px and press B to activate. Activation is handled by `Checkpoint.update()` returning `true`, which `GameScreen` catches to trigger heal + save + enemy reset.

### Asset conventions

- Player sprites: `assets/player/*.png` — horizontal sprite sheets, 120×80 px per frame, single row.
- Enemy sprites: `assets/enemies/enemy_*.png` — 99×46 px per frame.
- Skeleton sprites: `assets/enemies/skeleton/skeleton*.png` — 64×64 px (idle/move/hurt), 146×64 px (attack), 118×64 px (die).
- Bat sprites: `assets/enemies/bat/Bat-*.png` — 64×64 px per frame.
- Checkpoint sprite: `assets/totem_checkpoint.png`
- Tilemaps: Tiled `.tmx` files in `assets/`. Each map has a layer named `"suelo"` used as the collision layer.
- Audio: `assets/audio/*.ogg`
- Fonts: `assets/fonts/cinzel_*.fnt` + matching `.png` atlas

### Viewport

All screens use a fixed logical resolution of **608×320** pixels with an `OrthographicCamera`. Tile size is 32 px. The camera smoothly follows the player with lerp (`CAM_LERP = 3.5f`), clamped to map bounds.
