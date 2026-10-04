# DEV5 — Android lifecycle and stability (+ 5.6 terrain occlusion culling)

> **الملخص بالعربية:** dev.5 يجعل اللعبة تتصرف كتطبيق أندرويد سليم. عند الخروج إلى الشاشة الرئيسية
> يتوقف الرسم والصوت والخادم المحلي، ويُحفظ اللاعب فورًا. وعند العودة يُعاد إنشاء سطح الرسم (surface) دون كراش.
> زر الرجوع يفتح قائمة Esc ويغلقها. الشاشة تبقى مضاءة، والوضع أفقي، والنوتش يُترك كشريط أسود.
> الموارد تُستخرج مرة واحدة فقط (مع رقم إصدار)، وتظهر نافذة تقدّم أثناء الاستخراج الأول.
> عند الكراش يُكتب تقرير محلي، وفي التشغيل التالي يُعرض على اللاعب ليشاركه.
> أُضيف بند 5.6 بطلب المالك («استخدم هذه التقنية طالما انها تساعد»): **إخفاء قطع التضاريس المحجوبة خلف الجبال
> (occlusion culling)** قبل رسمها.
> على الجهاز الافتراضي في Firebase (vm7): سيناريو 4 نجح 8/8 (خروج وعودة 5 مرات + حفظ قبل القتل)، وسيناريو 5 نجح 6/6
> (الحفظ بقي بعد القتل، والرجوع يعمل)، وسيناريو 1 نجح 13/14.
> فائدة تقنية الإخفاء لم تظهر على المحاكي لأنه يرسم بالمعالج، والحكم عليها يكون على هاتف حقيقي.

Owner instructions (Slack, 2026-10-03):
- 22:14 / 22:22 UTC: «اغلق dev4 وابدأ في dev5» / «ابدأ dev5» (start dev.5).
- 22:49 UTC: shared [PORTING-RESEARCH.md](PORTING-RESEARCH.md) («ربما تساعد»).
- 22:53 UTC: «استخدم هذه التقنية طالما انها تساعد» (use this technique if it helps) with a screenshot about occlusion culling → item 5.6.

Branch `feat/dev5-lifecycle`, version `0.1.0-dev.5` (vc50).

## What was built

All game code is in `native/patches/0004-dev5-lifecycle.patch`. It applies on top of 0001 + 0002 + 0003 on the
upstream pin `585a91b4a`: 12 files, +1105/−65 lines (2 new files: `voxygen/src/lifecycle.rs`,
`voxygen/src/scene/terrain/occlusion.rs`). Lock-file churn is excluded. The Java side is in
`native/android/java/com/ayoub/myveloren/` (`VelorenActivity.java`, new `CoverActivity.java`).

