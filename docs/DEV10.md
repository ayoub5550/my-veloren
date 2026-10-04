# DEV10 — Smoothness: frame generation, Snapdragon GSR, frame pacing, lighter APK

> **الملخص بالعربية:** المالك أعاد تعريف dev.10 ليصبح «الأداء والسلاسة»: لعبة أخف وأكثر سلاسة، مع مولّد فريمات، خاصةً في إعدادات الجرافيك العالية. أما خطة التوزيع القديمة فانتقلت إلى dev.12، وdev.11 صار «لعبة أخف» (انظر ROADMAP).
> - **مولّد الفريمات (Frame Generation):** يتناوب إطار حقيقي وإطار مولَّد. الإطار المولَّد لا يرسم العالم من جديد، بل يعيد إسقاط الإطار الحقيقي السابق على وضع الكاميرا الجديد بالاعتماد على خريطة العمق. النتيجة أن حركة الكاميرا (الالتفات والمشي والطيران الشراعي) تُعرض بـ60 إطارًا بينما يرسم المعالج الرسومي العالم بـ30 فقط.
>   الواجهة وأزرار اللمس تُرسم من جديد في كل إطار.
>   يعمل افتراضيًا في High وUltra، ويوجد زر «Frame gen: ON/OFF» في إعدادات الرسوميات. يتوقف تلقائيًا في منظور الشخص الأول، وعند القطع المفاجئ للكاميرا، وعندما يكون الإطار الحقيقي أبطأ من أن يلحق.
> - **Snapdragon GSR:** مكبّر الصورة من Qualcomm (رخصة BSD) صار في كل المستويات بدل التكبير العادي. يرسم بدقة أقل، ثم يكبّر الصورة بحواف أوضح.
> - **انتظام الإطارات:** ‏`ANativeWindow_setFrameRate` يطلب من الشاشة معدّلًا ثابتًا، و‏ADPF (`APerformanceHint`) يخبر النظام بزمن كل إطار، لتقليل التقطيع والخنق الحراري.
> - **APK أخف:** ‏467.6MB صارت 393.9MB (أقل بـ74MB) بإعادة ترميز 32 ملف موسيقى ثقيلًا إلى Vorbis بجودة q4 وحد أقصى 48kHz. لم يُحذف أي مورد.
> - **QEMU:** لم يُستعمل لأنه محاكي، ويجعل اللعبة أبطأ لا أسرع (شُرح للمالك في المحادثة).
> - يُثبَّت فوق dev.9 مباشرة (نفس مفتاح التوقيع)، والحفظ يبقى. الترقية تنقل إعدادات High/Ultra القديمة إلى الإعدادات الجديدة مرة واحدة.

Owner instructions (Slack DM, 2026-10-04):
- 16:11 UTC: «اجل ادمج وابدأ في dev.10 واستهدف جعل اللعبة اخف واكثر سلاسة مع اضافة مولد فريمات وتقنيات افضل تجعلها سلسة
  اكثر في الإعدادات الجرافيك العالية وتقنيات اذا كانت مفيدة مثل qemu» → PR #10 (dev.9) merged; dev.10 = performance.
- Standing: automated tests on the virtual device only (save the device quota); no prerelease.

Branch `feat/dev10-perf` from `main` b20875c (dev.9 merged). Version `0.1.0-dev.10` (vc100), same signing key as
dev.9.

## What was built

All game code is in `native/patches/0009-dev10-smoothness.patch` (19 files, 2 new: `voxygen/src/framegen.rs`,
`assets/voxygen/shaders/antialias/sgsr.glsl`). It applies on top of 0001–0008 on the upstream pin `585a91b4a`.
Packaging: `native/tools/slim_audio.sh` (new) and `native/build_android.sh`.

