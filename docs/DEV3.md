# DEV3 — Touch controls (stick, buttons, layout editor)

> **الملخص بالعربية:** dev.3 يضيف طبقة تحكم لمس مرئية كاملة فوق Voxygen الأصلية:
> عصا تحكم تناظرية، وكاميرا بالسحب، وتكبير بإصبعين، وأزرار القتال والحركة، وشريط قوائم علوي،
> وأزرار تظهر حسب السياق (Use / Respawn / Mount…)، ومحرر تخطيط يُحفظ في `settings.ron`، واهتزاز.
> اختُبر آليًا في Firebase Test Lab بحقن اللمس (game-loop) على جهاز حقيقي Galaxy S20 FE (r8q)
> وعلى الجهاز الافتراضي MediumPhone.arm. النتائج والأدلة في الجداول أدناه.

Owner instruction (Slack, 2026-10-03): «نفّذ dev.3 كاملا من غير اخطاء وتجرب في firebase».
Branch `feat/dev3-touch-controls`, version `0.1.0-dev.3` (vc30). Not merged: owner approval needed.

## What was built

All game code is in patch `native/patches/0002-dev3-touch-controls.patch` (applies on top of 0001
on the upstream pin `585a91b4a`; 10 files, about +2.4k/−0.2k lines). Lock-file churn is not part of the
patch, same as 0001.

| # | Item | Implementation |
|---|---|---|
| 3.1 | Visible overlay | `voxygen/src/hud/touch_overlay.rs` draws the layer with conrod primitives above the HUD; global opacity setting plus opacity per button |
| 3.2 | Dynamic analog stick | appears where the left-half finger lands, has a dead-zone, and sends an analog `move_dir` to the session (no W/A/S/D emulation). Light tilt walks and full tilt runs |
| 3.3 | Camera drag + pinch | right-half drag moves the camera, with sensitivity and smoothing settings; two-finger pinch zooms |
| 3.4 | Combat buttons | Attack (M1), Skill (M2), Block, Roll, Jump, abilities 1–5. Holding a button keeps the input pressed, so charged attacks work |
| 3.5 | Movement/interaction | Glide, Sneak, Wield, Lamp; Swim up/down while in liquid |
| 3.6 | Menu bar | Menu (Esc), Bag, Map, Skills, Craft, Social, Chat, Edit. The bar stays visible while windows are open, so every window can be closed |
| 3.7 | Context buttons | **Use** only when something is interactable; **Mount/Dismount** only near a mount or while riding; **Respawn** only when dead; swim buttons only in water |
| 3.8 | Layout editor | Edit → drag any button, Bigger/Smaller, Fainter/Stronger, Reset, Done. Saved under `touch` in `settings.ron` |
| 3.9 | Multi-touch | separate tracking per finger: the finger that starts on a button never moves the camera; stick + camera + attack work at the same time |
| 3.10 | Haptics + hints | vibration through JNI `Vibrator` on hits taken (70 ms) and hits dealt (25 ms); key hints such as `[E]` are shown with touch labels (`[Use] Talk`, `[Glide]`) |

Files: `voxygen/src/touch.rs` (pure state machine, host unit tests),
`hud/touch_overlay.rs`, `android.rs` (touch → events, JNI, Test Lab autopilot),
`session/mod.rs` (analog move, context publishing, haptics), `settings/mod.rs` (`touch` section),
`run.rs`, `window.rs`, `lib.rs`, `hud/mod.rs`, `Cargo.toml`.
Repo side: Java shim (game-loop scenario + `Vibrator` + `reportFullyDrawn`/finish),
`native/build_android.sh` (vc30), `tools/ftl.sh` (scenarios), and `native/touch-tests/`, which has 11 host tests:
layout fits 16:9 to 22:9 without overlap, stick dead-zone/analog, multi-touch isolation, smoothing, pinch,
context buttons, mouse mode in windows, and editor plus RON round trip.

Layout previews (host render): [play](evidence/dev3/layout_preview_play.jpg) ·
[context buttons](evidence/dev3/layout_preview_context.jpg) · [edit mode](evidence/dev3/layout_preview_edit.jpg).

**Quests (3.6):** this upstream version has no standalone quest window key; quests are opened from
NPC dialogue. That path is covered by the NPC test (dialogue opens). No separate Quests button was added.

## Firebase Test Lab — automated touch test (game-loop)

The Java shim receives `TEST_LOOP` with the scenario number. `android.rs` then injects real touch events
through the same path as the screen: finger down, move, and up. It reads the game state back
(position, velocity, camera, character state, windows) and logs `VEL-CHECK <name> PASS|FAIL`.

- Scenario 1: play and check every dev.3 feature, then save an edited layout.
- Scenario 2: runs in a new process (a restart). It checks that the layout was restored, then resets it.

| Check | What it proves |
|---|---|
| multitouch | stick + camera drag + Attack held **at the same time** (3 fingers): character moved, camera turned, attack state seen |
| analog_slow / analog_fast | light tilt walks slower than full tilt (m/s measured) |
| pinch_zoom | camera distance decreases |
| buttons | Skill, Block, Roll, Jump, 1–5, Sneak, Wield, Lamp pressed; states seen; no crash |
| glide | glider wielded / gliding after Glide + Jump |
| bag_open/closed, map_open/closed | menu bar opens/closes windows; game input released/returned |
| npc_interact | walks to the nearest villager, taps the contextual **Use** button, dialogue/trade opens |
| layout_saved → layout_restored → layout_reset | editor changes persist across a restart; Reset restores defaults |
| haptics | JNI vibration self-test (+ count of hit pulses) |

### Results

