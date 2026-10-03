# DEV4 — Mobile performance and graphics tiers

> **الملخص بالعربية:** dev.4 يضيف إعدادات رسوميات خاصة بالهاتف بثلاثة مستويات (منخفض، متوسط، عالٍ).
> يختار التطبيق المستوى تلقائيًا حسب كرت الشاشة (GPU) عند أول تشغيل، ويمكن تغييره من قائمة الإعدادات.
> ويضيف أيضًا ذاكرة للشيدرات (pipeline cache)، وقياس أداء دقيقًا بالوقت الفعلي في السجل، وسيناريو اختبار 3:
> مقارنة المستويات الأربعة ثم جلسة 10 دقائق متواصلة.
> اختُبر على الجهاز الافتراضي في Firebase: لا كراش، وسيناريو 1 نجح 14/14، وسيناريو 2 نجح 3/3، وسيناريو 3 اكتمل.
> المستوى المنخفض أسرع بنحو 3.4 مرات من إعدادات الحاسوب على نفس الجهاز الافتراضي.
> **لم يُشغَّل بعد على هاتف حقيقي**: حصة Test Lab اليومية للأجهزة الحقيقية نفدت (`TEST_QUOTA_EXCEEDED`).
> لذلك شرط القبول (30 fps على S20 FE) ما زال **NOT RUN**.
> أغلق المالك dev.4 بهذه الحالة («اغلق dev4 وابدأ في dev5»، 2026-10-03). القياس على الأجهزة الحقيقية
> S20 FE وS24 Ultra ينتقل إلى بناء dev.5، وهو يحتوي كل تغييرات dev.4.

Owner instructions (Slack, 2026-10-03):
- 21:05 UTC: «جيد ابدأ في dev4» (start dev4).
- 22:14 UTC: «اغلق dev4 وابدأ في dev5» (close dev4 and start dev5).

Branch `feat/dev4-mobile-perf`, version `0.1.0-dev.4` (vc40).

## What was built

All game code is in `native/patches/0003-dev4-mobile-perf.patch`. It applies on top of 0001 + 0002 on the
upstream pin `585a91b4a`: 27 files, about +0.7k/−30 lines. Lock-file churn is excluded.

| # | Item | Status | Implementation |
|---|---|---|---|
| 4.1 | Measurement on real devices | **Tooling DONE; physical run NOT RUN (quota)** | `VEL-PERF` every 5 s: wall-clock fps, frame ms split into tick / acquire / record / sleep / maintain / hook / winit, RSS, temperature, CPU % per thread. `VEL-BENCH` for each benchmark window gives p50/p95/p99 frame ms. The r8q baseline from dev.3 is 5–10.6 fps in world (mean 6.8) at desktop defaults |
| 4.2 | Default "Mobile" graphics | **DONE** | `voxygen/src/mobile.rs` → `Tier::Low`: view distance 6 (entities 6), LOD distance 60 / detail 100, sprites 60, no shadows, minimal clouds, low water/reflections, Lambertian lighting, bloom off, render scale 0.6, 60 fps cap (10 in background), Fifo present |
| 4.3 | Tiers in the settings menu + auto-pick | **DONE** | Graphics settings on Android show Mobile Low / Medium / High plus Desktop buttons. `mobile_gpu.rs` sorts GPUs by driver name: Adreno ≥730, Mali-G ≥710, Immortalis and Xclipse → **Medium**; everything else (incl. Adreno 650 / S20 FE) → **Low**. The first launch writes `config_dir/mobile_preset`, and the user's choice is never overwritten after that. A `force_tier` file overrides the choice for tests |
| 4.4 | Strip the `.so` | **DONE** (since dev.2) | `build_android.sh` runs `llvm-strip --strip-unneeded`: 477 MB → 57 MB. The unstripped `.so` stays in the build tree (`target/…/release-thinlto`) for symbolication. It is not published |
| 4.5 | Pipeline cache | **DONE; benefit NOT MEASURED on device** | All 23 render pipelines use a `wgpu::PipelineCache` (when the adapter supports it). It is loaded at start and saved after the pipelines are built and on exit, in `files/.cache/wgpu_pipeline_cache_<backend>_<vendor>_<device>.bin`. On the VM (llvmpipe) the driver only writes a 96-byte header, so the effect can only be seen on a real Adreno or Mali |
| 4.6 | Thread distribution | **NOT DONE (deliberately)** | Thread counts are only changed on evidence from a real device. VM CPU numbers are dominated by software rendering. Per-thread CPU % is now logged (`cpu=[android_main:…, slowjob:…, rayon-s:…]`), ready for that decision |

Other changes:
- A tier change requested outside the settings window (the test autopilot, first-launch preset) goes into a pending-graphics slot. The session applies it on its own tick through the normal settings path.
- `mobile_gpu` has 2 new host tests. `native/touch-tests` now runs 13 tests, all passing.
- `tools/ftl.sh` and the Manifest have game loop 3.

## Firebase Test Lab results

