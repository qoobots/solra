# Solra Core SDK - CMake toolchain for Android NDK cross-compilation
# Usage: cmake -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/android.cmake -DANDROID_ABI=arm64-v8a ...

# Use Android NDK's built-in toolchain file as base
if(NOT DEFINED ANDROID_NDK)
  set(ANDROID_NDK "$ENV{ANDROID_NDK_HOME}" CACHE PATH "Path to Android NDK")
endif()

if(NOT EXISTS "${ANDROID_NDK}/build/cmake/android.toolchain.cmake")
  message(FATAL_ERROR "Android NDK not found at ${ANDROID_NDK}. Set ANDROID_NDK or ANDROID_NDK_HOME environment variable.")
endif()

# Base Android NDK toolchain settings
set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 21 CACHE STRING "Android API level")
set(CMAKE_ANDROID_ARCH_ABI "arm64-v8a" CACHE STRING "Target ABI")
set(CMAKE_ANDROID_NDK "${ANDROID_NDK}")
set(CMAKE_ANDROID_STL_TYPE "c++_shared" CACHE STRING "C++ STL type")

# Android-specific compiler flags
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Vcpkg triplet
set(VCPKG_TARGET_TRIPLET "arm64-android" CACHE STRING "")

# Solra platform defines
add_compile_definitions(SOLRA_PLATFORM_ANDROID=1)
add_compile_definitions(SOLRA_ENABLE_VULKAN=1)
add_compile_definitions(SOLRA_ENABLE_OPENGLES=1)
add_compile_definitions(SOLRA_ENABLE_NNAPI=1)
