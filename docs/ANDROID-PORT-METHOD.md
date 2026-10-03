# Android port method (native Rust game → APK → Firebase Test Lab)

How dev2 ports upstream **Veloren Voxygen** to Android without rewriting it. The same recipe
works for other Rust/winit/wgpu games (it is the LibreQuake approach applied to Rust).
Live status and evidence: [DEV2-NATIVE.md](DEV2-NATIVE.md). Owner decision: ADR-002 there.

## 1. Principle

Do not re-implement the game. Cross-compile the **real upstream client** (with the embedded
singleplayer server) to `aarch64-linux-android` as a `cdylib`, host it in a `NativeActivity`,
ship the **complete upstream `assets/` tree** inside the APK and keep upstream code changes
small and isolated in one patch (`native/patches/0001-voxygen-android.patch`).

- World: a new singleplayer world without generation options uses the official pre-generated
  map `assets/world/map/veloren_0_18_0_0.bin` (16 MB) → identical world to desktop, no on-device worldgen.
- Fully offline: embedded server, no login, no download at first launch.

## 2. Toolchain (one-time, ~10 min)

| Piece | Version / location |
|---|---|
| Upstream | `veloren/veloren` @ `585a91b4a76fcf5df7a4851127cf5907a3ce34df` cloned at `$VELOREN_SRC` |
| Rust | upstream `rust-toolchain` (`nightly-2026-06-13`) + target `aarch64-linux-android` (rustup) |
| NDK | r27c (27.2.12479018), API 26 clang wrappers |
| cmake + ninja | for `shaderc-sys` (built from source) |
| Android SDK | build-tools 34.0.0, platforms/android-34 (aapt2, d8, zipalign, apksigner) |
| JDK | 17 (javac for the small Java shim) |

Environment: copy `native/env.example.sh` → `/work/native/env.sh` and adjust paths.
**Gotcha:** `shaderc-sys` cmake defaults to `armeabi-v7a` → link errors. Fix = wrapper toolchain
`native/android-arm64.toolchain.cmake` (forces `ANDROID_ABI=arm64-v8a`, `android-26`) exported via
`CMAKE_TOOLCHAIN_FILE_aarch64_linux_android`.

## 3. Upstream patch (what and why)

Apply: `cd $VELOREN_SRC && git apply /path/to/my-veloren/native/patches/0001-voxygen-android.patch`.

| File | Change |
|---|---|
| `voxygen/Cargo.toml` | `[lib] crate-type = ["rlib","cdylib"]`, name `veloren_voxygen`; android deps: winit `android-native-activity`, `libc`, `tar`; `mumble-link` excluded on android |
| `voxygen/src/android.rs` | `android_main`: stdout/stderr → logcat pipe, panic hook, extracts `assets.tar` once to internal storage, sets `VELOREN_ASSETS/USERDATA`, `VOXYGEN_CONFIG/LOGS`, `HOME`, `XDG_*`, `VOXYGEN_SCREENSHOT`; touch → mouse/keys translation; Test Lab autopilot + `VEL-STAT` fps log |
| `voxygen/src/android_main_body.rs` | upstream `main.rs` body without CLI parsing |
| `voxygen/src/run.rs`, `window.rs` | Android event loop (wait for `Resumed` + native window), per-event translation |
| `voxygen/src/menu/main`, `menu/char_selection` | `VELOREN_ANDROID_AUTOSTART=1` (Test Lab only): auto singleplayer → auto character → enter world |
| `voxygen/src/render/renderer/mod.rs` | surface alpha mode chosen from capabilities (Android often only offers `Inherit`) |
| `voxygen/src/session/mod.rs` | mumble disabled on android |

Regenerate the patch after edits: `git add -N <new files> && git diff -- voxygen > .../0001-voxygen-android.patch`.

## 4. Build

```bash
source /work/native/env.sh
cd $VELOREN_SRC
cargo build --profile release-thinlto --target aarch64-linux-android -p veloren-voxygen --lib \
  --no-default-features --features singleplayer,simd,shaderc-from-source
# first build is long (hundreds of crates); incremental 1–3 min
cd my-veloren/native && SKIP_CARGO=1 ./build_android.sh   # → native/out/my-veloren-dev2.apk
```

`build_android.sh`: strip `.so` (477 MB → 57 MB), add `libc++_shared.so`, tar the full asset tree
into `assets/assets.tar` **stored uncompressed** (`aapt2 -0 tar`, so it streams fast), compile the Java
shim to `classes.dex`, zipalign, sign with a local debug keystore (gitignored). APK ≈ 466 MB.

Excluded cargo features (desktop-only or not needed): plugins, discord, native-dialog, egui, hot-reloading.

## 5. Java shim

`native/android/java/com/ayoub/myveloren/VelorenActivity.java` extends `NativeActivity`.
Needed because Firebase rejects APKs without dex (`NO_CODE_APK`) and to receive the game-loop
intent: on `com.google.intent.action.TEST_LOOP` it writes `files/autostart` (autopilot on);
on a normal launch it deletes it.