| Run | APK | Device | Outcome | Sc. 1 | Sc. 2 | Sc. 3 |
|---|---|---|---|---|---|---|
| vm4 | build8 `f6f09b5f…` | MediumPhone.arm v34 (llvmpipe CPU render) | **Passed**, gcloud exit 0, no PANIC | 13/14 (npc_interact: no villager in range) | **3/3** | **DONE** (4-tier bench + 10-min soak, no crash) |
| e3q1, e3q2 | build8 / build9 | **Galaxy S24 Ultra (e3q) v34, physical** | NOT RUN: `TEST_QUOTA_EXCEEDED` | — | — | — |
| vm5 | build9 `09a89e90…` | MediumPhone.arm v34 | **Passed**, no PANIC | **14/14** (npc_interact PASS, dialogue opened) | — | — |
| r8q (S20 FE) | — | physical | NOT RUN: daily physical quota already used (shared) | — | — | — |

### Tier benchmark (vm4, same spot, 40 s per tier, CPU-rendered VM)

build8 printed fps as frames ÷ (sum of the measured parts), which overstated it about 10×. The table below
uses wall-clock fps = frames ÷ 40 s window. build9 fixed the logger: in vm5, `VEL-PERF` fps equals the
`VEL-STAT` wall-clock fps.

| Tier | View dist. | Render scale | Shadows | Frames in 40 s | **Wall fps** |
|---|---|---|---|---|---|
| desktop (upstream default) | 10 | 1.00 | Map 1.0 | 45 | **1.1** |
| mobile-high | 10 | 0.85 | Map 0.5 | 48 | **1.2** |
| mobile-medium | 8 | 0.75 | Cheap | 99 | **2.5** |
| mobile-low | 6 | 0.60 | None | 152 | **3.8** (×3.4 vs desktop) |
| soak, mobile-low, 10 min | 6 | 0.60 | None | 2239 in ~620 s | **3.6**, stable, RSS ~1.6 GB, no crash |

**Why VM fps is not representative:** in vm5, about 140 ms of each ~155 ms frame is spent in
`GlobalState::maintain` → `device.poll()`. On the VM, the "GPU" is llvmpipe running on the CPU,
so all GPU work shows up as main-thread CPU time. On a phone with Adreno/Mali this work runs on the GPU and shows up in
`acquire`. The **ratio** between tiers is useful; the absolute numbers are not. The physical r8q/e3q runs decide
the 30 fps gate.

Evidence: [low tier in world](evidence/dev4/vm5_low_tier_in_world.jpg) ·
[forest, low tier](evidence/dev4/vm5_low_tier_forest.jpg) ·
[walking to a villager for npc_interact](evidence/dev4/vm5_npc_approach.jpg) ·
log excerpts [evidence/dev4/logcat-perf.txt](evidence/dev4/logcat-perf.txt).

### Acceptance (ROADMAP dev.4)

| Criterion | Status | Evidence |
|---|---|---|
| ≥30 fps in world on r8q, low tier | **NOT RUN** (physical quota). Carried to the dev.5 build, which includes dev.4 | — |
| 15-min session, no crash or severe throttling | **VM: 10-min soak + ~5 min of scenarios, no crash (PASS on VM)**. Physical: **NOT RUN** | vm4 sc. 3 |
| Tiers selectable + auto-pick | **PASS** (VM picked mobile-low for llvmpipe; the choice was kept on relaunch) | `mobile preset:` lines |
| No regressions in dev.3 touch checks | **PASS** vm5 14/14, vm4 sc. 2 3/3 | logcat |

## Rebuild

```bash
cd $VELOREN_SRC && git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
git apply <repo>/native/patches/0001-voxygen-android.patch
git apply <repo>/native/patches/0002-dev3-touch-controls.patch
git apply <repo>/native/patches/0003-dev4-mobile-perf.patch
source native/env.sh
<repo>/native/build_android.sh                 # -> native/out/my-veloren-dev4.apk (vc40)
native/touch-tests/run.sh                      # 13 host tests
FTL_DEVICE=r8q FTL_VERSION=33 FTL_TIMEOUT=35m FTL_SCENARIOS=1,2,3 OUT=... tools/ftl.sh native/out/my-veloren-dev4.apk dev4-r8q
# force a tier for a test: write "mobile-low|mobile-medium|mobile-high|desktop" to <config_dir>/force_tier
```

Patch sha256: 0003 `83999715131cb871fb446b4697bfee122646faf507835213d10601cf55294cc8`.
APK build9 (466,035,311 bytes) sha256 `09a89e90a751b3f155d3ce3e08005f56528173cfb6807155097983db37d479e6`.
Toolchain: nightly-2026-06-13, NDK r27c, build-tools 34, JDK 17.

## Known gaps / carried forward

- Physical performance numbers (r8q, e3q) and the 30 fps gate: first physical run of the dev.5 build.
- 4.6 thread tuning waits for that physical data.
- Not tried: full LTO and `target-cpu` flags (SIGILL risk on older cores), ASTC textures, asset slimming (all assets stay).
- On the VM, switching to the desktop tier triggered one ANR while pipelines rebuilt (llvmpipe). The process survived.
