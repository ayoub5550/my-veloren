# DEV6 — Full touch UI (windows, chat keyboard, character name, UI scale, Arabic)

> **الملخص بالعربية:** dev.6 يجعل نوافذ اللعبة قابلة للاستعمال باللمس. داخل النوافذ: النقرة تصبح نقرة فأرة،
> والضغط المطوّل يعرض تفاصيل العنصر (وعلى الخريطة يضع علامة)، والنقر المزدوج يستعمل العنصر،
> والسحب بإصبعين يمرّر القوائم ويكبّر الخريطة.
> الشات واسم الشخصية يُكتبان بلوحة مفاتيح أندرويد الحقيقية (نافذة نص)، **والعربية تعمل**: الحروف تتصل
> وتُعرض من اليمين إلى اليسار في الشات وقائمة اللغات.
> حجم الواجهة يُضبط تلقائيًا حسب كثافة الشاشة (1.5 على هاتف 420 dpi).
> على الجهاز الافتراضي في Firebase (vm8): إنشاء شخصية باسم «أيوب» نجح، وإرسال «مرحبا Veloren 123» في الشات نجح،
> وسيناريو 1 القديم نجح 14/14. اختبارات الإيماءات داخل النوافذ فشلت في vm8 لأن حقل الشات بقي مفتوحًا بعد الإرسال
> فتعطّلت أزرار الواجهة. هذا الخلل أُصلح. في إعادة الاختبار الأخيرة (vm10، البناء النهائي) نجحت كل الفحوص:
> سيناريو 8 نجح 1/1 وسيناريو 7 نجح 7/7 (الضغط المطوّل أظهر تفاصيل «Minor Potion»، والنقر المزدوج شرب الجرعة
> فصارت «Empty Vial»). الاختبار على هاتف حقيقي لم يُجرَ بعد (مؤجل بطلب المالك).
> الترجمة العربية في Veloren نفسها قليلة جدًا (44 من 4254 نصًا)، وترجمتها كاملة تحتاج قرارًا من المالك.

Owner instructions (Slack, 2026-10-03):
- 23:25 UTC: «اجلها وابدأ dev.6»: postpone the physical Firebase runs and start dev.6.

