# DEV2 — Native Veloren on Android (ADR-002 + live handoff)

## ADR-002 (owner decision, 2026-10-03)

The owner tried the dev1 Unity APK, did not like it, and asked for **"the world itself and
everything in Veloren, all game resources converted to Android (likely > 500 MB), then test it on
Firebase"**. Re-implementing Veloren's world/AI/combat in Unity C# would take months and still
only approximate it. Decision: build **upstream Veloren itself (Rust: voxygen client + embedded
singleplayer server)** for Android ARM64, rendering via wgpu (Vulkan/GLES), with the **complete
asset tree** packed in the APK. This supersedes the "Unity is the Android host" rule in
AGENTS.md §2 for this line of work. The Unity slice (dev1, `game/`) stays as an archived experiment.

Offline: singleplayer runs the server in-process; no account/login/network required.

## Pins / toolchain

| Item | Value |
|---|---|
| Upstream | veloren `585a91b4a76fcf5df7a4851127cf5907a3ce34df` (GitHub mirror) |
| Rust | `nightly-2026-06-13` (upstream rust-toolchain) + `aarch64-linux-android` |
| NDK | r27c, API 26+ |
| Key crates | winit 0.30.12 (needs `android-native-activity`), wgpu 27, shaderc 0.10 |

## Plan (each step pushed when done)

1. Toolchain + upstream checkout (sandbox has 17 usable cores although `nproc` says 1).
2. Host (x86_64 Linux) release build of voxygen to prove the pin builds.
3. Android patches (kept as `native/patches/*.patch` against the pin):
   - voxygen as `cdylib` with `android_main` (winit android-native-activity);
   - userdata/settings/saves → app internal storage; assets → extracted APK assets dir;
   - disable/adjust desktop-only deps (clipboard, dialogs, gamepad, discord, etc.);
   - touch controls (virtual stick + buttons) mapped to existing input actions.
4. World: pre-generate the singleplayer world map on the build host and ship it, because
   generating a full world on a phone may take far too long.
5. Package APK (cargo-apk2 / custom Gradle-less packager), full `assets/` included.
6. Firebase Test Lab (e3q / r8q) smoke: launch, reach singleplayer world, screenshots.

## Status log

| Date | Step | Result |
|---|---|---|
| 2026-10-03 | ADR + branch `feat/dev2-native-android` | done |
| 2026-10-03 | Toolchain (rustup nightly-2026-06-13 + aarch64-linux-android, NDK r27c, cmake/ninja) | done |
| 2026-10-03 | Android cross-build of `veloren-voxygen` lib (`release-thinlto`, features `singleplayer,simd,shaderc-from-source`) | **PASS** — `libveloren_voxygen.so` 477 MB unstripped, exports `android_main` + `ANativeActivity_onCreate`; NEEDED libc++_shared, liblog, libandroid, libaaudio |
| 2026-10-03 | Fixes needed: winit `android-native-activity`; mumble-link off on Android; shaderc cmake must get `ANDROID_ABI=arm64-v8a` (wrapper `native/android-arm64.toolchain.cmake`, else armeabi-v7a objects → link error) | done |
| 2026-10-03 | APK packaging (`native/build_android.sh`: aapt2 + full `assets/` as uncompressed `assets/assets.tar` + zipalign + apksigner) | **PASS** — `my-veloren-dev2.apk` 465,945,142 bytes, sha256 `ce43b4f685c0682425a60326c29d807092a2504d319efe0a5897061fb59b40a2`, vc20 |
| 2026-10-03 | Test Lab attempt 1 (r8q + e3q) | rejected at validation: `NO_CODE_APK` (pure NativeActivity has no dex; no quota used) |
| 2026-10-03 | Added Java shim `VelorenActivity extends NativeActivity` (classes.dex): game-loop intent → `files/autostart` marker → autostart singleplayer (default official world map `world.map.veloren_0_18_0_0`), auto-create character "Ayoub" (starter sword), enter world, autopilot walk/jump/attack/turn; `VEL-STAT fps=` logged every 5 s | built: APK 465,937,007 bytes, sha256 `62505b7bbf10acdfdba81d9bfdff3d41f92f86fc84f7525f3dace13a7b12a413` |
| 2026-10-03 | Test Lab attempt 2 game-loop 15 min r8q (A13) + e3q (A16) | running |

## How to rebuild

```bash
git clone https://github.com/veloren/veloren /work/native/veloren && cd /work/native/veloren
git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
git apply <repo>/native/patches/0001-voxygen-android.patch
# toolchain: see native/env.example.sh (+ native/android-arm64.toolchain.cmake)
<repo>/native/build_android.sh          # -> native/out/my-veloren-dev2.apk
FIREBASE_SA_JSON=... tools/ftl.sh native/out/my-veloren-dev2.apk dev2-r8q
```

## Android code map (patch 0001)

- `voxygen/src/android.rs`: `android_main`, logcat redirect (tag `veloren`), first-launch extraction of
  `assets.tar` → `<internal>/veloren/assets` (marker `.assets-<ver>`), env `VELOREN_ASSETS/USERDATA`,
  `VOXYGEN_CONFIG/LOGS`, event loop pumped until `Resumed`, touch mapping:
  menus = 1-finger mouse; session = left 40% virtual stick (WASD), right side drag = camera,
  right edge: Primary (middle), Jump (bottom-right), Roll (left of Jump), Escape (top-right corner).
- `voxygen/src/android_main_body.rs`: copy of `main.rs` body without CLI parsing.
- `run.rs`: touch events translated before the normal pipeline. `window.rs`: android event loop.
- Known gaps: surface loss on app pause/resume not handled yet; no on-screen drawing of touch zones yet.
