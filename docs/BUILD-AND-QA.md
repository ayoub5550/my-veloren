# Build and QA runbook specification

**Planning only. No tools below exist in this repository yet and no commands have been run
for a Unity project here.** Future developers must implement and verify the interfaces before
replacing this statement with real commands.

## 1. Toolchain decision

The owner's predecessor projects pin Unity **2022.3.62f3**, changeset `96770f904ca7`.
That is useful precedent, not a guarantee that it is the best supported baseline for this
new game's renderer, Android requirements or native libraries.

In dev1, record and justify:

- Unity exact version/revision and matching Android Build Support modules;
- render pipeline/package lock and all package versions;
- JDK, SDK platform, Build Tools, command-line tools, NDK and Gradle versions;
- scripting backend and ABI: **IL2CPP + ARM64 proposed**; do not assume ARMv7;
- graphics APIs: GLES3 baseline candidate, Vulkan only after device evidence;
- minimum/target Android SDK chosen for supported devices and current distribution rules;
- native-library page-size compatibility, including 16 KB devices where applicable;
- selected .NET test SDK, Python, and Rust only if used by conversion/oracle tools.

Do not mix NDK/JDK versions from different Unity versions. Do not copy historical SDK
numbers from old projects without verifying the selected editor/module combination.
Do not assume future CI quotas, licenses or paid resources from old project notes.

## 2. Fresh-agent environment check (after authorization)

Inventory: OS/architecture, usable CPU/RAM/disk, tool binaries/versions, licenses, Android
modules, graphics access, KVM availability and writable caches. A reported 17-core host
does not establish usable graphics acceleration or a licensed Editor.

Use named environment variables such as `UNITY_EDITOR`, `ANDROID_SDK_ROOT` and
`ANDROID_NDK_ROOT`; values are machine-specific and never hardcoded in source.
Credentials/activation files/signing material must come from approved secret storage.
Do not print secret values into logs or pass them into committed command examples.

Previous constrained environments sometimes needed shader-compiler/audio workarounds.
Reproduce a specific failure and validate a minimal fix first. Never install qemu/preload
shims by default, suppress all audio errors or treat zero imported sounds as success.

## 3. Future command interface — specification, NOT executable today

| Proposed interface | Responsibility |
|---|---|
| `python3 tools/doctor.py` | Validate toolchain, modules, paths and optional graphics support |
| `python3 tools/content.py inventory` | Compare source tree against pinned inventory |
| `python3 tools/content.py acquire` | Acquire and hash the complete raw corpus |
| `python3 tools/content.py convert --all` | Deterministic conversion + dependency/coverage report |
| `python3 tools/content.py verify` | Detect corruption, missing files, unresolved pointers and outputs |
| `python3 tools/unity.py compile` | Real Editor script compilation |
| `python3 tools/unity.py test` | Editor tests with machine-readable results |
| `python3 tools/unity.py prepare` | Generate known scenes/content; restore canonical scene list |
| `python3 tools/unity.py playtest` | Deterministic gameplay scenario and assertions |
| `python3 tools/unity.py android` | Build actual signed/test APK using pinned configuration |
| `python3 tools/verify_apk.py <artifact>` | Manifest, ABI, signatures, native libraries and checksum |

Do not run these names now: they are not implemented. The milestone author must add
`--help`, error semantics, inputs, bounded timeouts and receipts before documenting them
as supported commands. Include an offline-cache mode only when all required inputs exist.

Proposed order: doctor → acquire/verify → convert → compile → Editor tests → prepare/
restore scenes → playtest → Android → artifact checks → device/offline validation.
Any test that mutates generated scenes must be followed by restoration and validation.
Use one Unity Editor process per project cache; parallelize safe pure tests instead.

## 4. Test ladder and what each layer proves

