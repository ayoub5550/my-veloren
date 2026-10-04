# Content and behavior coverage matrix

**dev3 status (2026-10-03, native port):** Camera/movement (touch stick/camera/pinch/glide) and
UI touch (menu bar, context buttons, layout editor) are **IMPLEMENTED for touch** with Test Lab evidence:
[DEV3.md](DEV3.md). Under the native port (ADR-002), gameplay rows run the upstream code as-is;
Android-specific gaps are performance (dev4: mobile tiers done, physical 30 fps gate NOT RUN, see [DEV4](DEV4.md)) and lifecycle (dev5: pause/resume, autosave, Back, crash report PASS on VM, see [DEV5](DEV5.md)) and touch UI (dev6: window gestures, Android keyboard for chat/name, DPI UI scale, Arabic display: 7/7 + 1/1 PASS on VM, physical NOT RUN; see [DEV6](DEV6.md)) and the owner's phone fixes (dev7: phone-safe Ultra + thermal guard, icon buttons without overlap, floating stick, touch-only controls settings, new launcher icon: VM 15/15 + 3/3 + 8/9 (harness check fixed), owner's Poco F3 is the physical gate; see [DEV7](DEV7.md)).

**dev1 status (2026-10-03, archived Unity slice):** Voxel geometry, Character creation, Equipment visuals, Procedural
animation, Camera/movement, World generation/rendering and Android robustness are **STARTED**
(sample/placeholder level, see [DEV1.md](DEV1.md)); APK built, real-device smoke NOT RUN.
All other rows: NOT STARTED. Expand each broad row into per-family/per-feature cases when implementation
is authorized. Every exception needs a reason, owner decision and impact on advertised scope.

| Area | First evidence gate | Full-scope follow-up | Milestone |
|---|---|---|---|
| Raw asset corpus | Complete pinned path/hash inventory | Every upstream file accounted for | dev2 |
| Voxel geometry | Golden single/multi-model and malformed fixtures | Every required model decodes without silent fallback | dev1–2 |
| Palette/material semantics | Hollow/mirror/offset/emissive/recolor cases | Surface and equipment combinations across families | dev2–3 |
| Data/manifests | Typed RON and stable-ID references | Every required enum/field/dependency mapped | dev2–6 |
| Character creation | One valid assembled humanoid | Required appearances/races/body variations | dev3–6 |
| Equipment visuals | Weapon + armor correctly attached | All required slots/items and visibility rules | dev3–6 |
| Procedural animation | Idle/move/jump/attack/hit/death | Each required skeleton and state transition | dev3–6 |
| Camera/movement | Touch + collision + pause — **dev3: touch stick/camera/pinch/glide PASS on r8q** ([DEV3](DEV3.md)); pause = **dev5: home/return ×5 + save after kill PASS on VM** ([DEV5](DEV5.md)) | Swimming/climbing/gliding/mounts if required by agreed parity | dev3–6 |
| World generation | Repeatable seed and chunk boundaries | Biomes/sites/dungeons and original rules comparison | dev4–6 |
| World rendering | Terrain/water/foliage with bounded meshes | Lighting/weather/time-of-day effects required by scope | dev4–7 |
| Local simulation | Actors progress without a network transport | Server/world/rtsim responsibilities required for offline experience | dev4–6 |
| Combat | One melee weapon and enemy with correct damage | Weapon families, abilities, projectiles, effects, status rules | dev5–6 |
| AI | Detect/chase/attack/recover/death | Creatures, NPC roles, navigation and encounter behavior | dev5–6 |
| Inventory/progression | Loot/equip/use/craft/save | Items, stats, recipes, skill/progression and balance tables | dev5–6 |
| NPC interaction/economy | One agreed interaction | Trading/dialogue/quest-like systems present in selected upstream scope | dev6 |
| Persistence | Kill/relaunch preserves world/player | Migration, corruption recovery, large-world stress | dev4–8 |
| Audio | Nonzero decoded audio, played on device | Music/ambience/spatial feedback and manual sound check | dev2–7 |
| UI/localization | Usable menus/touch safe areas — **dev3: menu bar, context buttons, layout editor, 16:9–22:9 layout tests PASS** ([DEV3](DEV3.md)) | FTL semantics, Arabic shaping/RTL/glyph and accessibility tests | dev3–7 |
| Offline distribution | Fresh complete install launches in airplane mode | All core paths work with no first-run download/account | dev5–8 |
| Android robustness | ARM64/IL2CPP artifact and real-device smoke | Lifecycle/thermal/memory/page-size/upgrade matrix | dev1–8 |
| Provenance/distribution | Original attribution and component map | Release obligations and engine integration resolved | dev1–8 |

## Explicit exclusions or separate decisions

- Online multiplayer, public matchmaking, public accounts and remote economy: excluded from
  the offline product; do not replace local AI/simulation with empty stubs.
- Original Rust save import: separate optional converter, not implied by new local saves.
- Original-world/physics/combat bit-for-bit equivalence: unproven; require comparison tests.
- Selling, rebranding or store submission: not authorized by this roadmap request.
- Executing any development milestone: requires a new owner instruction.

## Future evidence row template

`feature_id | upstream_source@commit | required_behavior | fixture_ids | implementation |
source_asset_ids | editor_result | android_result | offline_result | evidence_sha | status`.

Use NOT RUN for missing execution. Use BLOCKED for known missing prerequisites.
Use NOT APPLICABLE only with an explicit scope decision, never to hide unimplemented content.
