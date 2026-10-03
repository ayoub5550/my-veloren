export RUSTUP_HOME=/work/native/rustup CARGO_HOME=/work/native/cargo
export NDK=/work/native/ndk ANDROID_NDK_HOME=/work/native/ndk ANDROID_NDK_ROOT=/work/native/ndk
TC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin
export PATH=/work/native/cargo/bin:/work/native/tools-venv/bin:$TC:$PATH
export CC_aarch64_linux_android=$TC/aarch64-linux-android26-clang
export CXX_aarch64_linux_android=$TC/aarch64-linux-android26-clang++
export AR_aarch64_linux_android=$TC/llvm-ar
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$TC/aarch64-linux-android26-clang
export CMAKE_TOOLCHAIN_FILE_aarch64_linux_android=/work/native/android-arm64.toolchain.cmake
export ANDROID_ABI=arm64-v8a ANDROID_PLATFORM=android-26
export CMAKE_GENERATOR=Ninja
export CARGO_BUILD_JOBS=16
