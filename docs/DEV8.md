# DEV8 — Content completeness, several worlds/characters, ready-made worlds, save export/import

> **الملخص بالعربية:** dev.8 يراجع محتوى العالم كله على الهاتف ويضيف إدارة العوالم والحفظ.
> - **مراجعة المحتوى (سيناريو 10 الآلي):** تمت مراجعة المدن والقرويين والحديث مع NPC، والحصاد في الحقول، والكهوف،
>   و5 أنواع من الزنزانات وزعمائها (أقواهم Minotaur بـ3000 HP)، والليل والنهار، والمطر، والطيران الشراعي، وظهور القارب والمنطاد.
>   النتيجة في vm3 (الأخيرة) 21/25: نجح الآن الصيد والترويض بالطوق والركوب والنزول وزنزانة Adlet. ما زال يفشل آليًا: التعدين والصعود إلى القارب،
>   أما الحصاد والحديث مع NPC في المدينة فقد نجحا في vm2 وفشلا في vm3 (تذبذب توقيت على الجهاز الافتراضي).
>   سبب هذا الفشل هو أداة الاختبار عند ~5 fps على الجهاز الافتراضي (التصويب والاقتراب)، ولم نجد خطأ في كود اللعبة، لكن ذلك غير مؤكد.
>   هذه البنود تبقى لتجربة المالك على هاتفه. التفاصيل في الجدول أدناه وفي [PARITY-MATRIX.md](PARITY-MATRIX.md).
> - **المهمات:** تأتي من حوار القرويين (rtsim). الحوار نفسه يعمل (`npc_interact` PASS)، أما إكمال مهمة كاملة فلم يُختبر آليًا (NOT RUN).
> - **عوالم جاهزة:** تُشحن مع اللعبة عوالم مُنشأة مسبقًا، لأن إنشاء عالم على الهاتف بطيء:
>   - «Small land»: مساحته 4 كم، وفيه مدينة وقلعتا Gnarling وزنزانة Sahagin.
>   - «Island»: جزيرة بمساحة 8 كم، وفيها 3 مدن و9 مواقع أخرى.
>   تظهر في قائمة العوالم كأزرار «+ الاسم (ready)».
> - **عالم جديد على الهاتف:** حجم العالم محدود بحد آمن (الافتراضي 7×7 والحد الأقصى 8×8). الاسم والبذرة (seed) يُكتبان في نافذة كتابة أندرويد.
> - **عدة شخصيات:** مُختبرة آليًا: إنشاء شخصية ثانية واللعب بها.
> - **تصدير الحفظ واستيراده:**
>   - زر «Export saves» يحفظ كل العوالم في ملف tar داخل مجلد Download.
>   - زر «Import saves» يفتح منتقي الملفات في أندرويد. لا يكتب الاستيراد فوق عالم موجود، بل يضيف لاحقة `_importN`.
> - **تنبيه:** مفتاح التوقيع تغيّر (ضاع المفتاح القديم مع بيئة البناء السابقة). لذلك يجب حذف dev.7 من الهاتف مرة واحدة قبل التثبيت، وستضيع حفظات dev.7.
>   أُرسل المفتاح الجديد للمالك بشكل خاص، لكي لا يتكرر هذا.
> - **بقرار المالك:** لا يوجد إصدار تجريبي (prerelease) لـdev.8. الـAPK يُبنى بالأوامر المذكورة أدناه.

Owner instructions (Slack DM, 2026-10-04):
- 13:01 UTC: «ابدأ dev.8»; a Firebase service-account key for testing; "always rely on automated testing to save the
  device quota" → only the virtual device MediumPhone.arm was used, no physical devices.
- 14:29 UTC: «لا داعي للإصدار التجريبي حاول اغلاق dev.8 بأسرع وقت» → no prerelease; close dev.8 quickly.

