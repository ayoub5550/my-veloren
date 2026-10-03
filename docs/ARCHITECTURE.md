# Proposed architecture — not implemented

## Verified upstream observations

Reference commit: `585a91b4a76fcf5df7a4851127cf5907a3ce34df`, inspected 2026-10-03.
The workspace separates `common`, `client`, `server`, `server/agent`, `world`, `rtsim`,
`network`, `voxygen` and `voxygen/anim`.

- `voxygen/src/singleplayer/mod.rs` starts a background `Server`, uses local persistence,
  and runs a 30 TPS server loop. The local game still needs simulation responsibilities.
- `singleplayer_world.rs` records world metadata in RON, seed/options and a world map path.
  Copying these files into Unity does not implement their formats or semantics.
- `voxygen/src/scene/figure/load.rs` loads voxel model indices, manifests, offsets and
  mirroring. One source file is not necessarily one complete character.
- `common/src/figure/{mod.rs,cell.rs}` interprets palette indices as surface/hollowing
  semantics as well as RGB. A generic colored-cube loader is insufficient.
- `voxygen/anim` has skeleton families and procedural animation interfaces, not a directory
  of ready-made FBX animation clips.

These are source observations. Proposed designs below have not been compiled or benchmarked.

## Proposed runtime layers

```text
Android application lifecycle
  └── Unity presentation (rendering, audio, touch, UI, cameras)
         ↕ snapshots / commands; no gameplay truth in UI
      OfflineSession
         ├── SimulationCore (C#, fixed-step)
         │     ├── actors, combat, items, crafting, AI, progression
         │     └── world / chunk scheduling / local simulation
         ├── ContentCatalog (stable IDs + converted immutable data)
         └── SaveService (versioned local state + world deltas)
```

### Authority and offline behavior

`SimulationCore` owns health, inventory, AI, rewards, time and persistence-relevant state.
Unity components adapt input and render snapshots. No remote authority, public authentication
service, mandatory socket transport or external content URL belongs on the critical path.
A 30 Hz simulation is a candidate informed by upstream, not yet a chosen mobile budget.
Rendering can run at another rate using interpolation.

An in-process design does **not** imply mechanically merging Rust server/client source files.
Port responsibilities through explicit interfaces and tests. No mock network stub may stand
in for missing AI, world simulation or persistence.

### World

Design chunk streaming, bounded work queues, cancellation, mesh ownership and save deltas
before attempting continent-scale content. Separate:

1. deterministic base world generation or conversion;
2. local modifications and persistent entities;
3. near-player high-frequency simulation;
4. coarse distant simulation if needed for required upstream behavior.

Use integer chunk coordinates and explicit coordinate transforms. Treat seed + generator
version + content pin as a world identity. Matching a seed does not establish parity with
upstream's Rust world algorithm. A simple noise terrain can be a labelled diagnostic only,
not a substitute claimed as the original world.

### Rendering and assets

Candidate: mobile-oriented URP with limited lighting and shaders authored for the selected
pipeline. dev1 must compare real-device results before committing to URP or Built-in.
Do not assume Rust/GLSL/wgpu shaders can be dropped into Unity unchanged.

Use batched chunk meshes rather than one GameObject/collider per voxel. Benchmark greedy
meshing or another bounded mesher, opaque/cutout/transparent passes, LOD and draw-call cost.
Track CPU copies and GPU allocations separately; unload old chunks/assets deterministically.

Keep figure segment assembly, equipment visibility, bone transforms and animation state
separate from file decoding. Establish fixtures for humanoid plus non-humanoid families.

### Data and interface contracts to write in dev1

| Contract | Required fields / invariants |
|---|---|
| Content ID | Upstream-relative canonical ID; no machine paths; collision policy |
| Asset record | Pin, path, source hash, type, provenance, license, dependency IDs, output IDs |
| Figure definition | Segment IDs, model indices, offsets, mirror flags, palette semantics, skeleton |
| World key | Seed, generator version, chunk coordinate, content/schema versions |
| Simulation command | Tick, actor ID, action/parameters; validation independent of UI |
| Save header | Format version, content version, world identity, player state checksum |
| Build receipt | Commit, toolchain, content manifest hash, tasks/results, artifact hash |

Contracts are proposals. JSON in the planning repository is only the research inventory,
not these runtime schemas.

### Persistence

Start with a versioned, bounded format and explicit migrations. Choose JSON/binary/SQLite
after data sizing; do not embed an unreviewed native database dependency just for convenience.
Require atomic write/replace, previous-good backup, corruption detection, interrupted-write
recovery, pause/resume handling and compatibility tests across releases.
Do not promise compatibility with original Veloren saves.

### Input and accessibility

Touch needs independent finger ownership for movement, look, attack and UI, safe-area
layout, sensitivity settings and a pause path. Test aim-while-attacking, cancelled touches,
orientation/lifecycle transitions and UI interception. Controller and gyro are later explicit
features, not inferred from a desktop input map.

Arabic localization requires actual RTL/shaping and glyph validation. Copying FTL files is
not implementation of Fluent syntax or Arabic text shaping.

## Alternative: Rust native simulation

Possible advantage: preserve some original rules/world code. Costs: ARM64 toolchain,
C ABI contracts, memory ownership, threading, packaging/16 KB compatibility, build size,
crash diagnosis and licensing integration. It is **not proven feasible here**.
If full C# parity is impractical, create an ADR comparing measured alternatives and obtain
owner agreement. Do not quietly ship a different engine strategy.
