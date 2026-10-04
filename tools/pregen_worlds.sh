#!/bin/bash
# dev.8 (8.2): generate the ready-made small worlds shipped in the APK
# (native/assets-extra/assets/world/map/*.bin). The seeds/sizes must match PREGEN in
# voxygen/src/singleplayer/phone_worlds.rs (patch 0007). Host build, no device needed.
#   VELOREN_SRC=/work/repos/veloren tools/pregen_worlds.sh
# Also prints the generation time per size (THREADS=8 ≈ a phone's core count; a phone
# core is several times slower than a build-server core).
set -euo pipefail
HERE=$(cd "$(dirname "$0")/.." && pwd)
SRC=${VELOREN_SRC:-/work/repos/veloren}
OUT=$HERE/native/assets-extra/assets/world/map
mkdir -p "$OUT"
[ -f /work/native/env_host.sh ] && source /work/native/env_host.sh
cp "$HERE/native/pregen/my_veloren_pregen.rs" "$SRC/world/examples/my_veloren_pregen.rs"
trap 'rm -f "$SRC/world/examples/my_veloren_pregen.rs"' EXIT
cd "$SRC"
cargo build --profile no_overflow -p veloren-world --example my_veloren_pregen
BIN=${CARGO_TARGET_DIR:-$SRC/target}/no_overflow/examples/my_veloren_pregen
gen() { "$BIN" "$OUT/$1.bin" "$2" "$3" "$4" "$5" "${THREADS:-8}"; }
# seed 1337 7x7 square: 1 town, 2 gnarling forts, 1 sahagin dungeon (4 km)
gen my_veloren_small_7 1337 7 7 square
# seed 1337 8x8 circle: 3 towns, cultist, vampire castle, dwarven mine, gnarling, caves... (8 km)
gen my_veloren_island_8 1337 8 8 circle
ls -l "$OUT"; sha256sum "$OUT"/*.bin
