# DEV7 — Owner's Poco F3 fixes (Ultra heat, buttons, joystick, menus, touch settings, icon)

> **الملخص بالعربية:** dev.7 يصلح ما لاحظه المالك على هاتفه Poco F3 في dev.6.
> - **Ultra:** على أندرويد صار Ultra «للهاتف» (مسافة رؤية 12، حد 30 fps، ظلال أخف). يوجد أيضًا مراقب حرارة
>   (Android Thermal API) يخفّض الإعدادات خطوة كل 45 ثانية عند السخونة، ويكتب رسالة في الشات.
> - **الأزرار:** صار لكل زر أيقونة بأسلوب الفوكسل بدل الحروف، وحلقة ملونة، وتوهج عند الضغط.
>   الأزرار القتالية مرتبة على قوسين حول زر الهجوم تحت الإبهام الأيمن، والتخطيط مثبّت على حواف الشاشة.
>   اختبار آلي يمنع أي تداخل، ومحرر التخطيط يرفض وضع زر فوق زر.
>   زر الطيران الشراعي يظهر مكان زر القفز فقط وأنت في الهواء. أزرار القدرات 1–5 تظهر فقط إذا كانت خانتها في الشريط ممتلئة.
> - **العصا:** صارت قاعدتها عائمة تتبع الإصبع، ونصف قطرها أكبر، والمنطقة الميتة أصغر، والحركة أنعم.
>   تتوقف فور رفع الإصبع.
> - **القوائم:** في القائمة الرئيسية زر كبير واحد «Singleplayer» (أُخفيت الخوادم وتسجيل الدخول). أزرار اختيار الشخصية أكبر.
> - **الإعدادات:** تبويب «التحكم» على أندرويد يعرض إعدادات اللمس فقط: محرر التخطيط، وإعادة الضبط، و7 أشرطة، ومفتاحان.
>   أُخفيت ربطات الكيبورد ويد التحكم، وأُخفيت تلميحات الكيبورد في الشاشة.
> - **الأيقونة:** أيقونة جديدة متكيفة (adaptive) من شعار Veloren الرسمي.
> - **الاختبار:** على الجهاز الافتراضي في Firebase (vm11) نجح سيناريو 1 بنتيجة 15/15، وسيناريو 2 بنتيجة 3/3، وسيناريو 9 الجديد بنتيجة 8/9، ولا يوجد أي PANIC.
>   الفحص الوحيد الفاشل كان خطأ في الاختبار نفسه وليس في اللعبة، وقد أُصلح. أُعيد البناء (d7build4) ولم يتغير فيه إلا كود الاختبار.
>   إعادة الاختبار (vm12) رفضها Firebase لأن حصة اليوم المجانية انتهت. ستُعاد بعد تجدد الحصة.
>   المرجع النهائي هو تجربة المالك على هاتفه.
>   الكراشات (7.7): لم تصلنا تقارير بعد.

Owner instructions (Slack, 2026-10-04):
- 01:10 UTC: Poco F3 feedback on dev.6 → write the fixes into the next plan; the pending Firebase physical runs are no longer needed.
- 01:26 UTC: «ابدأ dev. 7» and «اصلح كذلك الأيقونة» (launcher icon, screenshot from his MIUI launcher) → item 7.9.

Branch `feat/dev7-phone-fixes` (from `main` 91125bf). Version `0.1.0-dev.7` (vc70).

## What was built

All game code is in `native/patches/0006-dev7-phone-fixes.patch`. It applies on top of 0001–0005 on the upstream pin
`585a91b4a`, and the lock-file churn is excluded. Android packaging changes are in `native/android/` (manifest, `res/` icons) and
`native/build_android.sh` (aapt2 compiles `res/`).

