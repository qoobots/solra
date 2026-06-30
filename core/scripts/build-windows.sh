#!/usr/bin/env bash
# ============================================================================
# Solra Core SDK - Windows x64 Build Script (MSVC)
# ============================================================================
# Prerequisites:
#   - Visual Studio 2022 with C++ Desktop workload
#   - cmake >= 3.24
#   - vcpkg (set VCPKG_ROOT or install to %USERPROFILE%/vcpkg)
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="${CORE_DIR}/build/windows"
VCPKG_ROOT="${VCPKG_ROOT:-$HOME/vcpkg}"
BUILD_TYPE="${BUILD_TYPE:-Release}"

echo "=== Solra Core SDK - Windows x64 Build ==="
echo "Core dir:   ${CORE_DIR}"
echo "Build dir:  ${BUILD_DIR}"
echo "Build type: ${BUILD_TYPE}"

# Configure
echo "Configuring CMake (Windows x64 MSVC)..."
cmake -B "${BUILD_DIR}" \
  -G "Visual Studio 17 2022" -A x64 \
  -DCMAKE_TOOLCHAIN_FILE="${VCPKG_ROOT}/scripts/buildsystems/vcpkg.cmake" \
  -DVCPKG_TARGET_TRIPLET=x64-windows \
  -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
  -DSOLRA_BUILD_RENDER=ON \
  -DSOLRA_BUILD_INFERENCE=ON \
  -DSOLRA_BUILD_STREAMING=ON \
  -DSOLRA_BUILD_TESTS=OFF

# Build
echo "Building for Windows x64 (${BUILD_TYPE})..."
cmake --build "${BUILD_DIR}" \
  --config "${BUILD_TYPE}"

echo ""
echo "=== Windows x64 Build Complete ==="
echo "Artifacts: ${BUILD_DIR}/${BUILD_TYPE}/"