| # | Item | Status | Implementation |
|---|---|---|---|
| 5.1 | Real pause/resume | **PASS (VM)** | winit `Suspended` (Android `TerminateWindow`) → `lifecycle.rs`: drop the wgpu surface, mute kira (`set_master_volume(0)`, kira has no pause), save settings, force-persist all characters, pause the local server tick. `Resumed` (`InitWindow`) → new surface from the new native window, restore volume, unpause. The renderer skips frames while it has no surface |
| 5.2 | Autosave on `onPause` | **PASS (VM)** | The suspend path asks the server's `PersistenceScheduler` to write every character now (normally every 10 s), before Android can kill the process |
| 5.3 | Back, landscape, screen on, notch | **PASS (VM)** | Android Back (winit `NamedKey::BrowserBack`) maps to Escape → opens/closes the Esc menu. Manifest: `sensorLandscape`. `FLAG_KEEP_SCREEN_ON`, immersive sticky mode. Notch: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER` (letterbox), because the HUD has no safe-area support yet |
| 5.4 | Extract assets once + progress | **PASS (VM)** | `build_android.sh` writes `assets/assets.version` (first 16 hex of the tar's sha256). The app re-extracts only when the installed version differs. Extraction runs on a worker thread; Java shows a progress dialog |
| 5.5 | Local crash report | **PASS (VM)** | A Rust panic hook writes `files/userdata/crash/<ts>.txt` (message, location, backtrace). On the next start Java joins it with `ApplicationExitInfo` (Android 11+: native crash, ANR, signal) into `files/crash_last_report.txt` and offers a share dialog (not shown under Test Lab) |
| 5.6 | Terrain occlusion culling | **Works; benefit NOT MEASURED on a real GPU** | See below |

### 5.6 Terrain occlusion culling

Voxygen upstream only does frustum culling: every terrain chunk inside the camera view is drawn, even when
a hill hides it. Veloren chunks are full-height 32×32 columns, so a cheap 2.5-D test works:

- When a chunk is meshed, the mesh worker also stores a coarse **ground height map**: 4×4 cells of 8×8
  columns. Each cell holds the lowest top of natural terrain (rock, grass, earth, sand, snow) in that cell.
  Trees, houses and sprites are ignored on purpose, so they never hide anything.
- Each frame, for every in-frustum chunk at least 2 chunks away, a cone is marched from the camera to the
  chunk's top (mesh top or sun-occluder top + 4 blocks margin) in 8-block steps. If the ground under the
  whole cone footprint is higher than the ray at some step, the chunk is hidden.
- It is switched off when the camera is underground (caves) and it does not change shadows: shadow
  casting still uses the frustum result.
- Switch: `OCCLUSION_CULLING` (on by default). `VEL-BENCH` reports `occ_tested / occ_culled / occ_pct / occ_cost`.

Scenario 6 measures it as an ABBA A/B with the same camera sweep: off, on, on, off, 30 s each.

## Firebase Test Lab results

| Run | APK | Device | Outcome | Sc. 4 | Sc. 5 | Sc. 6 | Sc. 1 |
|---|---|---|---|---|---|---|---|
| vm6 | d5build1 `174fae6e…` | MediumPhone.arm v34 (llvmpipe) | gcloud exit 0, no PANIC | **8/8** | 4/6 (Back: `sendKeyDownUpSync` → `SecurityException`, needs INJECT_EVENTS) | — | 13/14 (npc_interact: nearest villager 11 m lower) |
| vm7 | d5build2 `47baf46c…` | MediumPhone.arm v34 (llvmpipe) | gcloud exit 0, no PANIC | **8/8** | **6/6** | 1/2 (see below) | 13/14 (analog_fast, see below) |
| r8q4 | d5build2 | Galaxy S20 FE (r8q) v33, physical | NOT RUN: `TEST_QUOTA_EXCEEDED` at 23:19 UTC (matrix-3gk5d31yy34fn); daily physical quota resets ~07:00 UTC | | | | |
| e3q3 | d5build2 | Galaxy S24 Ultra (e3q) v34, physical | PENDING | | | | |

vm7 details:
- Scenario 4: 5 home/return cycles via a cover activity. Each time: surface gone, server paused, audio muted
  while in background; 12–14 frames drawn within 3 s after return; 0 resume failures. Then inventory 2 → 1,
  background, `SIGKILL`.
- Scenario 5 (relaunch after the kill): inventory still 1 → **save survived the kill**; assets not re-extracted;
  crash report collected (360 B, exit-info `SIGNALED status=9`); activity flags OK; Back opened and closed
  the Esc menu ([open](evidence/dev5/vm7_back_opens_menu.jpg), [closed](evidence/dev5/vm7_back_closes_menu.jpg)).
- Scenario 6 (llvmpipe, mobile-low):

  | Window | fps | occ tested / culled per frame | culled % | cost |
  |---|---|---|---|---|
  | off-1 | 2.9 | — | — | — |
  | on-1 | 2.3 | 11.3 / 0.8 | 7 % | 4 µs |
  | on-2 | 2.4 | 26.0 / 0.6 | 2 % | 9 µs |
  | off-2 | 2.5 | — | — | — |

  `occlusion_culls` PASS, `occlusion_not_slower` FAIL. The test itself costs 4–9 µs per frame, which cannot explain a
  ~0.3 fps difference. On the VM ~370 ms of each frame is software rendering inside `maintain`, and off-1 → off-2
  alone drifts by 0.4 fps. The spawn area is fairly open, so only 2–7 % of the chunks tested are hidden. **No
  conclusion on the VM.** The physical A/B decides. If the physical runs show no gain, the default becomes off and
  the switch stays.
- Scenario 1: `npc_interact` now PASS (falls back to villagers within 16 m height difference). `analog_fast` FAIL
  (full stick 0.64 m/s vs light 0.51 m/s). It passed in vm1–vm6, and the code path was not touched in dev.5. It is
  speed-based and this VM run was unusually slow (≈2.5 fps), so it is treated as a VM flake to recheck on the physical runs.

The fix between vm6 and vm7: `Instrumentation.sendKeyDownUpSync` needs `INJECT_EVENTS`, which normal apps do not
have. The self-test now sends `KEYCODE_BACK` through `BaseInputConnection(decorView).sendKeyEvent(...)` on the UI
thread. That uses the window's own input pipeline (→ NativeActivity InputQueue → winit), the same path a real
Back press takes after the system dispatcher.

Evidence: [logcat excerpts vm6 + vm7](evidence/dev5/logcat-vm6-vm7.txt) ·
[occlusion-on sweep frame](evidence/dev5/vm7_occlusion_on_sweep.jpg).

### Acceptance (ROADMAP dev.5)

| Criterion | Status | Evidence |
|---|---|---|
| Home and return 5×, no crash (video) | **PASS on VM** (vm6 + vm7). Physical: PENDING | sc. 4, Test Lab video |
| Save survives a kill | **PASS on VM** (vm6 + vm7) | sc. 4 → 5 |
| Back opens the Esc menu | **PASS on VM** (vm7) | sc. 5 |
| Assets extracted once, version check, progress | **PASS on VM** | sc. 5 `assets_once` |
| Local crash report | **PASS on VM** | sc. 4/5 |
| Carried from dev.4: ≥30 fps on r8q at low tier | **PENDING** (first physical run of this build) | sc. 3 |

## Rebuild

```bash
cd $VELOREN_SRC && git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
for p in 0001-voxygen-android 0002-dev3-touch-controls 0003-dev4-mobile-perf 0004-dev5-lifecycle; do
  git apply <repo>/native/patches/$p.patch; done