| Layer | Proves | Does NOT prove |
|---|---|---|
| Pure C# / Python / converter tests | Parser bounds, hashes, deterministic algorithms, safety, schemas | Unity import, rendering or IL2CPP |
| Unity Edit Mode | Actual Unity compilation, serialization, asset references, importer behavior | Android shader variants or device input |
| Unity Play Mode | Scene lifecycle, game loop, interactions, save/reload assertions | Real phone thermals or performance |
| Rendered desktop probe | Visual references, missing/magenta materials, UI layout | Mobile FPS; software rendering is not a benchmark |
| Android artifact audit | Real manifest/ABI/content/signature, exact artifact hash | That a player can complete the game |
| Physical Android testing | Device-specific rendering, touch, memory, lifecycle and performance | Every device, audible quality without audio evidence, full content parity |
| Owner/manual validation | Comfort, sound, controls and expected gameplay | A replacement for automated regression |

Unity `-nographics` may serve non-rendering tests, never visual validation.
Xvfb plus a software renderer is a potential screenshot route, not a mobile performance proxy.
Firebase Test Lab physical devices are **real hardware**, but automation/video cannot alone
establish audio quality, gyro usability or human control comfort. Use explicit manual checks.

## 5. Regression lessons applied as gates

- Runtime shader lookup can cause stripping. Make shader dependencies/variants explicit,
  inspect player build logs and render each material family on Android.
  Do **not** assume `Resources.Load` is inherently stripped; inclusion and variant coverage
  are different questions.
- Zero unexpected fallback/magenta/white materials; golden samples for axis, UV, palette,
  gamma, transparency and equipment assembly.
- Generated GUIDs must be deterministic and unique; scene references must survive reimport.
- Do not clear shared registries in arbitrary `Awake` order; test reload/additive scenes.
- Use Unity-aware destroyed-object checks, not ordinary CLR null assumptions.
- Test IL2CPP/module stripping and shader keyword combinations in the player, not just Editor.
- Separate network/dependency download failures from source/compiler failures in build logs.
- Test the final artifact: manifest filters and scenes can differ from intermediate state.

## 6. Offline release scenario

1. Complete installation of the candidate and all install-time data.
2. Clear app data, enable airplane mode, then launch for the first time.
3. Create/select a world and character without login or external requests.
4. Explore, fight, acquire/equip an item, craft, save, background, kill and relaunch.
5. Verify character/world deltas, audio, loading screens and all menus function offline.
6. Test pause/resume, low storage, corrupted/truncated save and prior-good backup recovery.
7. Upgrade the candidate preserving data; verify migrations and version mismatch handling.
8. Inspect manifest permissions and traffic to catch accidental online services.

Disable unused Unity Services/Analytics, cloud authentication and advertising SDKs.
Audit the merged manifest, including any injected internet permission, and dependencies;
do not assume that an offline game loop means the application emits no traffic.

Core content must be packaged in the completed installation. No fast-follow/on-demand
download can be a hidden requirement for the promised first offline launch.

## 7. Provisional performance goals — not measurements

Choose named reference devices in dev1. Suggested starting targets for discussion:
stable 30 FPS on the chosen mid-range device, optional 60 FPS higher preset, a bounded
resident-memory budget and no sustained streaming growth. Do not promise these today.

Record p50/p95/p99 frame time, memory peak/trend, loading times, thermal throttling,
draw calls and gameplay scenario. State resolution/quality/device/OS/duration.
A meaningful soak should include movement through chunks, combat and save/reload rather
than an idle menu. Decide numerical memory/storage budgets from measured dev1/dev4 evidence.

## 8. Signing, release and secrets

Use a persistent signing key backed up outside Git. Separate debug and release identities.
Verify certificate, package ID, versionCode/versionName, native ABI and SHA-256 on the actual
APK/AAB. Never claim device QA for a different artifact hash.
Releases require owner approval and a table of PASS / FAIL / NOT RUN with known omissions.
Do not commit keys, Unity license files, cloud accounts, device videos or raw private logs.
