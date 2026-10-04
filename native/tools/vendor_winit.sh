#!/bin/bash
# dev.9: winit 0.30.13 + the my-veloren Android input hook, vendored into the Veloren tree
# (VELOREN_SRC/third_party/winit-0.30.13; patch 0008 points Cargo's [patch.crates-io] there).
# The crate comes from crates.io and is checked against the checksum in Veloren's Cargo.lock.
#   native/tools/vendor_winit.sh /path/to/veloren
set -euo pipefail
HERE=$(cd "$(dirname "$0")/.." && pwd)
SRC=${1:-${VELOREN_SRC:?veloren tree}}
V=0.30.13
SUM=a6755fa58a9f8350bd1e472d4c3fcc25f824ec358933bba33306d0b63df5978d   # Cargo.lock at the upstream pin
DEST="$SRC/third_party/winit-$V"
[ -d "$DEST" ] && { echo "already vendored: $DEST"; exit 0; }
TMP=$(mktemp -d)
curl -sSfL "https://crates.io/api/v1/crates/winit/$V/download" -o "$TMP/winit.crate"
echo "$SUM  $TMP/winit.crate" | sha256sum -c -
mkdir -p "$SRC/third_party"
tar -xzf "$TMP/winit.crate" -C "$SRC/third_party"
(cd "$DEST" && patch -p1 < "$HERE/patches/winit-$V-android-input-hook.patch")
rm -rf "$TMP"
echo "vendored $DEST"
