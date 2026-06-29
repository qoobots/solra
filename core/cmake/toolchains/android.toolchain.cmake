# Solra Android arm64-v8a Toolchain
# Requires: ANDROID_NDK environment variable or -DANDROID_NDK=...
# Usage: cmake -B build/android -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/android.toolchain.cmake -DANDROID_ABI=arm64-v8a

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 26)
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
set(CMAKE_ANDROID_NDK "$ENV{ANDROID_NDK}")
set(CMAKE_ANDROID_STL_TYPE c++_shared)
set(CMAKE_ANDROID_NDK_TOOLCHAIN_VERSION clang)

set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_definitions(-DSOLRA_PLATFORM_ANDROID=1 -DSOLRA_GPU_VULKAN=1)
add_compile_options(-fPIC -fno-exceptions -fno-rtti)

# Use Neon SIMD intrinsics
add_compile_options(-mfpu=neon)

# Strip debug symbols in release
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s -flto=thin")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s -flto=thin")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -Wl,--gc-sections -Wl,--strip-all")
