#!/usr/bin/env bash
# ============================================================================
# Solra Core SDK - iOS arm64 Build Script
# ============================================================================
# Prerequisites:
#   - macOS with Xcode 15.4+
#   - cmake >= 3.24
#   - ninja
#   - vcpkg (set VCPKG_ROOT or install to ~/vcpkg)
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="${CORE_DIR}/build/ios"
VCPKG_ROOT="${VCPKG_ROOT:-$HOME/vcpkg}"
BUILD_TYPE="${BUILD_TYPE:-Release}"

echo "=== Solra Core SDK - iOS arm64 Build ==="
echo "Core dir:   ${CORE_DIR}"
echo "Build dir:  ${BUILD_DIR}"
echo "Build type: ${BUILD_TYPE}"
echo "vcpkg root: ${VCPKG_ROOT}"

# Setup vcpkg if needed
if [ ! -f "${VCPKG_ROOT}/vcpkg" ]; then
  echo "Bootstrapping vcpkg..."
  git clone https://github.com/microsoft/vcpkg.git "${VCPKG_ROOT}"
  "${VCPKG_ROOT}/bootstrap-vcpkg.sh"
fi

# Install dependencies
echo "Installing vcpkg dependencies (arm64-ios)..."
cd "${CORE_DIR}"
"${VCPKG_ROOT}/vcpkg" install --triplet arm64-ios --vcpkg-root "${VCPKG_ROOT}"

# Configure
echo "Configuring CMake (iOS arm64)..."
cmake -B "${BUILD_DIR}" \
  -G Xcode \
  -DCMAKE_TOOLCHAIN_FILE="${CORE_DIR}/cmake/toolchains/ios.toolchain.cmake" \
  -DCMAKE_SYSTEM_NAME=iOS \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_OSX_DEPLOYMENT_TARGET=16.0 \
  -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
  -DSOLRA_BUILD_RENDER=ON \
  -DSOLRA_BUILD_INFERENCE=ON \
  -DSOLRA_BUILD_STREAMING=ON \
  -DSOLRA_BUILD_TESTS=OFF

# Build
echo "Building for iOS arm64 (${BUILD_TYPE})..."
cmake --build "${BUILD_DIR}" \
  --config "${BUILD_TYPE}" \
  -- -sdk iphoneos -arch arm64 \
  -j$(sysctl -n hw.ncpu)

echo ""
echo "=== iOS arm64 Build Complete ==="
echo "Artifacts: ${BUILD_DIR}/${BUILD_TYPE}-iphoneos/"
