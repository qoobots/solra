# Solra custom vcpkg triplet for Android arm64-v8a
# Place in: ${VCPKG_ROOT}/triplets/community/ or use --overlay-triplets
set(VCPKG_TARGET_ARCHITECTURE arm64)
set(VCPKG_CRT_LINKAGE static)
set(VCPKG_LIBRARY_LINKAGE static)
set(VCPKG_CMAKE_SYSTEM_NAME Android)
set(VCPKG_CMAKE_SYSTEM_VERSION 26)
set(VCPKG_ANDROID_NDK_VERSION 26)
set(VCPKG_BUILD_TYPE release)