| # | Item | Status | Implementation |
|---|---|---|---|
| 10.1 | Frame generation | **Built; host verified; VM: see below** | `framegen.rs`. In the world, frames alternate real / generated. A generated frame skips the scene, clouds, trails and bloom passes; the post-process pass reprojects the previous real `tgt_color_pp` + depth with `M = P_new·V_new·T(focus_old − focus_new)·V_old⁻¹·P_old⁻¹` (f64 on the CPU, plus its inverse). The shader inverts the mapping per pixel with a 5-step fixed-point search, run twice (from the pixel and from the far-plane mapping) and keeps the converged, nearer hit, with a sky/background fallback. HUD/touch UI are redrawn every frame. Guards: off in first person; a camera cut (a 10 m point moving > 0.35 NDC) forces a real frame; if real frames take > 1.3/60 s for 30 frames it renders all frames for 5 s (backoff). Presented cap 60 (`PRESENT_FPS`), real frames at 30; a generated frame is held so it presents one interval after its real frame |
| 10.2 | Frame-gen setting | **Built; host verified** | `GraphicsSettings.frame_generation` (default off on desktop). Toggle «Frame gen: ON/OFF» next to the presets in Video settings (en + ar-SA strings). Phone tiers: Low/Medium off, **High/Ultra on** |
| 10.3 | Snapdragon GSR (SGSR1) | **Built; host verified** | new `AaMode::Sgsr` («Snapdragon GSR» in the AA list), shader `antialias/sgsr.glsl` adapted from SnapdragonStudios/snapdragon-gsr (BSD-3-Clause, © 2025 Qualcomm Innovation Center; license in the shader and in `docs/licenses/SGSR-BSD-3-Clause.txt`). Edge direction on HDR colour (Reinhard on green). `texelFetch` instead of `textureGather` (naga rejects `textureGather` in this pipeline) |
| 10.4 | Phone tiers | **Built** | Low SGSR 0.6 · Medium SGSR 0.7 · High SGSR 0.75 + FG, 60 fps · Ultra SGSR 0.8 + FG, 30 real → 60 presented. Thermal cool-down no longer drops the fps cap while FG is on (it lowers the render scale instead). One-time `dev10_upgrade`: FxUpscale → SGSR, old High/Ultra get FG and the new scale |
| 10.5 | Frame pacing (Android) | **Built** | `mobile::pacing`: `ANativeWindow_setFrameRate` (display rate = target fps) and an ADPF `APerformanceHint` session for the main thread (target = frame interval, reports each frame's CPU work). Symbols are looked up with `dlsym`, so older Android versions just skip it. Logged as `VEL-PACING` |
| 10.6 | Lighter APK | **Built** | `slim_audio.sh` re-encodes audio files above 150 kb/s or 48 kHz to Vorbis q4, ≤ 48 kHz (sha256 cache). 32 files: 143.1 MB → 69.3 MB. APK 467.6 MB (dev.9) → **393.9 MB**. No asset removed |
| 10.7 | QEMU | **Not used** | QEMU emulates a CPU; on a phone it would make the game slower. The owner was told in the DM |

## Evidence on the host (desktop build, same shaders)

- `docs/evidence/dev10/host-sgsr-0.6-charcreate.jpg`: SGSR at render scale 0.6, character creation stays sharp.
- `docs/evidence/dev10/host-fg-rotation-a.jpg` / `-b.jpg`: camera turning with FG on, geometry stays coherent.
- `docs/evidence/dev10/host-fg-strafe-generated.jpg` vs `host-fg-strafe-real.jpg`: a generated frame and the next real
  frame while strafing. Static scenery matches; the differences (mean 22/255, 8 % of pixels over 30) are moving NPCs and
  rain, which only update on real frames.
- Host log: `plan #241 … out=true`, real = 720, generated = 1600 frames (forced mode `MY_VELOREN_FG_HOLD`).

## Firebase Test Lab (virtual device MediumPhone.arm v34 only)

Scenario 14: auto tier → ABBA benchmark fg-off-1 / fg-on-1 / fg-on-2 / fg-off-2 (25 s each, same spot) → Ultra + FG walk.
On the VM the GPU is software, so FG is forced (`force_framegen`) to measure it; on a phone the backoff decides.

### VM results (Firebase Test Lab, vm10a, 2026-10-04)

| Run | Device | Scenarios | Outcome |
|---|---|---|---|
| vm10a (matrix-208zbes7ps7uy) | MediumPhone.arm, API 34, landscape | 14, 1 | **Passed**: sc.14 7/7, sc.1 15/15 (no regression), 0 PANIC |

| Check | Result | Receipt |
|---|---|---|
| `sgsr_active` | PASS | aa=Sgsr, upscale 0.60, pipelines ready |
| `fg_generates` | PASS | FG on: 166 generated / 172 real frames in the window |
| `fg_off_all_real` | PASS | FG off: 0 generated / 212 real |
| `fg_more_fps` | PASS | presented fps 6.75 (FG on) vs 4.20 (FG off) = **1.61×** on the software GPU |
| `pacing_api` | PASS | setFrameRate hz=60 rc=0, ADPF session active, 2020 work-duration reports |
| `ultra_dev10` | PASS | Ultra = SGSR 0.80 + FG on + max 30 fps |
| `fg_walk` | PASS | Ultra walk: 197 generated / 1870 real, backoff engaged once |

Caveat: the VM renders on a software GPU (single-digit fps), so absolute numbers say nothing about a phone.
On the VM a generated frame cost 47 ms vs 28 ms for a real one (CPU-emulated compute); on Adreno the
backoff turns FG off by itself if a generated frame is not clearly cheaper. The real gate is the owner's phone checklist below.

## Known gaps

- **Phone feel is the owner's check.** The VM has a software GPU: it proves FG works and counts frames, but smoothness,
  heat and SGSR sharpness must be judged on the Poco F3.
- Generated frames only move the camera: animals, NPCs, water and particles move at the real rate (30 fps on Ultra).
  Fast-moving objects near the edge of the screen can show a thin smeared border for one frame.
- Disocclusion: the area revealed behind a close object while turning is filled from the nearest background pixel.
- No dynamic resolution scaling (needs a real device to tune).
- Two unused world maps (`veloren_0_9_0_0.bin`, `veloren_0_16_0_0.bin`, 16.7 MB each, 33.5 MB together) are still in
  the APK. Removing them needs the owner's decision (AGENTS.md keeps every asset).

## What the owner should check on the phone

1. Install dev.10 over dev.9 (no uninstall needed); check the worlds and characters are still there.
2. Settings → Video: «Frame gen: ON» on High/Ultra. Play on Ultra, turn the camera, walk, glide: it should feel smoother
   than dev.9. Turn it OFF and compare.
3. Watch heat after 15–20 minutes on Ultra.
4. Look at sharpness (SGSR) on Low and Medium compared with dev.9.

## Rebuild

```bash
# VELOREN_SRC = upstream at 585a91b4a with patches 0001..0009 applied (git apply native/patches/000*.patch)
env -i HOME=$HOME PATH=/usr/local/bin:/usr/bin:/bin bash -c 'source <env with ANDROID_NDK_HOME, ANDROID_HOME, JAVA_HOME,
  VELOREN_SRC, ANDROID_KEYSTORE>; native/build_android.sh'   # → native/out/my-veloren-dev10.apk (runs slim_audio.sh; needs ffmpeg)
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=25m FTL_SCENARIOS=14,1 tools/ftl.sh native/out/my-veloren-dev10.apk dev10
```