| # | Item | Status | Implementation |
|---|---|---|---|
| 7.1 | Ultra heats the phone | **Built; VM PASS** (`thermal_api`, `thermal_steps`); real heat needs the owner's phone | `mobile.rs`: `Tier::Ultra` = phone-safe Ultra (view distance 12, LoD 150/250, sprites 200, **30 fps cap**, shadow map 0.75, clouds Medium, render scale 1.0). The Ultra button in Graphics uses it on Android. The thermal guard is `AThermal_getCurrentThermalStatus` / `AThermal_getThermalHeadroom` (dlsym, API 30+/31+). It polls every 5 s and acts when status ≥ 2 (moderate) or headroom ≥ 0.95 for 2 polls in a row. It goes down at most one step per 45 s: fps 30 → shadows off → render scale −0.1 (min 0.6) → view distance −2 (min 5), and posts a chat line (`hud-thermal-lowered`, en + ar-SA). Instead of a warning dialog before heavy presets, Ultra itself is now safe and the guard catches the rest |
| 7.2 | Buttons look "dead" | **Built; VM visual PASS** | `touch_overlay.rs` rewritten: dark disc + coloured ring + icon. There are 12 new 16×16 voxel-style icons (`assets/voxygen/element/ui/touch/`), and Veloren's own icons are reused for menu buttons. Ability buttons show the hotbar ability icon and dim when not usable. The pressed state glows. No cooldown timers, because Veloren abilities cost energy and have no cooldowns, so "not usable" (greyed) is the equivalent |
| 7.3 | Buttons overlap, tiring layout | **Built; VM PASS** (`layout_fits`: 0 overlaps at 2.222 aspect; `glide_button_shown`, `glide`; `ability_buttons`) | `touch.rs` `default_layout_for(aspect)`: positions anchored to the left/right screen edge in units of height, so the layout keeps its shape on any phone. Attack (0.20, 0.76) r 0.115 under the right thumb. Inner arc R 0.265: Secondary, Block, Roll, Jump. Outer arc R 0.45: abilities 1–5. Minimum gap 1 % and kept clear of the minimap. Contextual buttons: Glide only while airborne, in Jump's place. Respawn only when dead. Abilities only for filled hotbar slots 1–5. The layout editor refuses drops/growth onto another button (`overlaps()`). Saved layouts move to the key `layout_v7`, so every phone starts from the new default |
| 7.4 | Joystick should be smoother | **Built; VM PASS** (`stick_full`, `stick_follow`, `stick_stopped`) | Floating base that follows the finger past the rim. Radius 0.15 of height (was 0.13). Dead zone 0.08 (was 0.15). Exponential smoothing τ = 30 ms while held, immediate stop on release. Settings sliders: stick size, dead zone, camera sensitivity/smoothing |
| 7.5 | Menus don't match the buttons | **Partly built; VM visual PASS for the main menu** | Main menu on Android: one large «Singleplayer» button. Server list, login fields, Multiplayer and the info box are hidden, and the left buttons are bigger. Character select: bigger buttons (46/66 px) and Spectate hidden. The Esc menu and settings windows keep the upstream look (bigger through the dev.6 DPI scale) |
| 7.6 | Controls settings: touch only | **Built; compile-verified, not driven by a VM scenario** | `settings_window/touch_controls.rs`: on Android the Controls tab shows Edit layout, Reset, 7 sliders (opacity, button size, stick size, dead zone, camera sensitivity, camera smoothing, zoom sensitivity) and 2 toggles (floating stick, vibration) instead of keyboard/gamepad bindings. Keyboard hints are hidden: the bottom-right hotkey hints, the Tutorial button and keyboard tutorial texts (touch texts `tutorial-touch-*` instead) |
| 7.7 | Crashes / game exits | **Partly** | No crash report received yet (dev.5 share dialog). Done: manifest `configChanges` now also covers locale, layoutDirection, fontScale, colorMode, touchscreen, mcc/mnc, so a system change no longer restarts the native activity. VM: 3 scenarios, 0 PANIC. Each report the owner sends gets a cause + fix in ANDROID-PORT-METHOD §8 |
| 7.8 | Controls not in harmony with the character/world | **Partly; waiting for the owner's clarification** | What could be inferred: smoother stick (7.4), camera smoothing default 0.2, voxel-style icons and Veloren colours. Asked the owner what exactly felt off |
| 7.9 | Launcher icon (owner's screenshot) | **Built; checked with `aapt2 dump badging`** | Legacy + adaptive icon (`mipmap-anydpi-v26`) generated from Veloren's official `net.veloren.veloren.png`: the "V" on `#387793`, safe-zone sized for round/squircle masks (MIUI). Generator `native/android/res_src/make_icon.py`. [Preview](evidence/dev7/launcher_icon_preview.png) |

Host unit tests: `native/touch-tests`, **20/20** (layout fits/no overlap at 16:9–22:9, contextual visibility, ability mask,
editor overlap guard, stick follow/stop).

## Firebase Test Lab results (virtual device only, by owner decision)

| Run | APK | Device | Outcome | Sc. 9 (dev.7) | Sc. 1 (regression) | Sc. 2 (layout persist) |
|---|---|---|---|---|---|---|
| vm11 | d7build3 | MediumPhone.arm v34 | gcloud exit 0, 0 PANIC | 8/9 (`ability_buttons`, see below) | **15/15** | **3/3** |
| vm12 | d7build4 (final) | MediumPhone.arm v34 | **NOT RUN**: `TEST_QUOTA_EXCEEDED` (Spark daily virtual-device quota used up by vm1–vm11 today); rerun after the daily reset | — | — | — |

vm11 `ability_buttons` FAIL `hotbar mask=00000` was a wrong expectation in the check, not a game bug. A new character
has no unlocked auxiliary abilities yet, so hotbar slots 1–5 are empty (visible in the video) and correctly no ability buttons show.
The check now verifies that each ability button is shown exactly when its hotbar slot is filled. d7build4 differs from
d7build3 only in this check (`android.rs`, test harness); no game code changed.

Evidence:
- [Before (dev.6)](evidence/dev7/before_dev6_buttons.jpg) / [after (dev.7)](evidence/dev7/vm11_new_buttons.jpg) buttons.
- [Glide button in Jump's place while airborne](evidence/dev7/vm11_glide_in_jump_spot.jpg).
- [Main menu on touch](evidence/dev7/vm11_main_menu_touch.jpg).

## Rebuild

```bash
cd $VELOREN_SRC && git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
for p in 0001-voxygen-android 0002-dev3-touch-controls 0003-dev4-mobile-perf 0004-dev5-lifecycle 0005-dev6-touch-ui 0006-dev7-phone-fixes; do
  git apply <repo>/native/patches/$p.patch; done
source native/env.sh
ANDROID_KEYSTORE=... <repo>/native/build_android.sh   # -> native/out/my-veloren-dev7.apk (vc70)
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=25m FTL_SCENARIOS=9,1,2 OUT=... tools/ftl.sh native/out/my-veloren-dev7.apk dev7-vm
```

Same signing key as dev.6, so dev.7 installs over dev.6 without uninstalling (saves kept). The touch layout resets
once to the new default (`layout_v7`).

## Patch and APK

| Item | Value |
|---|---|
| Patch | `native/patches/0006-dev7-phone-fixes.patch`, 32 files, +1521/−234, sha256 `8fda214a8eff96a8754a3eb19a3195b0f6baac352c973163ad00c6952ca897ea` |
| Patch check | applies cleanly on pin `585a91b4` + 0001–0005; result identical to the build tree |
| APK | `my-veloren-dev7.apk`, 0.1.0-dev.7 (vc70), 466,212,456 bytes, sha256 `423e73c74884119ed56c250e687a70bc07f91838749d4c506e295c7f3f7bf052` |
| Signing cert SHA-256 | `3620937abf3fc2b00394b5bdf600e59bd48138bdcf3760b22fb12f193c56ef14` (same stable key as dev.6) |
| Toolchain | Rust nightly-2026-06-13 (edition 2024: `gen` is a reserved word), NDK r27c, build-tools 34, JDK 17 |

## Known gaps / next

- **Physical gate = the owner's Poco F3:** 15 min on High without exit or annoying heat, comfort of buttons and stick.
- 7.7: crash reports from the phone (the dev.5 share dialog appears on the next start after a crash).
- 7.8: owner clarification of «غير نسق مع الشخصية والعالم».
- 7.5: Esc menu / settings windows still upstream style; tapping hotbar slots in game mode; moving the chat box off the
  stick area.
- The positive case of `ability_buttons` (filled slots → buttons visible) is covered by host tests only; a fresh VM
  character has no unlocked abilities.
