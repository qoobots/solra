#!/usr/bin/env bash
# ============================================================================
# Solra Core SDK - Android arm64-v8a Build Script
# ============================================================================
# Prerequisites:
#   - Android NDK r26+ (set ANDROID_NDK_HOME)
#   - cmake >= 3.24
#   - ninja
#   - JDK 17
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="${CORE_DIR}/build/android"
BUILD_TYPE="${BUILD_TYPE:-Release}"
ANDROID_ABI="${ANDROID_ABI:-arm64-v8a}"
ANDROID_API="${ANDROID_API:-26}"

echo "=== Solra Core SDK - Android ${ANDROID_ABI} Build ==="
echo "Core dir:    ${CORE_DIR}"
echo "Build dir:   ${BUILD_DIR}"
echo "Build type:  ${BUILD_TYPE}"
echo "NDK home:    ${ANDROID_NDK_HOME:-NOT SET}"

if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  echo "ERROR: ANDROID_NDK_HOME is not set."
  echo "  export ANDROID_NDK_HOME=/path/to/android-ndk-r26"
  exit 1
fi

# Configure
echo "Configuring CMake (Android ${ANDROID_ABI})..."
cmake -B "${BUILD_DIR}" \
  -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="${ANDROID_ABI}" \
  -DANDROID_PLATFORM="android-${ANDROID_API}" \
  -DANDROID_STL=c++_shared \
  -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
  -DSOLRA_BUILD_RENDER=ON \
  -DSOLRA_BUILD_INFERENCE=ON \
  -DSOLRA_BUILD_STREAMING=ON \
  -DSOLRA_BUILD_TESTS=OFF

# Build
echo "Building for Android ${ANDROID_ABI} (${BUILD_TYPE})..."
cmake --build "${BUILD_DIR}" \
  --config "${BUILD_TYPE}" \
  -j$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)

echo ""
echo "=== Android ${ANDROID_ABI} Build Complete ==="
echo "Artifacts: ${BUILD_DIR}/lib/"
