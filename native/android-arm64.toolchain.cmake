set(ANDROID_ABI arm64-v8a CACHE STRING "" FORCE)
set(ANDROID_PLATFORM android-26 CACHE STRING "" FORCE)
include(/work/native/ndk/build/cmake/android.toolchain.cmake)
