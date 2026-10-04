#!/bin/bash
# Build the native Veloren Android APK (dev2+, ADR-002).
# Usage: native/build_android.sh [VELOREN_SRC=/work/native/veloren] [APK_NAME=my-veloren-dev10.apk]
# Output: native/out/$APK_NAME. Patches 0001..0009 must already be applied to VELOREN_SRC, and
# native/tools/vendor_winit.sh must have created VELOREN_SRC/third_party/winit-0.30.13 (dev.9).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
SRC=${VELOREN_SRC:-/work/native/veloren}
source /work/native/env.sh
SDK=${ANDROID_SDK:-/work/native/android-sdk}
APK_NAME=${APK_NAME:-my-veloren-dev10.apk}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
export JAVA_HOME=${JAVA_HOME:-/work/native/jdk}
export PATH=$JAVA_HOME/bin:$PATH
OUT=$HERE/out; STAGE=$OUT/stage; rm -rf "$STAGE"; mkdir -p "$STAGE/lib/arm64-v8a" "$STAGE/assets"
# dev.9: winit with the Android input hook (gamepad / joystick / mouse), see tools/vendor_winit.sh
[ -d "$SRC/third_party/winit-0.30.13" ] || "$HERE/tools/vendor_winit.sh" "$SRC"
if [ "${SKIP_CARGO:-0}" != 1 ]; then
  (cd "$SRC" && cargo build --profile release-thinlto --target aarch64-linux-android -p veloren-voxygen --lib \
     --no-default-features --features singleplayer,simd,shaderc-from-source)
fi
SO=$SRC/target/aarch64-linux-android/release-thinlto/libveloren_voxygen.so
llvm-strip --strip-unneeded -o "$STAGE/lib/arm64-v8a/libveloren_voxygen.so" "$SO"
cp "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$STAGE/lib/arm64-v8a/"
# Full upstream asset tree, as one tar (extracted on first launch by voxygen/src/android.rs).
# dev.10: the heaviest music files (>150 kb/s or >48 kHz) are re-encoded to Vorbis q4 first
# (tools/slim_audio.sh); every file and path stays, only those bytes are smaller.
"$HERE/tools/slim_audio.sh" "$SRC" "$OUT/audio-slim" | tail -1
tar -C "$SRC" --exclude='*.blend' --exclude-from="$OUT/audio-slim/replaced.txt" -cf "$STAGE/assets/assets.tar" assets
tar -C "$OUT/audio-slim" -rf "$STAGE/assets/assets.tar" $(cat "$OUT/audio-slim/replaced.txt")
# dev.8 (8.2): ready-made small worlds generated on the build machine (tools/pregen_worlds.sh)
if [ -d "$HERE/assets-extra/assets" ]; then tar -C "$HERE/assets-extra" -rf "$STAGE/assets/assets.tar" assets; fi
# dev.5: content version of the tarball; the app re-extracts only when it changes
sha256sum "$STAGE/assets/assets.tar" | cut -c1-16 > "$STAGE/assets/assets.version"
# Java shim (VelorenActivity extends NativeActivity) -> classes.dex
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes" "$OUT/dex"
javac -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$JAR" -d "$OUT/classes" $(find "$HERE/android/java" -name '*.java') 2>/dev/null
"$BT/d8" --min-api 26 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/dex/classes.dex" "$STAGE/classes.dex"
# dev.7 (7.9): launcher icon resources (legacy + adaptive), see android/res_src/make_icon.py
rm -rf "$OUT/res.zip"; "$BT/aapt2" compile --dir "$HERE/android/res" -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" --manifest "$HERE/android/AndroidManifest.xml" -I "$JAR" -A "$STAGE/assets" -0 tar -R "$OUT/res.zip" --auto-add-overlay
python3 -c "import zipfile,os,sys;z=zipfile.ZipFile(sys.argv[1],'a',zipfile.ZIP_DEFLATED,compresslevel=6);[z.write(os.path.join(r,f),os.path.relpath(os.path.join(r,f),sys.argv[2])) for r,_,fs in os.walk(os.path.join(sys.argv[2],'lib')) for f in fs];z.write(os.path.join(sys.argv[2],'classes.dex'),'classes.dex');z.close()" "$OUT/base.apk" "$STAGE"
"$BT/zipalign" -f -p 16 "$OUT/base.apk" "$OUT/aligned.apk"
# dev.6: one signing key for every build (ANDROID_KEYSTORE, kept outside the repo), so a new
# APK installs over the old one and keeps the saves. Without it, a per-checkout debug key is made.
KS=${ANDROID_KEYSTORE:-$OUT/debug.keystore}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
   -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$OUT/$APK_NAME" "$OUT/aligned.apk"
rm -f "$OUT/base.apk" "$OUT/aligned.apk"
ls -l "$OUT/$APK_NAME"; sha256sum "$OUT/$APK_NAME"