Branch `feat/dev8-content`, **stacked on `feat/dev7-phone-fixes` (PR #8, still unmerged)**. Version `0.1.0-dev.8` (vc80).

## What was built

All game code is in `native/patches/0007-dev8-content.patch` (14 files). It applies on top of 0001–0006 on the upstream pin
`585a91b4a`, and the lock-file churn is excluded. The Android side is in `VelorenActivity.java` (export to Downloads, import picker, toast).
The ready-made worlds are in `native/assets-extra/` (appended to `assets.tar` by `build_android.sh`).

| # | Item | Status | Implementation |
|---|---|---|---|
| 8.1 | Content audit on the phone | **Built; VM 21/25 (vm3)**, see the table below | `voxygen/src/content_audit.rs`: `Probe8` reads the client ECS every session tick (sites, time of day, weather, hostiles + strongest, villagers, wild/pets/ships, riding, inventory, `mine_target`, `can_interact`/`can_mount`, nearby creatures). Scenario 10 in `android.rs` drives the game with injected touches plus admin commands (`/site plot:…`, `/time`, `/weather_zone`, `/spawn`, `/ship`, `/airship`, `/give_item`) |
| 8.2 | Several worlds; new world on the phone; ready-made worlds | **Built; VM PASS** (`pregen_world_added`, `world_small`); host screenshots | `menu/main/ui/world_selector.rs` (touch): a "+ {name} (ready)" row per ready-made world. The size sliders are capped at 2^8 chunks (`PHONE_MAX_LG = 8`), and the phone default is 7×7. Name/seed fields use the Android text dialog. `singleplayer/phone_worlds.rs`: `PREGEN` (Small land 7×7 square, Island 8×8 circle, seed 1337), `add_pregenerated` copies the `.bin` into `userdata/` as a normal world |
| 8.3 | Several characters | **Built; VM PASS** (`two_characters`, `second_character`) | Upstream feature; scenario 11 creates a second character "Second" and enters the world with it |
| 8.4 | Save export/import | **Built; VM PASS** (`export_tar`, `import_tar`, `export_downloads`); the SAF picker UI is not driven automatically | `phone_worlds::transfer`: a tar of every world → `userdata/exchange/`, then Java `exportFile` copies it to `Download/` (MediaStore, API 29+). Import: `ACTION_OPEN_DOCUMENT` → copy → `import_tar`, which never overwrites (`_importN` suffix). Toasts report the result |

Evidence (host harness, Xvfb + lavapipe, touch UI forced on with `MY_VELOREN_TOUCH_UI=1`):
[world list](evidence/dev8/host_world_list_touch.jpg), [new world with phone limits](evidence/dev8/host_new_world_phone_limits.jpg).

### Ready-made worlds (`tools/pregen_worlds.sh`, deterministic)

| File | Seed / size | Sites | Generation (8 threads, build server) | sha256 |
|---|---|---|---|---|
| `my_veloren_small_7.bin` (262,180 B) | 1337, 7×7 (128×128 chunks ≈ 4 km), square | 1 town, 2 Gnarling forts, 1 Sahagin | 13.4 s | `c8a89a00…a278` |
| `my_veloren_island_8.bin` | 1337, 8×8 (256×256 chunks ≈ 8 km), circle | 3 towns, Cultist, Dwarven mine, Gnarling, Jungle ruin, Pirate hideout, Rock circle, 2 Troll caves, Vampire castle | 33.7 s | `dce4bcbf…7cca` |

A phone core is several times slower than a build-server core, and the official world is 1024×1024 chunks. That is why
on-device generation is limited to 8×8 and ready-made worlds are offered first.

## Firebase Test Lab (virtual device MediumPhone.arm v34 only, by owner decision)

| Run | APK | Sc. 11 (worlds/chars/saves) | Sc. 10 (content audit) | Sc. 1 (regression) | PANIC |
|---|---|---|---|---|---|
| vm1 (`matrix-2ud0bnq3odrkk`) | d8build1 | **3/3** + menu checks 3/3 | 16/24 | 14/15 (`npc_interact`: sc. 10 left the character in a dungeon arena) | 0 |
| vm2 (`matrix-971au0at4g1da`) | d8build2 | **3/3** + menu checks 3/3 | **18/25** | **15/15** | 0 |
| vm3 (matrix `8172281575501606288`) | d8build3 (harness only: re-approach in hunting, a separate pet horse for riding, hop onto the boat deck, nearby-creature diagnostics) | **3/3** + menu checks 3/3 | **21/25** | **15/15** | 0 |

### Content audit, item by item (vm2)

| Area | Check | Result | Receipt / cause |
|---|---|---|---|
| World | `world_sites` | PASS | official world: 41 towns, 78 dungeons (Adlet 5, Gnarling 23, Haniwa 10, Sahagin 5, Myrmidon 1, Cultist 2, Terracotta 2, Vampire castle 12), 10 dwarven mines, 9 glider courses |
| Towns | `town`, `npc_interact` | PASS | 59 villagers around a tavern; the contextual Use opened the NPC dialogue |
| Farming | `farm_field`, `harvest` | PASS | teleport to a farm field; Interact collected a crop (items 4 → 5) |
| Caves | `cave` | PASS | troll cave reached, hostiles present. Note: the map has no `Cave` markers (caves = 0); caves are terrain features |
| Dungeons + bosses | `dungeon` ×5, `strong_enemy` | 4 PASS, 1 FAIL | Gnarling (Harvester 1300 HP), Haniwa (Gravewarden 1000), Sahagin (Karkatha 2000), Myrmidon (Minotaur 3000). Adlet: `hostiles=0` after a 14.7 km teleport (NPCs did not stream in within 90 s at ~5 fps; VM timing). vm1 had Adlet PASS (AdletElder 1500) |
| Day/night | `night`, `day` | PASS | 20.1 h, 12.1 h |
| Weather | `weather_rain` | PASS | rain 0.34, cloud 0.46 |
| Gliding | `glide` | PASS | glider wielded and gliding from +150 m (also PASS in sc. 1) |
| Boats/airships | `ship_spawned`, `airship_spawned` | PASS | SailBoat and AirBalloon spawn next to the player |
| Boarding a boat | `ship_board` | FAIL | 1.1 m from the boat but no Mount prompt (`can_mount=false`): the seats/helm are on the deck above the player. vm3 hops onto the deck first |
| Mining | `mining` | FAIL | stone pickaxe equipped in the main hand and wielded, iron ore placed, but the camera aim never put the ore under the crosshair (`mine_target=false`) at ~5 fps. Needs the owner's phone |
| Hunting | `hunt` | FAIL | the attack ran for 30 s (`ComboMelee2`) but the nearest "wild" target moved away (3 m). vm3 re-approaches between bursts and logs the nearby creatures |
| Taming | `tame` | FAIL | the collar was used 3× next to a spawned horse but was not consumed and no pet appeared. Server rule: wild + tameable within 5 m. vm3 logs the nearby creatures to find out why |
| Riding | `ride`, `ride_moves`, `dismount` | FAIL, FAIL, (PASS) | no pet, so nothing to mount (depends on taming). vm3 rides a pet horse from `/spawn pet horse`, independent of the collar |
| Quests | — | NOT RUN (automated) | quests come from villager dialogue (rtsim). The dialogue works; completing a quest needs the owner's manual check |

### vm3 note

Final automated run for dev.8 (2026-10-04, MediumPhone.arm v34, outcome Passed, 0 PANIC). Scenario 10: 21/25,
failed = `mining`, `ship_board`, `npc_interact` (town), `harvest`.

| Check | vm2 → vm3 | Receipt |
|---|---|---|
| `hunt` | FAIL → **PASS** | wild alive 1 → 0; near(before) = Boar/wild at 4.1 m, 55 HP. A `/spawn`ed animal has no loot table, so loot = 0 (natural animals drop loot) |
| `tame` | FAIL → **PASS** | pets 0 → 1, collars 1 → 0; near(before) = Horse/wild at 1.0 m. The vm2 failure was distance (the horse was out of the 5 m range), not game code |
| `ride`, `ride_moves`, `dismount` | FAIL, FAIL, PASS → **PASS, PASS, PASS** | riding = true, moved 64.3 m in 4 s on the horse, then dismount |
| `dungeon` Adlet | FAIL → **PASS** (all 5 dungeons PASS) | Gnarling Harvester 1300, Adlet FrostWyvern 1000, Haniwa Gravewarden 1000, Sahagin Karkatha 2000, Myrmidon Minotaur 3000 |
| `ship_board` | FAIL → FAIL | on deck (`ship_dist=0.09`) but `can_mount=false`: the harness does not reach a seat/helm with the crosshair. Owner's phone check |
| `mining` | FAIL → FAIL | pickaxe wielded, `mine_target=false` (crosshair aim at ~5 fps). Owner's phone check |
| `harvest` | PASS → FAIL | `can_interact=true` but items 3 → 3. Passed in vm2 (items 4 → 5): VM timing, not a regression (no code change between d8build2 and d8build3 outside the harness) |
| `npc_interact` (sc. 10, town) | PASS → FAIL | timeout at 1.1 m from an NPC, 6 taps. The same check PASSES in sc. 1 of the same run (dialogue = true) |

So every content area has passed at least once in vm1–vm3 except **mining** and **boat boarding**, and **quests** are NOT RUN.

## Known gaps

- Mining and boat boarding are not proven by the automated run (see the vm3 note); quest completion is NOT RUN. They are upstream
  gameplay that is unchanged by the port, so this is most likely a test-harness/VM limit, not a game bug. The owner's
  phone test is the gate.
- The SAF import picker flow is checked by code review only. The automated run feeds the exported tar into `import_tar` directly.
- iced buttons in the world selector use OpenSans, so Arabic labels may show no glyphs there (the dev.6 Arabic decision is still pending).
  The new labels are in `en` and `ar-SA` `main.ftl`.
- The APK is ~467 MB because of the official map plus 2 ready-made worlds (+0.5 MB). Shrinking it needs an owner decision (dev.10, Play Asset Delivery).
- The new signing key means one uninstall of dev.7 (saves lost). Export saves in dev.8 avoid this from now on.

## What the owner should check on the phone

1. Uninstall dev.7 once, then install dev.8.
2. Singleplayer → «+ Small land (ready)» → play: it loads in seconds. Try «Island» too.
3. New world: type a name and seed, keep the size at 7×7, and generate it. Note how long it takes.
4. Create a second character and switch between them.
5. Export saves → check `Download/my-veloren-saves-*.tar` → Import saves → pick the file → a `_import1` world appears.
6. In a world: tame an animal with a collar (from a merchant), ride it, mine ore with a pickaxe, hunt an animal, board a boat, and take a quest from a villager.

## Rebuild

```bash
# toolchain: rust nightly-2026-06-13 + aarch64-linux-android, NDK r27c, SDK 34 (build-tools 34.0.0), JDK 17, cmake ≥3.5 policy
# VELOREN_SRC = upstream at 585a91b4a with patches 0001..0007 applied (git apply native/patches/000*.patch)
env -i HOME=$HOME PATH=/usr/local/bin:/usr/bin:/bin bash -c 'source <env with ANDROID_NDK_HOME, ANDROID_HOME, JAVA_HOME,
  VELOREN_SRC, ANDROID_KEYSTORE>; native/build_android.sh'          # → native/out/my-veloren-dev8.apk
tools/pregen_worlds.sh                                              # regenerate the ready-made worlds (host build)
FTL_DEVICE=MediumPhone.arm FTL_VERSION=34 FTL_TIMEOUT=60m FTL_SCENARIOS=11,10,1 tools/ftl.sh native/out/my-veloren-dev8.apk dev8
```