source native/env.sh
<repo>/native/build_android.sh                 # -> native/out/my-veloren-dev5.apk (vc50)
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=25m FTL_SCENARIOS=4,5,6,1 OUT=... tools/ftl.sh native/out/my-veloren-dev5.apk dev5-vm
FTL_DEVICE=r8q FTL_VERSION=33 FTL_TIMEOUT=45m FTL_SCENARIOS=1,3,4,5,6 OUT=... tools/ftl.sh native/out/my-veloren-dev5.apk dev5-r8q
```

Scenarios 4 and 5 must run together and in that order (5 checks what 4 left behind).

Patch sha256: 0004 `2b847a8ca5e1dbd6e7bbe2f8dfaf1801985fa529922e256d4496dc4bd10bf21e`
(verified with `git apply` on a clean pin + 0001–0003; the result is identical to the build tree).
APK d5build2 (466,076,338 bytes) sha256 `47baf46c300e91a7f35364d1a94819b66a101b51ad553d48ed50743bf55cb9ef`.
Toolchain: nightly-2026-06-13, NDK r27c, build-tools 34, JDK 17.

## Known gaps / next

- Physical runs (r8q, e3q): 30 fps gate (sc. 3), lifecycle on a real GPU driver, occlusion A/B (sc. 6).
- Notch/safe-area: letterbox for now; a HUD safe-area inset belongs to dev.6 (touch UI).
- Owner research ([PORTING-RESEARCH.md](PORTING-RESEARCH.md)) candidates, not implemented, listed in the ROADMAP:
  SGSR upscaling, ADPF thermal hints, APK size reduction. Deleting maps needs an explicit owner decision
  (asset-preservation rule).