| Run | APK | Device | Outcome | Scenario 1 | Scenario 2 |
|---|---|---|---|---|---|
| vm1 | build4 `b34e3e34…` | MediumPhone.arm v34 (CPU render) | **Crashed at exit** (EGL abort, see lessons) | 13/14 (npc_interact: died on the way) | 3/3 |
| vm2 | build5 `a83e149b…` | MediumPhone.arm v34 | Passed | 13/14 (glide: character was dead) | 3/3 |
| r8q1 | build5 `a83e149b…` | **Galaxy S20 FE (r8q), Android 13, physical** | **Passed** | 9/14 (see below) | **3/3** |
| vm3 | build6 `08a6ca64…` | MediumPhone.arm v34 | **Passed** (gcloud exit 0, no tombstone/panic) | **14/14** | **3/3** |
| r8q2 | build6 | r8q physical | NOT RUN — `TEST_QUOTA_EXCEEDED` (daily physical quota used; e3q also refused) | — | — |

**r8q1 (physical, 7–9 fps in world) details:** these passed:
- multitouch: moved 36.6 m, camera yaw changed 1.24 rad, 3 fingers, attack seen
- pinch: 9.99 → 7.65
- buttons
- glide: `glider_wielded=true gliding=true`
- bag and map open/close
- layout restore after restart
- multitouch after restart: moved 63.2 m

These failures came from the test script, not from the controls:
- analog 0.00 m/s: the character stood against a house wall after the 36 m run.
- npc_interact: **Use** was tapped at 21 m and targeted a crafting station, not the villager. The crafting window opened ([frame](evidence/dev3/r8q1_use_opened_crafting_station.jpg)).
- The crafting window stayed open, so game_input_back and layout_saved failed after it.

build6 fixes the script:
- The analog test walks back along the path just used.
- **Use** is tapped only when the villager is 3.5 m away or closer.
- Stray windows are closed with Menu.
- Detours keep the same side 3 times, then switch.
- Villagers on other floors are ignored.
- The character respawns before each step if it died.

Evidence frames (r8q physical): [stick + camera + attack](evidence/dev3/r8q1_multitouch_attack.jpg) ·
[glide](evidence/dev3/r8q1_glide.jpg) · [bag](evidence/dev3/r8q1_bag_open.jpg) · [map](evidence/dev3/r8q1_map_open.jpg).
VM: [contextual Respawn after death](evidence/dev3/vm2_died_contextual_respawn.jpg) ·
[contextual Use next to a villager](evidence/dev3/vm2_npc_contextual_use.jpg) ·
[NPC dialogue opened by touch](evidence/dev3/vm2_npc_dialogue.jpg).
Text excerpts of the logs: [evidence/dev3/logcat-checks.txt](evidence/dev3/logcat-checks.txt).

### Acceptance (ROADMAP dev.3)

| Criterion | Status | Evidence |
|---|---|---|
| r8q video: move + camera + attack simultaneously | **PASS** | r8q1 `multitouch PASS moved=36.6m cam_yaw_delta=1.24rad attack_seen=true max_fingers=3`, video frame |
| Glide | **PASS** (r8q1, vm1) | `glide PASS glider_wielded=true gliding=true` |
| NPC interaction by touch | **PASS on VM** (vm2, vm3: contextual Use → dialogue opened). vm3 (build6): `npc_interact PASS dialogue=true taps=1`. Physical on build6: NOT RUN yet — daily Test Lab physical quota; r8q rerun scheduled | vm2 frames + log |
| Layout saved, restored after restart | **PASS** (vm1, vm2, r8q1 restore/reset; save on VM) | `layout_saved` / `layout_restored` / `layout_reset` |
| Game-loop scenario with touch injection | **PASS** | `tools/ftl.sh` scenarios 1,2 |
| No crash | **PASS** from build5 on (r8q1 + vm2 outcome `Passed`, no tombstone) | |

## Rebuild

```bash
cd $VELOREN_SRC && git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
git apply <repo>/native/patches/0001-voxygen-android.patch
git apply <repo>/native/patches/0002-dev3-touch-controls.patch
source native/env.sh   # see native/env.example.sh
cargo build --profile release-thinlto --target aarch64-linux-android -p veloren-voxygen --lib \
  --no-default-features --features singleplayer,simd,shaderc-from-source
<repo>/native/build_android.sh                 # -> native/out/my-veloren-dev3.apk (vc30)
native/touch-tests/run.sh                      # 11 host tests
FTL_DEVICE=r8q FTL_VERSION=33 FTL_TIMEOUT=20m FTL_SCENARIOS=1,2 OUT=... tools/ftl.sh native/out/my-veloren-dev3.apk dev3-r8q
```

Patch sha256: 0001 `1a4068f474d5a0e1172ed99e231f69de6192a6740a2c027c3a5fd24ebef30e92`,
0002 `cc2a9e67fb54176a3ce42279d9e3255db0aa445521c3df99a160f733d4bc24a7`. Toolchain: nightly-2026-06-13, NDK r27c, build-tools 34, JDK 17.

## Known gaps (next milestones)

- Performance is about 7–9 fps in the world on r8q with desktop default graphics settings. This is **dev.4** (mobile graphics preset).
- No pause/resume surface recreation yet (**dev.5**).
- An upstream tutorial line still says "Press [ ] to free your cursor": it refers to a key with no touch equivalent.
- No in-game sliders yet for global opacity, camera sensitivity or stick size. They are in `settings.ron` (`touch`) for now. The full touch UI for settings is **dev.6**.
- The FTL video is recorded in portrait on the VM, so VM frames are rotated back for evidence.
