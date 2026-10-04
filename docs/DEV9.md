# DEV9 — Gamepad, Bluetooth keyboard and mouse, multiplayer on the official Veloren servers

> **الملخص بالعربية:** dev.9 يضيف التحكم بالملحقات واللعب الجماعي.
> - **يد التحكم (Gamepad):** العصا اليسرى للمشي، والعصا اليمنى للكاميرا، وRT للهجوم، وA للقفز، وD-pad يمين للحقيبة، وStart لقائمة Esc، وB/Start للرجوع داخل النوافذ.
>   الأزرار كلها هي أزرار Veloren الأصلية للذراع. عند استعمال الذراع تختفي أزرار اللمس، وتعود عند لمس الشاشة.
> - **لوحة مفاتيح بلوتوث:** WASD والمسافة وI وEsc وكل اختصارات Veloren. الحروف العربية في لوحة المفاتيح تُحوَّل إلى مواضعها الإنجليزية (W، A، S، D…)، لكي يعمل اللعب على أي تخطيط.
> - **فأرة بلوتوث:** تدوير الكاميرا (مع Pointer Capture)، والنقر للهجوم، والعجلة للتقريب.
> - **اللعب الجماعي على خوادم Veloren الرسمية (بأمر المالك):** في القائمة الرئيسية لوحة فيها اسم المستخدم وكلمة السر والخادم، وزر «Multiplayer»، وقائمة الخوادم، وزر «إنشاء حساب مجاني» يفتح veloren.net.
>   نُجح آليًا: الدخول بحساب المالك `ayoubteke` إلى «Official Veloren Server»، وقبول القواعد، وإنشاء شخصية «Ayoub»، ودخول العالم (24 لاعبًا متصلًا)، والمشي على الخادم.
> - **إصلاحات:** شهادات TLS على أندرويد (لم يكن الاتصال الآمن بخادم الحسابات يعمل)، وزر المسافة في لوحة المفاتيح (القفز).
> - **ينتظر هاتف المالك:** ربط ذراع وفأرة ولوحة مفاتيح حقيقية عبر البلوتوث. في الاختبار الآلي تُحقن الأحداث في نافذة التطبيق نفسها، لا عبر البلوتوث.
> - **مهم:** الخادم الرسمي يتحدّث كثيرًا. إذا تغيّر بروتوكوله سيرفض نسخة اللعبة، ويلزم عندها إعادة بناء على نسخة Veloren أحدث.
> - يُثبَّت فوق dev.8 مباشرة (نفس مفتاح التوقيع)، والحفظ يبقى.

Owner instructions (Slack DM, 2026-10-04):
- 14:59 UTC: «جيد قم بالدمج وابدأ في dev. 9» → PR #8 (dev.7) and PR #9 (dev.8) merged; dev.9 started.
- 15:10 UTC: «اذا كان اللعب الجماعي ممكن فأضفه بإتصال بخوادم veloren الرسمية فأضفه اذا لا امحيه» → multiplayer on
  the official servers is possible, so it was added (no LAN hosting).
- 15:44 UTC: the owner created a veloren.net account for testing (username `ayoubteke`). The password is kept only
  outside the repo (`FTL_MP_ACCOUNT` file on the build machine), never in the APK, the repo or the docs.
- Standing: automated tests on the virtual device only (save the device quota); no prerelease.

Branch `feat/dev9-input` from `main` 6f218a7 (dev.7 + dev.8 merged). Version `0.1.0-dev.9` (vc90), same signing key as
dev.8, so it installs over dev.8 and keeps the saves.

## What was built

All game code is in `native/patches/0008-dev9-input-multiplayer.patch` (14 files). It applies on top of 0001–0007 on the
upstream pin `585a91b4a`. winit 0.30.13 is vendored by `native/tools/vendor_winit.sh` (checked against the Cargo.lock
checksum, then patched with `native/patches/winit-0.30.13-android-input-hook.patch`); patch 0008 points
`[patch.crates-io] winit` at it. The Android side is in `VelorenActivity.java` (pointer capture, `openUrl`, test injection).

