# Research basis and decision register

## Scope record

2026-10-03: owner selected Veloren, Unity, offline Android, full asset import and agent docs.
The immediate follow-up explicitly limited current work to **drawing the development
roadmap, without beginning implementation/building**. This revision respects that limit.
The target repository was empty and public at inspection.

Only public upstream metadata/text were used for the source study. No upstream game asset
corpus was downloaded or imported. No Unity toolchain was installed or invoked.

## Pinned public sources

GitHub mirror snapshot: **585a91b4a76fcf5df7a4851127cf5907a3ce34df**.
This is an inspected development snapshot, not a promise to use the latest master forever.
Before implementation, confirm the canonical upstream/mirror relationship and select an
approved stable/reproducible baseline.

- [Workspace crates](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/Cargo.toml)
- [Singleplayer local server](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/voxygen/src/singleplayer/mod.rs)
- [World metadata/persistence](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/voxygen/src/singleplayer/singleplayer_world.rs)
- [Figure assembly](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/voxygen/src/scene/figure/load.rs)
- [Voxel conversion](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/common/src/figure/mod.rs)
- [Cell/palette semantics](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/common/src/figure/cell.rs)
- [Animation families/interfaces](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/voxygen/anim/src/lib.rs)
- [World module](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/world/src/lib.rs)
- [Voxel organization](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/assets/voxygen/voxel/README.md)
- [Credits and attribution](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/assets/credits.ron)
- [Root license](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/LICENSE)
- [Asset attributes](https://github.com/veloren/veloren/blob/585a91b4a76fcf5df7a4851127cf5907a3ce34df/.gitattributes)

Tree inventory was obtained from GitHub's recursive Git tree API, non-truncated, filtered
to blobs beneath `assets/`. Counts are path inventory only. No per-payload verification
or complete source/asset licensing audit was performed.

## General lessons from predecessor projects

The owner asked to benefit from prior LibreQuake, Xonotic and REKKR development experience.
The following are generalized engineering lessons, not copied private implementations,
private source links or promises that their historical results transfer to this game.

| Lesson | Application here |
|---|---|
| Pin editor and content versions | Record exact source/Editor/package/toolchain pins |
| Source-only tests can miss player failures | Separate parser, Editor, rendering, Android and device gates |
| Runtime shaders can disappear in builds | Explicit dependencies/variants + actual player visual cases |
| Models can import white or mirrored without exceptions | Golden axis/palette/material/attachment fixtures |
| Import order matters | Deterministic preparation and scene restoration before build |
| Scene lifecycle order creates hidden state bugs | Explicit initialization and scene reload tests |
| Sound import can silently fail in constrained hosts | Verify decoded samples and actual playback |
| Regenerated content can overwhelm repos/caches | Separate raw source, cache, derived output and shipped pack |
| Lost signing keys break upgrade paths | Persistent protected signing identity and artifact inspection |
| Automated test runs have limits | Report not-run checks; physical-device and manual evidence separately |

All three predecessor project version files specified Unity 2022.3.62f3 at inspection.
No private credentials, source assets, certificates, account IDs or deployment configuration
are reproduced. Historical timings, quotas and FPS are deliberately not used as estimates.

## Decision register

| ID | Topic | Current state |
|---|---|---|
| D01 | Immediate authorization | Documentation only; implementation paused |
| D02 | Product platform | Unity application on Android, offline gameplay |
| D03 | Simulation approach | C# core proposed; Rust FFI alternative needs evidence/approval |
| D04 | Unity version | Prior-project version is a candidate; dev1 must validate final pin |
| D05 | Rendering | Simplified URP candidate; prove on Android before locking |
| D06 | Full asset scope | Complete pinned corpus; samples only for intermediate proof |
| D07 | Raw asset distribution/storage | Decide LFS vs verified source archive/release in dev1 |
| D08 | World fidelity | No equivalence claim; compare original algorithms and declare changes |
| D09 | Device/performance budgets | Device set and budgets unapproved; measure in dev1/dev4 |
| D10 | Saves | New versioned local format proposed; original-save migration separate |
| D11 | Android release/store | No build or submission authorized now |
| D12 | Licenses | dev1 gate before derived-code reuse/linking: document GPLv3/Unity runtime distribution feasibility and asset exceptions; seek appropriate review and owner decision if blocked |

## Main risks

1. **Scope**: importing a content library is much smaller than reproducing the original game.
2. **Semantics**: palette/material/body/animation data require Veloren-specific interpretation.
3. **World cost**: simulation, generation and streaming can dominate mobile CPU/RAM.
4. **Toolchain**: abundant host resources do not prove Editor/license/GPU/NDK availability.
5. **Fidelity**: a substitute terrain generator or generic enemy is not equivalent to upstream.
6. **Packaging**: asset payload, derived meshes and audio can grow independently; measure each.
7. **Offline correctness**: login removal alone does not replace server-owned game logic.
8. **Distribution**: GPL-derived code, media exceptions and Unity/runtime distribution obligations
   must be resolved for the actual implementation; the planning revision is not legal clearance.