Branch `feat/dev6-touch-ui` (stacked on `feat/dev5-lifecycle`, PR #6). Version `0.1.0-dev.6` (vc60).

## What was built

All game code is in `native/patches/0005-dev6-touch-ui.patch`. It applies on top of 0001–0004 on the upstream pin
`585a91b4a`. Lock-file churn is excluded. The Java side is `VelorenActivity.showTextInput` + `nativeText`.

| # | Item | Status | Implementation |
|---|---|---|---|
| 6.1 | Gestures inside windows (bag, equipment, trade) | PASS (VM, vm10); physical NOT RUN | `touch.rs` window mode: tap = left click on release; drag past 2.5 % of the height = mouse down at the start point and follow (drag and drop); long-press 0.45 s = hover (item tooltip); double-tap within 0.35 s = right click (use/equip). 19/19 host tests (`native/touch-tests/run.sh`), 6 of them new |
| 6.2 | Crafting / skill tree scrolling | PASS (VM, vm9+vm10); physical NOT RUN | Two fingers moving together = pixel scroll events (winit `MouseWheel::PixelDelta`) for conrod lists. Bigger UI through 6.6 |
| 6.3 | Map: pinch zoom + marker | PASS (VM, vm9+vm10); physical NOT RUN | When the map is open, two-finger pinch = wheel lines (map zoom). Long-press on the map = middle click = upstream `MapSetMarker` |
| 6.4 | Android keyboard (IME) for chat and commands | **PASS (VM)** | When a HUD `TextEdit` captures the keyboard (rising edge of `Hud::typing()`), Rust asks Java for a native `AlertDialog` + `EditText` (full Android keyboard, any language). The result comes back through `nativeText(String, boolean)`, registered with `RegisterNatives` (a `NativeActivity` never calls `System.loadLibrary`, so JNI cannot find it by name). The text goes to conrod as `Input::Text` + Return, then the chat field is released (Escape) so the HUD buttons work again. Cancel = Escape |
| 6.5 | Manual character creation | **PASS (VM)** | The creation form is the upstream one (body, species, weapon, start area). Focusing the name field opens the same text dialog; the result becomes `Message::Name`. The automatic creation stays only for Test Lab runs. Scenario 8 drives the real form: New character → focus name → dialog «أيوب» → Create |
| 6.6 | UI scale by screen density | **PASS (VM)** | First start after install: `ScaleMode::Absolute(clamp(0.57 × dpi/160, 1.0, 2.5))`; 420 dpi → 1.50. Applied once (marker `ui_scale_preset`, version `dev6-v1`); later changes in Settings are kept |
| 6.7 | Arabic (RTL) text | **Works for display** (see below) | New `voxygen/src/ui/rtl.rs`: Arabic shaping (presentation forms, lam-alef ligatures) + a simple bidi pass (RTL runs reversed, numbers and Latin kept left-to-right, brackets mirrored), applied per wrapped line in conrod and per line in iced |

### 6.7 Arabic — findings

- Veloren ships `assets/voxygen/i18n/ar-SA` with **44 of 4254** strings translated (≈1 %). The `ar` folder has only an
  empty `command.ftl`. Choosing Arabic today shows a mostly English UI.
- The "universal" font (GoNotoCurrent, used by chat and the language list) contains Arabic and the presentation
  forms A/B. The default UI fonts (OpenSans, haxrcorp) have no Arabic, so translated strings in those widgets need
  a font fallback (not done; it only matters once a translation exists).
- conrod and iced do no shaping and no bidi, so Arabic was drawn as separate letters in the wrong order. `rtl.rs`
  fixes that for display; 6 unit tests. Verified on the host build (language list) and on the Test Lab VM (chat
  line `أيوب: مرحبا Veloren 123`, see evidence).
- Not done (needs an owner decision): a full Arabic translation, and switching to Arabic automatically on
  Arabic-locale phones.

## Firebase Test Lab results

| Run | APK | Device | Outcome | Sc. 8 | Sc. 7 | Sc. 1 |
|---|---|---|---|---|---|---|
| vm8 | d6build1 | MediumPhone.arm v34 (llvmpipe) | gcloud exit 0, no PANIC | **1/1** | 2/7 (see below) | **14/14** |
| vm9 | d6build2 | MediumPhone.arm v34 | gcloud exit 0, no PANIC | **1/1** | 5/7 (see below) | — |
| vm10 | d6build3 (final) | MediumPhone.arm v34 | gcloud exit 0, no PANIC | **1/1** | **7/7** | — |
| physical | — | r8q / e3q | NOT RUN (postponed by the owner; Spark daily quota) | | | |

vm8 details:
- Scenario 8: the name dialog opened from the real creation form, «أيوب» was typed through the Android keyboard
  path, and the created character is named «أيوب» ([dialog](evidence/dev6/vm8_char_name_dialog.jpg)).
- Scenario 7: `ui_scale` PASS (Absolute 1.50 at 420 dpi), `chat_ime` PASS: the message came back from the server
  and is drawn joined and right-to-left ([chat](evidence/dev6/vm8_chat_arabic.jpg)). The 5 window-gesture checks
  failed because no window opened: upstream keeps the chat field focused after Enter (desktop players press Enter
  or Esc again), and while it is focused the HUD ignores game keys, so the Bag/Craft/Map taps did nothing.
  Fix: release the field 3 frames after a submitted dialog; `chat_ime` now also checks that the field is released.
- Scenario 1 (dev.3 regression): 14/14, including `analog_fast` (the vm7 failure was a VM flake).

vm9 details (d6build2, chat fix included):
- Scenario 8: 1/1 again (character «أيوب»).
- Scenario 7: 5/7. `ui_scale` PASS (Absolute 1.496, 2400x1080, 420 dpi); `chat_ime` PASS with `field_released=true`
  ([chat released](evidence/dev6/vm9_chat_released.jpg)); `two_finger_scroll` PASS, 13 scroll events
  ([craft list + bag](evidence/dev6/vm9_craft_scroll_bag.jpg)); `map_pinch_zoom` PASS, zoom 10 → 16
  ([before](evidence/dev6/vm9_map_before_pinch.jpg), [after](evidence/dev6/vm9_map_after_pinch.jpg));
  `map_marker` PASS, marker `Some([19173, 18645])` ([tooltip "Marked Location"](evidence/dev6/vm9_map_marker.jpg)).
- `long_press_tooltip` and `double_tap_use` FAIL with `VEL-STEP ui point inv_first unknown`: a test-harness bug.
  The `inv_first` UI point was registered in `InventoryScroller` (the widget used by the trade window), not in
  `InventoryMenu` (the live bag), so the script never knew where the first bag slot was. Fix: register it after the
  `SlotGrid` loop in `InventoryMenu::update` (touch mode only). No gesture code changed.

vm10 details (d6build3, final APK, sha256 `d6396fed…3eacd`):
- Scenario 8: 1/1 (`char_name_dialog`, `char_name_arabic` = «أيوب»).
- Scenario 7: **7/7**, 0 PANIC. `ui_scale`, `chat_ime` (`field_released=true`), `long_press_tooltip`
  (bag open, long-press at (0.766, 0.211) for 1.6 s, inventory unchanged; the Minor Potion tooltip is shown:
  [evidence](evidence/dev6/vm10_longpress_tooltip.jpg)), `double_tap_use` (inventory changed: the potion was drunk
  and became an Empty Vial, buff icon above the hotbar: [evidence](evidence/dev6/vm10_doubletap_used.jpg)),
  `two_finger_scroll` (11 events), `map_pinch_zoom` (10 → 16), `map_marker` (None → `Some([21420, 8871])`).

## Rebuild

```bash
cd $VELOREN_SRC && git checkout 585a91b4a76fcf5df7a4851127cf5907a3ce34df
for p in 0001-voxygen-android 0002-dev3-touch-controls 0003-dev4-mobile-perf 0004-dev5-lifecycle 0005-dev6-touch-ui; do
  git apply <repo>/native/patches/$p.patch; done
source native/env.sh
<repo>/native/build_android.sh                 # -> native/out/my-veloren-dev6.apk (vc60)
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=25m FTL_SCENARIOS=8,7,1 OUT=... tools/ftl.sh native/out/my-veloren-dev6.apk dev6-vm
```

Scenario 8 must run first on a fresh install (it creates the character the other scenarios use).

**Signing key:** dev.4, dev.5 and the first dev.6 build were each signed with a different throw-away debug key, so
installing a new build over an old one failed and needed an uninstall (which deletes saves). From dev.6 on,
`build_android.sh` uses `ANDROID_KEYSTORE` (one stable key kept outside the repo). Updating from dev.5 to dev.6 still
needs one uninstall; later updates install over it.

## Patch and APK

| Item | Value |
|---|---|
| Patch | `native/patches/0005-dev6-touch-ui.patch`, 12 files, +1360/−43, sha256 `547c2ff1d89078efbb57f803b5db8582eb4c4aacc8a943edcd66ecb443d3bcb6` |
| Patch check | applies cleanly on pin `585a91b4` + 0001–0004; result identical to the build tree |
| APK | `my-veloren-dev6.apk`, 0.1.0-dev.6 (vc60), 466,096,818 bytes, sha256 `d6396fed0022ec454fe0a335d3468021ec00fc7c8893f6d669cf64aaeeb3eacd` |
| Signing cert SHA-256 | `3620937abf3fc2b00394b5bdf600e59bd48138bdcf3760b22fb12f193c56ef14` (stable key, `apksigner verify` OK) |
| Toolchain | Rust nightly-2026-06-13, NDK r27c, build-tools 34, JDK 17 |

## Known gaps / next

- Physical runs (r8q, e3q): carried from dev.4/dev.5 (30 fps gate, lifecycle, occlusion A/B), plus dev.6 gestures on
  a real touchscreen.
- The chat box sits over the joystick area in the bottom-left; the stick still wins the touch, but moving the chat
  box is a later polish item.
- Full Arabic translation and Arabic font fallback for OpenSans/haxrcorp widgets: owner decision.
- Notch/safe-area inset: still letterboxed.
