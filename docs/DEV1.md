# DEV1 — Unity feasibility slice (0.1.0-dev.1, versionCode 10)

Authorized by the owner on 2026-10-03 (Slack: "check my-veloren, start working, build an APK and
push"; confirmed: Unity Android port like LibreQuake). Branch `feat/dev1-unity-feasibility`.

## Scope delivered

| Area | What exists | Fidelity note |
|---|---|---|
| Content | `tools/fetch_assets.py` fetches 26 real `.vox` files from upstream pin `585a91b4a76fcf5df7a4851127cf5907a3ce34df` → `game/Assets/Veloren/Resources/Vox/`; `assets-manifest.json` (path, bytes, sha256) | Sample only; full corpus import = dev2 |
| VOX | `Vox.cs`: MagicaVoxel parser (SIZE/XYZI/RGBA) + face-culled mesher with vertex colours | No greedy meshing / AO yet |
| Character | `Humanoid.cs`: Human Male assembled from upstream head/eyes/hair/chest/belt/pants/hands/feet + starter sword, offsets from `humanoid_*_manifest.ron`, skin/hair/eye palette recolour, grey-tint (`recolor_grey`) | Procedural walk/attack simplified vs. Veloren `anim` crate |
| World | `World.cs`: 8×8 chunks × 32 (256²), seeded heightmap (seed 1337), water level 14, up to 140 upstream trees (oak/pine/temperate), MeshColliders | **Not** Veloren's `world` crate; placeholder worldgen |
| Controls | `PlayerController.cs`: touch joystick, drag-look, JUMP/ATK buttons, keyboard, follow camera | — |
| Build | `game/Assets/Veloren/Editor/VelBuild.cs` + `tools/build.py compile|playtest|android` | — |

## Toolchain

Unity 2022.3.62f3 (96770f904ca7), Built-in RP, IL2CPP, ARM64 only, OpenGLES3, minSdk 26,
targetSdk 34, JDK 11, NDK 23.1.7779620 (Unity Android module), package `com.ayoub.myveloren`.
Debug-signed APK (no custom keystore in dev1).

## Evidence (code commit 2f28f9f, clean tree at build time)

| Gate | Command | Result |
|---|---|---|
| Script compile | `python3 tools/build.py compile` | **PASS** — `[VelBuild] compile ok`, 0 `error CS` |
| Android APK | `VEL_VERSION_CODE=10 python3 tools/build.py android` | **PASS** — `result=Succeeded errors=0`; APK 10,095,353 bytes; sha256 `b2af484b9c86972ed32560073e1f3f53fcb41def1bf9e292cd8be0212a647f6f`; contains `lib/arm64-v8a/libil2cpp.so` |
| Linux player boot (xvfb, llvmpipe) | `python3 tools/build.py playtest` | **PASS (boot only)** — `[MyVeloren] boot version=0.1.0-dev.1 chunks=64 trees=140 parts=11`, 0 exceptions in player.log |
| Linux autopilot (walk/jump/attack) + screenshots | same | **NOT COMPLETED** — sandbox restarted mid-run (llvmpipe is very slow); re-run next session |
| Real Android device (Firebase Test Lab) | — | **NOT RUN** — awaiting owner go-ahead (quota) |
| Visual review of character orientation/assembly | — | **NOT RUN** |

## Known gaps → next

1. Run the Firebase Test Lab smoke (e3q / r8q) with the APK and record fps + screenshots.
2. Finish the Linux autopilot and review screenshots (humanoid assembly, sword grip).
3. dev2: full pinned asset corpus import (4,752 `.vox`, 898 `.png`, 639 `.ogg`, 2,855 `.ron`) with provenance.
4. Real Veloren terrain rules, enemies/AI, inventory, persistence, audio — see `PARITY-MATRIX.md`.
