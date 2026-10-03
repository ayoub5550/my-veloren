# Full-asset import plan — no assets imported

## 1. What the inventory actually establishes

The GitHub mirror tree at commit `585a91b4a76fcf5df7a4851127cf5907a3ce34df`
was queried recursively on 2026-10-03. `truncated=false`.
There are **10,654 blob entries under `assets/`**.
See [machine-readable inventory](upstream-inventory.json).

| Extension | Count | Planned handling |
|---|---:|---|
| `.vox` | 4,752 | Parse models/palettes; apply upstream segment/material semantics; generate meshes |
| `.ron` | 2,855 | Parse typed records/manifests into versioned data; resolve references |
| `.ftl` | 1,342 | Localization: preserve Fluent semantics, fallback, pluralization and RTL tests |
| `.png` | 898 | Textures/UI; explicit color/data/alpha import policy |
| `.ogg` | 639 | Audio classification, decoded-sample checks, streaming policy |
| `.glsl` | 67 | Reference shaders; port required behavior, not a direct Unity import |
| `.jpg` | 42 | Texture/UI conversion with explicit alpha handling where applicable |
| `.obj` | 26 | Mesh scale/axis/material inspection |
| `.ttf` | 13 | Preserve font notices; build and validate glyph coverage |
| `.txt` | 9 | Notices and text; retain attribution |
| `.bin` | 3 | Identify world/binary schemas before conversion; never guess |
| `.md` | 2 | Upstream documentation |
| Other | 6 | One each: `.canary`, `.desktop`, `.frag`, `.ico`, `.vert`, `.xml` |

Tree-reported blob sizes sum to **435,429,176 bytes**. This is not a downloaded archive size,
not a verified payload footprint, not Unity output size, and not an APK estimate.
No textures, sounds or voxel binaries were imported for this plan.

### Git LFS caution

Upstream `.gitattributes` contains LFS patterns **and a final wildcard resetting attributes**.
Sample tree entries have binary-sized blobs, not uniformly tiny pointers.
Therefore do not assert that every asset is currently LFS-backed, or that an archive always
contains real payloads. At acquisition, inspect actual bytes for the LFS pointer signature,
resolve only genuine pointers, and validate the pinned object hashes and payload lengths.
Changing historical asset-storage strategy is not permission to change the snapshot silently.

## 2. Full inventory versus shipped game

All upstream asset files must remain accountable in the source catalog:

- **Acquired**: exact raw payload preserved with provenance.
- **Interpreted/converted**: type-specific output and dependency references verified.
- **Runtime supported**: corresponding figure/material/data semantics implemented.
- **Playable**: feature and device tests demonstrate the intended behavior.

Do not use a single "imported" percentage for these four states.
Every record needs status and reason. Unused source documentation need not be packaged into
the player, but source notices must be preserved and distribution notices included.
Any excluded gameplay content requires an explicit owner decision; performance tuning must
not silently redefine "all assets" to a handful of demo models.

## 3. Acquisition design for an authorized future milestone

1. Pin repository/commit and record a deterministic source manifest.
2. Acquire the exact raw asset corpus into a cache outside the Unity import directory.
3. Preserve paths and original bytes; calculate SHA-256 per payload and aggregate manifests.
4. Reject path traversal, symlinks escaping the root, duplicate/case-colliding paths,
   decompression bombs, missing dependencies, pointer files and unexpected executables.
5. Preserve upstream `LICENSE`, `assets/credits.ron` and per-font/other notices.
6. Derive a type/dependency catalog; fail unknown required types instead of ignoring them.
7. Keep raw inputs separate from deterministic Unity outputs and runtime content packs.
8. Test a clean extraction and a second identical conversion, not only a warm working tree.

Future storage options: Git LFS for suitable source assets or a pinned release/archive with
automatic verified acquisition. Ordinary Git is not a place for generated meshes, Unity
Library caches or APKs. Select a documented strategy before importing the corpus.

## 4. Critical format work

### VOX is more than a mesh

Read upstream figure loading and cell semantics before designing the importer.
The source uses model indices, mirroring, offsets, custom palette indices and humanoid
recoloring. Some palette indices mean shiny/glowing/hollow/override, not just a color.
Test against the upstream library's indexing convention; raw VOX palette indexing may differ
from the parser's exposed indices. Do not hardcode an untested off-by-one interpretation.

Golden cases: empty model, multiple models, mirrored limb, offset assembly, colored armor,
hollowing, emissive cells, large/sparse dimensions and malformed/truncated chunks.
Bound allocation sizes and integer arithmetic. Preserve explicit unsupported-chunk reports.

### RON is not JSON

Use a parser that supports the required grammar, enums, tuples, options, maps and comments.
A regex rewrite is not an acceptable general converter.
Possible approach: a small pinned Rust conversion tool at development time exporting a
stable schema; this would not require a Rust runtime in the Android player.
Retain enum variants and report every unmapped required field. Validate referenced IDs.

### Procedural animation

`.vox` carries voxel geometry, not the full motion system. Port or accurately reproduce the
required `voxygen/anim` skeleton/state logic. Validate rest pose, equipped attachments,
idle/walk/run/jump/attack/hit/death before expanding body families.

### Textures, sound and shaders

- Classify sRGB/color, normal/direction/data, alpha/cutout and UI separately.
- Do not compress numeric textures with a lossy format merely because color textures use it.
- Preserve alpha explicitly when combining RGB images with another mask.
- Audio import must prove nonzero decoded samples and correct channels/duration.
- Shader inclusion and keyword combinations need actual Android player validation.
- Convert shader behavior to the selected Unity pipeline; retain source and attribution.

## 5. Provenance gate

`assets/credits.ron` documents per-asset attribution, optional license/link fields and a
GPL3 default when its license field is omitted. This is a starting point, not proof that a
blanket label fits every file. Keep font licenses and all explicit exceptions.
Record modifications to derived files and trace outputs back to raw inputs.
Resolve unknown terms and engine-integration/distribution obligations before copying code
or releasing a product. This plan does not claim a finished licensing audit.

## 6. dev2 acceptance report

Required columns:
`source_path | type | source_sha256 | provenance | raw_status | converter |
output_ids | dependency_status | runtime_status | test_evidence | blocker`.

Required totals: source count by extension; acquired/missing/pointer counts; conversion
pass/fail/unsupported counts; dependency errors; source/derived size; deterministic rerun hash.
For this planning revision, **all acquisition/conversion/runtime totals are NOT RUN**.
