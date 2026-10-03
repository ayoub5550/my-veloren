#!/usr/bin/env python3
"""dev1: fetch the pinned Veloren asset sample into the Unity project (Resources/Vox/*.bytes).

Usage (repo root):  python3 tools/fetch_assets.py
Writes game/Assets/Veloren/Resources/Vox/<veloren path under assets/voxygen/voxel or assets/>.bytes
and assets-manifest.json (path, bytes, sha256). Fails on an LFS pointer or a non-VOX payload.
SPDX-License-Identifier: GPL-3.0-or-later
"""
import hashlib, json, sys, urllib.request
from pathlib import Path

COMMIT = "585a91b4a76fcf5df7a4851127cf5907a3ce34df"  # pinned upstream (docs/upstream-inventory.json)
BASE = f"https://raw.githubusercontent.com/veloren/veloren/{COMMIT}/"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "game/Assets/Veloren/Resources/Vox"

V = "assets/voxygen/voxel/"
SAMPLE = [
    V + "figure/head/human/male.vox", V + "figure/eyes/general/male_default-0.vox",
    V + "figure/hair/human/male-1.vox", V + "figure/body/hand.vox",
    V + "armor/misc/chest/grayscale.vox", V + "armor/misc/pants/grayscale.vox",
    V + "armor/misc/belt/dark.vox", V + "armor/misc/foot/dark.vox",
    V + "weapon/sword/starter.vox",
] + [f"assets/world/tree/pine_green/{i}.vox" for i in range(1, 9)] \
  + [f"assets/world/tree/temperate_small/{i}.vox" for i in range(1, 7)] \
  + [f"assets/world/tree/oak_stump/{i}.vox" for i in range(1, 4)]


def key(path: str) -> str:
    rel = path[len(V):] if path.startswith(V) else path[len("assets/"):]
    return rel[: -len(".vox")]


def main() -> int:
    manifest = []
    for p in SAMPLE:
        data = urllib.request.urlopen(BASE + p, timeout=60).read()
        if not data.startswith(b"VOX "):
            print("NOT A VOX PAYLOAD (LFS pointer?):", p, data[:60]); return 1
        dst = OUT / (key(p) + ".bytes")
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(data)
        manifest.append({"path": p, "key": key(p), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        print(f"{len(data):8d}  {p}")
    (ROOT / "assets-manifest.json").write_text(json.dumps(
        {"upstream": "https://github.com/veloren/veloren", "commit": COMMIT, "license": "GPL-3.0 (upstream)",
         "count": len(manifest), "files": manifest}, indent=1) + "\n")
    print("fetched", len(manifest), "files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