## 6. Touch controls (session, dev.3)

Patch `0002-dev3-touch-controls.patch` replaces the invisible dev2 zones with a drawn overlay.
Details and evidence: [DEV3.md](DEV3.md).

| Part | Where |
|---|---|
| State machine (fingers, stick, camera, pinch, buttons, editor, context) | `voxygen/src/touch.rs`, pure Rust, host tests in `native/touch-tests` |
| Drawing | `voxygen/src/hud/touch_overlay.rs` (conrod primitives above the HUD) |
| Touch → game | `android.rs` maps actions to `GameInput` press/release, analog `move_dir`, camera pan/zoom; one finger = mouse while a window is open |
| Context (Use/Mount/Respawn/Swim) + haptics on hits | `session/mod.rs` publishes `touch::Context` every tick |
| Layout | `settings.ron` → `touch` (position, size, alpha per button; opacity, sensitivity, smoothing, stick radius/dead-zone, haptics) |

## 7. Firebase Test Lab

`tools/ftl.sh <apk> <tag>` with env `OUT`, `FTL_DEVICE`, `FTL_VERSION`, `FTL_TIMEOUT`; type `game-loop`.
Run it as a **background job** (upload of 466 MB + 10–15 min test). Project `ayoub-261d7` (Spark):
~5 physical device runs/day shared with other projects; virtual devices have a separate quota.
Never enable billing, never cancel a running matrix.

- Physical: `r8q` (A13, version 33), `e3q` (A16, version 36).
- Virtual: `MediumPhone.arm` version 34 — renders with llvmpipe (CPU), slow; good for boot/crash checks.

Scenarios (dev.3): `FTL_SCENARIOS=1,2`; the Java shim passes the scenario number to the game.
Scenario 1 injects touches and logs `VEL-CHECK <name> PASS|FAIL`; scenario 2 runs after a restart.

Read results: `grep -a " veloren" <OUT>/<device>/logcat`. Markers:
`android: extracted assets … in Ns`, `android: activity resumed`, `android-autostart: …`,
`VEL-STAT t= state= fps=` (every 5 s), `PANIC:`. Tombstones + `video.mp4` are downloaded too.

## 8. Crash log → fix (lessons)

| Symptom | Cause | Fix |
|---|---|---|
| `NO_CODE_APK` at validation | pure NativeActivity APK | Java shim with `hasCode=true` |
| `SetLoggerError` abort right after extraction | `android_logger` and voxygen's tracing `log` bridge both set the global logger | no `android_logger`; own logs via `__android_log_write` |
| `System's $HOME directory path not found!` | Android has no `$HOME` | set `HOME`/`XDG_*` to internal storage before starting |
| `UnsupportedAlphaMode { requested: Opaque, available: [Inherit] }` | Android surface caps | choose alpha mode from caps |
| `TEST_QUOTA_EXCEEDED` | daily Spark quota used | switch to virtual device or wait for tomorrow |
| log spam `Failed to toggle cursor grab … NotSupported` | winit Android has no cursor grab | harmless; can be silenced on Android |
| VM Session at 0.5–2 fps | virtual device renders on CPU (llvmpipe) | logic check only; judge fps on a physical device |
| Outcome "Application crashed" after all checks passed; tombstone `FORTIFY: pthread_mutex_lock called on a destroyed mutex` in `libEGL_emulation eglDisplay::~eglDisplay` | `std::process::exit` runs C `atexit`/static destructors while the render thread still owns the EGL context | leave the game loop with `libc::_exit(0)` after `reportFullyDrawn`/finish |
| Autopilot step fails (glide, NPC) with `dead=true` in `VEL-STAT` | wild animals kill the character during the scripted run | before every step: if dead, tap the contextual Respawn button |
| "Use" opened a crafting window instead of the NPC dialogue | the nearest interactable was a crafting station | tap Use only when the target is within 3.5 m; close stray windows with Menu (Esc) |
| Analog speed test 0.00 m/s on a fast device | after a 36 m run the character stood against a wall | measure the walk back along the path just used |
| Shell killed by `pkill -f <pattern>` | the pattern also matched the agent's own command line | kill by PID, never `pkill -f` with a pattern from your own command |
- **Perf numbers (dev.4):** measure fps as frames ÷ wall clock, never as frames ÷ (sum of timed parts):
  the parts miss work outside them, and that overstated fps about 10× in build8. On the Test Lab VM (llvmpipe),
  `device.poll()` in `GlobalState::maintain` absorbs GPU work (~140 ms/frame), so VM fps only ranks tiers
  against each other. Gate decisions need a real Adreno or Mali.

## 9. Known gaps / next

- Touch overlay done in dev.3. Still missing: pause/resume surface recreation (dev.5).
- Mobile graphics tiers done in dev.4 (auto Low/Medium by GPU). Physical fps gate pending (Test Lab quota).
- VM attempt 5 reached the world (Session) and ran 10 min without crash; physical-device confirmation pending quota.
