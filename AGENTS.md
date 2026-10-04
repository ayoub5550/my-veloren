# AGENTS.md — developer and AI-agent contract

> **Owner override 2026-10-03 (ADR-002, see [DEV2-NATIVE](docs/DEV2-NATIVE.md)):** the owner rejected the
> Unity dev1 slice and chose the **native port** (upstream Rust Voxygen cross-compiled for Android,
> full pinned asset tree in the APK, official world map). For dev2+ this supersedes the "Unity is the
> Android host" rule in §2 and the planning-only stop in §0 for the dev2 milestone. Start with
> `docs/DEV2-NATIVE.md` (status log, rebuild steps, Android code map).
>
> **2026-10-03:** dev2 merged to `main` by owner (PR #3, prerelease v0.1.0-dev.2). Next milestones
> (dev.3 touch controls → dev.10 distribution) are in [docs/ROADMAP.md](docs/ROADMAP.md); each needs an
> owner instruction «نفّذ dev.N». The old Unity roadmap is `docs/ROADMAP-UNITY-ARCHIVED.md`.
>
> **2026-10-04:** dev.5 + dev.6 merged (PR #6, #7). Owner tested dev.6 on his Poco F3 and his fixes became
> **dev.7** in [docs/ROADMAP.md](docs/ROADMAP.md) (Ultra heat, button redesign/no overlap, smoother joystick,
> touch-consistent menus, touch-only controls settings, crashes). Owner: the pending Firebase physical runs
> (r8q/e3q) are no longer needed; his own phone test is the physical gate. dev.7 starts on «ابدأ dev.7».
>
> **2026-10-03:** dev.6 authorized («اجلها وابدأ dev.6», physical runs postponed): branch `feat/dev6-touch-ui`
> stacked on dev.5, patch 0005 (window gestures, Android text dialog for chat + character name via RegisterNatives,
> DPI UI scale, Arabic shaping/bidi in `ui/rtl.rs`). APKs are signed with one stable key from `ANDROID_KEYSTORE`
> (outside the repo). See [docs/DEV6.md](docs/DEV6.md). Not merged without owner approval.
>
> **2026-10-03:** dev.5 authorized («ابدأ dev5»): branch `feat/dev5-lifecycle`, patch 0004 (lifecycle, autosave,
> Back, assets-once, crash reports, + 5.6 terrain occlusion culling approved by the owner). VM: sc. 4 8/8, sc. 5 6/6.
> Physical runs pending quota. See [docs/DEV5.md](docs/DEV5.md). Not merged without owner approval.
>
> **2026-10-03:** dev.3 merged (PR #4). dev.4 (mobile tiers, pipeline cache, perf telemetry; patch 0003)
> was closed by the owner («اغلق dev4 وابدأ في dev5») with its physical 30 fps gate NOT RUN (Test Lab quota),
> carried to the first physical run of dev.5. See [docs/DEV4.md](docs/DEV4.md).
>
> **2026-10-03:** owner instructed «نفّذ dev.3 كاملا من غير اخطاء وتجرب في firebase» → dev.3 (touch
> controls) authorized: branch `feat/dev3-touch-controls`, patch `native/patches/0002-…`, status and
> evidence in [docs/DEV3.md](docs/DEV3.md). Not merged without owner approval.

## 0. Authorization: dev1 authorized (2026-10-03)

The owner chose **Veloren → Unity → Android → offline**. On 2026-10-03 the owner instructed
(Slack): "check my-veloren, start working on it, build an APK and push to the repo" and confirmed
it is a Unity Android port like the earlier LibreQuake port. This authorizes **dev1 only**
(Unity feasibility slice + APK + prerelease). Later milestones need a new owner instruction.

Current state and evidence: [docs/DEV1.md](docs/DEV1.md). Never report a gate as PASS without
its receipt. Do not merge PRs without owner approval.

## 1. Read order

1. [README](README.md)
2. [ROADMAP](docs/ROADMAP.md)
3. [ARCHITECTURE](docs/ARCHITECTURE.md)
4. [ASSET-PIPELINE](docs/ASSET-PIPELINE.md)
5. [BUILD-AND-QA](docs/BUILD-AND-QA.md)
6. [PARITY-MATRIX](docs/PARITY-MATRIX.md)
7. [RESEARCH-AND-DECISIONS](docs/RESEARCH-AND-DECISIONS.md)

Read the entire relevant milestone document and current status before resuming.
`agent.md` is a pointer, not a second set of rules.

## 2. Non-negotiable product requirements

- Unity is the Android renderer/application host; no substitution with a desktop wrapper.
  If dev1 finds a technical or distribution blocker, stop and present an ADR to the owner.
  Only the owner may approve a changed platform/architecture requirement.
- Gameplay, world simulation and persistence run locally. No login, remote game server,
  mandatory telemetry, first-launch asset download or online license check for the player.
- Preserve the complete pinned upstream asset inventory. Small fixtures are an incremental
  engineering step, never permission to silently reduce the final asset-import goal.
- Importing a `.vox` file is not implementing its creature, equipment, animation or AI.
- A playable slice is not the full game. Keep fidelity and omissions visible in the matrix.
- Never copy private predecessor code/assets, credentials or logs into this public repository.
  Reuse generalized lessons; any future code reuse needs a separate provenance decision.

## 3. Work model once a milestone is authorized

1. Read current Git status, branch, upstream pin, milestone and outstanding review notes.
2. Work in an isolated feature worktree under `.worktrees/`; keep one branch per milestone.
3. Split ownership into non-overlapping areas:
   - **Content**: inventory, provenance, converters and deterministic import.
   - **Simulation**: pure C# rules, state and persistence.
   - **Presentation**: Unity rendering, characters, camera and touch.
   - **Integration/QA**: build, regression, Android packaging and release evidence.
4. Share stable interface/schema contracts before parallel edits. One integrator owns
   cross-cutting project settings, scene generation and package changes.
5. Run cheap tests first, then real Unity compilation/import, then Android/device gates.
6. Update the milestone, parity matrix and evidence record with PASS / FAIL / NOT RUN.
7. Push a review branch and report blockers. Do not merge automatically.

For Viktor environments, all Git/GitHub operations must use the authenticated platform SDK,
not raw shell git/gh. Other agents must use their approved authenticated tools and identity.
Serialize Git mutations on a shared checkout. Do not reset another worker's changes.

## 4. Evidence and claims

Every build/test receipt must identify commit, dirty-tree state, upstream asset pin, toolchain,
command, exit code, test totals, artifact SHA-256 and evidence locations.
Secrets must never appear in receipts. A checksum verifies bytes, not licensing or quality.

Keep these claims distinct:

| Claim | Required evidence |
|---|---|
| Inventory researched | Complete non-truncated tree, source commit, path/type counts |
| Assets acquired | Payload hashes and sizes, no unresolved LFS pointers |
| Assets converted | Converter report and full dependency coverage |
| Unity imported | Editor import/compile logs, GUID/material/reference checks |
| APK built | Actual artifact, manifest, signature and checksum checks |
| Offline playable | Fresh install + airplane-mode play/save/reload/device evidence |
| Full parity | Every required matrix row passes its content and behavior gates |

Do not replace an unverified behavior with a placeholder and mark it complete.
Never swallow importer exceptions, silently drop RON fields or label missing content optional.

## 5. Sandbox portability

High CPU counts do not imply Unity, its license, Android modules, GPU or KVM are available.
At the start of an authorized build milestone, inventory actual CPU/RAM/disk/tool versions
and graphics access. See the runbook for the proposed toolchain decision and test ladder.

Use environment variables for local tool paths; do not hardcode another agent's machine.
Use deterministic content caches with checksums; avoid repeated multi-GB regeneration.
Do not install compatibility shims unless the exact failure has been reproduced.

## 6. Handoff format

Each future `docs/DEV<n>.md` must contain:

- owner instruction authorizing the stage; scope and explicitly excluded work;
- branch/commit, upstream pin, chosen toolchain, interfaces changed;
- reproducible commands that actually exist at that revision;
- evidence table with pass/fail/not-run and what each test proves;
- imported/raw/converted/playable counts separately;
- known defects, risks, rollback and next task;
- no copied credentials, private logs, APKs or generated assets in the document.

Write code and technical runbooks in English; keep the owner-facing overview and milestone
summary understandable in Arabic. Keep this guide concise; move history into milestone docs.