| # | Item | Status | Implementation |
|---|---|---|---|
| 9.1 | Gamepad | **Built; VM PASS** (walk, camera, jump, attack, Esc menu, Back) | winit's Android backend drops gamepad/joystick events, and gilrs has no Android backend. A small hook in the vendored winit (`winit::platform::android::set_input_hook`) gives every raw `InputEvent` to `voxygen/src/android_input.rs` first. Keys of a gamepad source (A/B/X/Y, shoulders, Start/Select, thumbs, D-pad) and joystick axes (sticks, analog triggers with hysteresis, hat → D-pad) become the same `PadEvent`s that `Window::fetch_events` builds from gilrs on desktop, so the upstream bindings, layers and deadzones apply unchanged. Outside the game (menus, open windows) B and Start act as Back. No gamepad settings page (owner decision 7.6) |
| 9.2 | Bluetooth keyboard | **Built; VM PASS** | winit already delivers keys. Fixes: Android reports Space as `Character(" ")` (upstream binds `Named(Space)`), Tab and Enter likewise; Arabic letters from an Arabic layout are mapped to the US key at the same position, so WASD/I/M… work on any layout. Text fields still get the typed characters |
| 9.3 | Bluetooth mouse | **Built; VM PASS** | the hook reads mouse/touchpad MotionEvents: hover/relative move → camera (pointer capture requested while the game has the cursor, relative axes when captured), buttons → left/right click, `AXIS_VSCROLL` → wheel zoom |
| 9.4 | Touch overlay auto-hide | **Built; VM PASS** | the last device used decides: keyboard/mouse/gamepad hide the touch buttons, a touch brings them back |
| 9.5 | Multiplayer on the official servers | **Built; VM PASS with the owner's account** | phone login panel in the main menu (`menu/main/ui/login.rs`): Username / Password / Server fields open the Android text dialog; «Multiplayer», «Servers» (upstream server list) and «Create a free account» (opens https://veloren.net/account/). The rest is the upstream client: auth.veloren.net, server rules, character select/create |
| 9.6 | TLS certificates on Android | **Fixed; VM PASS** | rustls-native-certs found no CA store (`CertificateLoad("no native root CA certificates found")`). `android_main` sets `SSL_CERT_DIR` to `/apex/com.android.conscrypt/cacerts` and `/system/etc/security/cacerts` (those that exist) |

## Firebase Test Lab (virtual device MediumPhone.arm v34 only)

Scenario 12 injects keys, joystick and mouse events through the activity window's own input pipeline
(`VelorenActivity.inject` → `PhoneWindow.injectInputEvent` → ViewRootImpl input stages → NativeActivity InputQueue →
winit hook → game). That is the path of a paired device after the system InputDispatcher. A probe checks that a
MotionEvent reaches the hook; if Android ever blocks it, the harness switches to a labelled synthetic mode
(values fed to the same handlers). In vm9b/vm9d the probe confirmed **real Android MotionEvents**.

Scenario 13 logs in to the official server. With `FTL_MP_ACCOUNT` it uses the owner's account and plays; without it,
it uses a non-existent account and passes when the game server and auth.veloren.net both answer.

| Run | APK | Sc. 12 (input) | Sc. 13 (multiplayer) | Sc. 1 (regression) | PANIC |
|---|---|---|---|---|---|
| vm9a | d9build1 | 11/19 (motion events never arrived: `ViewRootImpl.dispatchInputEvent` is a hidden API on API 34; Space key mismatch) | 1/1, but a **false pass**: the error was the missing CA store, not the expected auth refusal | 15/15 | 0 |
| vm9b | d9build2 | **18/19**, failed `pad_bag_open` | **1/1** (no account): server + auth answered `ServerError(400 "The username + password combination was incorrect")` | **15/15** | 0 |
| vm9c | d9build2 + owner's account | — | **2/2** + `mp_server_rules` PASS (logged from the menu): `mp_in_world` (Official Veloren Server, 24 players online, `singleplayer=false`), `mp_walk` (5.0 m on the server); character «Ayoub» created on the account | — | 0 |
| vm9d | d9build3 (harness only: D-pad press checked, touch-mode diagnosis) | **19/19** (`pad_bag_open` PASS, input summary: keyboard 8, gamepad buttons 12, axes 4, mouse 30) | — | — | 0 |

### Input checks (scenario 12)

