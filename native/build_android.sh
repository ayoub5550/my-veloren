#!/bin/bash
# Build the native Veloren Android APK (dev2, ADR-002).
# Usage: native/build_android.sh [VELOREN_SRC=/work/native/veloren] ; output: native/out/my-veloren-dev2.apk
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
SRC=${VELOREN_SRC:-/work/native/veloren}
SDK=${ANDROID_SDK:-/work/unity/android-sdk}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
source /work/native/env.sh
export JAVA_HOME=${JAVA_HOME:-/work/unity/editor/Editor/Data/PlaybackEngines/AndroidPlayer/OpenJDK}
export PATH=$JAVA_HOME/bin:$PATH
OUT=$HERE/out; STAGE=$OUT/stage; rm -rf "$STAGE"; mkdir -p "$STAGE/lib/arm64-v8a" "$STAGE/assets"
if [ "${SKIP_CARGO:-0}" != 1 ]; then
  (cd "$SRC" && cargo build --profile release-thinlto --target aarch64-linux-android -p veloren-voxygen --lib \
     --no-default-features --features singleplayer,simd,shaderc-from-source)
fi
SO=$SRC/target/aarch64-linux-android/release-thinlto/libveloren_voxygen.so
llvm-strip --strip-unneeded -o "$STAGE/lib/arm64-v8a/libveloren_voxygen.so" "$SO"
cp "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$STAGE/lib/arm64-v8a/"
# Full upstream asset tree, as one tar (extracted on first launch by voxygen/src/android.rs)
tar -C "$SRC" --exclude='*.blend' -cf "$STAGE/assets/assets.tar" assets
"$BT/aapt2" link -o "$OUT/base.apk" --manifest "$HERE/android/AndroidManifest.xml" -I "$JAR" -A "$STAGE/assets" -0 tar
python3 -c "import zipfile,os,sys;z=zipfile.ZipFile(sys.argv[1],'a',zipfile.ZIP_DEFLATED,compresslevel=6);[z.write(os.path.join(r,f),os.path.relpath(os.path.join(r,f),sys.argv[2])) for r,_,fs in os.walk(os.path.join(sys.argv[2],'lib')) for f in fs];z.close()" "$OUT/base.apk" "$STAGE"
"$BT/zipalign" -f -p 16 "$OUT/base.apk" "$OUT/aligned.apk"
KS=$OUT/debug.keystore
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
   -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$OUT/my-veloren-dev2.apk" "$OUT/aligned.apk"
rm -f "$OUT/base.apk" "$OUT/aligned.apk"
ls -l "$OUT/my-veloren-dev2.apk"; sha256sum "$OUT/my-veloren-dev2.apk"
