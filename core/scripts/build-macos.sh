#!/usr/bin/env bash
# ============================================================================
# Solra Core SDK - macOS arm64 Build Script
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
BUILD_DIR="${CORE_DIR}/build/macos"
VCPKG_ROOT="${VCPKG_ROOT:-$HOME/vcpkg}"
BUILD_TYPE="${BUILD_TYPE:-Release}"

echo "=== Solra Core SDK - macOS arm64 Build ==="
echo "Core dir:   ${CORE_DIR}"
echo "Build dir:  ${BUILD_DIR}"
echo "Build type: ${BUILD_TYPE}"

# Setup vcpkg if needed
if [ ! -f "${VCPKG_ROOT}/vcpkg" ]; then
  echo "Bootstrapping vcpkg..."
  git clone https://github.com/microsoft/vcpkg.git "${VCPKG_ROOT}"
  "${VCPKG_ROOT}/bootstrap-vcpkg.sh"
fi

# Install dependencies
echo "Installing vcpkg dependencies (arm64-osx)..."
cd "${CORE_DIR}"
"${VCPKG_ROOT}/vcpkg" install --triplet arm64-osx --vcpkg-root "${VCPKG_ROOT}"

# Configure
echo "Configuring CMake (macOS arm64)..."
cmake -B "${BUILD_DIR}" \
  -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="${VCPKG_ROOT}/scripts/buildsystems/vcpkg.cmake" \
  -DVCPKG_TARGET_TRIPLET=arm64-osx \
  -DCMAKE_OSX_ARCHITECTURES=arm64 \
  -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
  -DSOLRA_BUILD_RENDER=ON \
  -DSOLRA_BUILD_INFERENCE=ON \
  -DSOLRA_BUILD_STREAMING=ON \
  -DSOLRA_BUILD_TESTS=OFF

# Build
echo "Building for macOS arm64 (${BUILD_TYPE})..."
cmake --build "${BUILD_DIR}" \
  --config "${BUILD_TYPE}" \
  -j$(sysctl -n hw.ncpu)

echo ""
echo "=== macOS arm64 Build Complete ==="
echo "Artifacts: ${BUILD_DIR}/lib/"
