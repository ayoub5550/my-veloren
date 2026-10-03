#!/bin/bash
# Host unit tests of the touch layer (no Android device, no GPU): layout overlap for
# 16:9..22:9, analog stick, multi-touch, camera smoothing, pinch, context buttons,
# window/mouse mode, layout editor + RON round trip.
#   VELOREN_SRC=/work/native/veloren native/touch-tests/run.sh
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
SRC=${VELOREN_SRC:-/work/native/veloren}
ln -sf "$SRC/voxygen/src/touch.rs" "$HERE/src/touch.rs"
cd "$HERE" && CARGO_TARGET_DIR=${CARGO_TARGET_DIR:-/tmp/touch-tests-target} cargo test "$@"