| Check | vm9b | Receipt |
|---|---|---|
| `kb_walk` | PASS | W held: 54.0 m, 2.13 m/s |
| `kb_jump` | PASS | Space: vertical speed 10 m/s (vm9a FAIL: Space arrived as `Character(" ")`) |
| `kb_bag_open` / `kb_bag_closed` | PASS / PASS | I opens and closes the bag |
| `overlay_hidden` / `overlay_back` | PASS / PASS | keyboard hides the touch overlay, a tap brings it back |
| `mouse_look` | PASS | yaw −2.57 rad from 22 mouse events, pointer capture granted |
| `mouse_attack` | PASS | left click → `ComboMelee2` |
| `mouse_wheel` | PASS | zoom 10.00 → 14.67 |
| `pad_walk` | PASS | left stick: 10.4 m |
| `pad_camera` | PASS | right stick: yaw 0.60 rad |
| `pad_jump` | PASS | A |
| `pad_attack` | PASS | right trigger → `ComboMelee2` |
| `pad_bag_open` | FAIL → **PASS** (vm9d) | D-pad right opens the bag. vm9b cause, logged in vm9d: the window was in touch mode (`windowTouchMode=true`) and ViewRootImpl consumed the first injected navigation key to leave touch mode, before the NativeActivity queue. The harness now checks the press and re-sends it with `FLAG_KEEP_TOUCH_MODE` |
| `pad_back_closes` | PASS (vm9b, vm9d) | B closes the window |
| `pad_esc_open` / `pad_esc_closed` | PASS / PASS | Start opens the Esc menu, Start again closes it |
| `overlay_hidden_pad` | PASS | gamepad hides the touch overlay |
| `input_summary` | PASS | keyboard keys 8, gamepad buttons 10, axes 4, mouse events 30; real MotionEvents |

## Known gaps

- **Real Bluetooth devices are the owner's check.** The VM injects events into the app's window; the system
  InputDispatcher, real device IDs, pointer capture with a real mouse and controller-specific key maps are not covered.
- **Server version drift:** the official server updates often. This build is upstream `585a91b4a` (2026-09-30) and the
  server accepted it on 2026-10-04. When the server's protocol changes, the client refuses to connect and the patches
  must be rebased on a newer upstream (the dev.10 "sync with upstream" script).
- **First D-pad press after touching the screen:** in vm9b/vm9d ViewRootImpl ate the first injected D-pad press while the
  window was in touch mode. A real Bluetooth device goes through the system InputDispatcher, which on recent Android
  leaves touch mode itself, so this is most likely a test-injection effect only; on an older Android version the first
  D-pad press after a touch might be lost. Owner's phone check (step 3 below).
- The Arabic-layout key remap is tested by code review only (the VM keyboard is US).
- Menus are driven by touch, Back, B and Start; there is no gamepad cursor for the iced menus.
- iced labels use OpenSans, so the Arabic title of the multiplayer panel may show no glyphs (dev.6 Arabic decision pending).
- Multiplayer needs internet and a free veloren.net account; singleplayer stays fully offline.

## What the owner should check on the phone

1. Install dev.9 over dev.8 (no uninstall needed); check the worlds and characters are still there.
2. Main menu → enter your username and password → Multiplayer → accept the rules → your character «Ayoub» → play.
3. Pair a Bluetooth gamepad: left stick walk, right stick camera, A jump, RT attack, D-pad right bag, B close, Start menu.
   Touch the screen, then press D-pad right once: the bag should open on the first press.
   The touch buttons should disappear while you use it and come back when you touch the screen.
4. Pair a Bluetooth keyboard and mouse: WASD, Space, I; mouse look, click attack, wheel zoom. Try with the Arabic layout too.

## Rebuild

```bash
# VELOREN_SRC = upstream at 585a91b4a with patches 0001..0008 applied (git apply native/patches/000*.patch)
native/tools/vendor_winit.sh "$VELOREN_SRC"          # winit 0.30.13 + input hook (build_android.sh also calls it)
env -i HOME=$HOME PATH=/usr/local/bin:/usr/bin:/bin bash -c 'source <env with ANDROID_NDK_HOME, ANDROID_HOME, JAVA_HOME,
  VELOREN_SRC, ANDROID_KEYSTORE>; native/build_android.sh'          # → native/out/my-veloren-dev9.apk
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=30m FTL_SCENARIOS=12,13,1 tools/ftl.sh native/out/my-veloren-dev9.apk dev9
FTL_MP_ACCOUNT=<file: username, password> FTL_SCENARIOS=13 tools/ftl.sh native/out/my-veloren-dev9.apk dev9-mp
```
