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
