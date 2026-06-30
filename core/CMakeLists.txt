cmake_minimum_required(VERSION 3.24)
project(solra_core
  VERSION 0.1.0
  DESCRIPTION "Solra Core SDK - Cross-platform 3D rendering, on-device inference, streaming engine"
  LANGUAGES C CXX
)

# ============================================================
# Policies (compatibility with CMake >= 3.28)
# ============================================================
cmake_policy(SET CMP0169 OLD)  # Allow FetchContent_Populate

# ============================================================
# C++17 Standard
# ============================================================
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# ============================================================
# Project Options
# ============================================================
option(SOLRA_BUILD_RENDER "Build rendering engine" ON)
option(SOLRA_BUILD_INFERENCE "Build on-device inference engine" ON)
option(SOLRA_BUILD_STREAMING "Build streaming/asset loading engine" ON)
option(SOLRA_BUILD_WEBRTC "Build WebRTC integration" OFF)
option(SOLRA_BUILD_ANIMATION "Build animation system" OFF)
option(SOLRA_BUILD_TESTS "Build unit tests and benchmarks" OFF)
option(SOLRA_BUILD_EXAMPLES "Build example applications" OFF)

option(SOLRA_ENABLE_METAL "Enable Metal rendering backend" OFF)
option(SOLRA_ENABLE_VULKAN "Enable Vulkan rendering backend" OFF)
option(SOLRA_ENABLE_OPENGLES "Enable OpenGL ES fallback backend" OFF)
option(SOLRA_ENABLE_COREML "Enable CoreML NPU backend" OFF)
option(SOLRA_ENABLE_NNAPI "Enable Android NNAPI backend" OFF)

# ============================================================
# Platform Detection
# ============================================================
if(APPLE)
  set(SOLRA_PLATFORM_APPLE ON)
  if(CMAKE_SYSTEM_PROCESSOR STREQUAL "arm64" OR CMAKE_SYSTEM_PROCESSOR STREQUAL "aarch64")
    set(SOLRA_PLATFORM_APPLE_SILICON ON)
  endif()
elseif(ANDROID)
  set(SOLRA_PLATFORM_ANDROID ON)
elseif(WIN32)
  set(SOLRA_PLATFORM_WINDOWS ON)
elseif(UNIX)
  set(SOLRA_PLATFORM_LINUX ON)
endif()

# ============================================================
# Platform-specific Defaults
# ============================================================
if(SOLRA_PLATFORM_APPLE)
  set(SOLRA_ENABLE_METAL ON CACHE BOOL "Enable Metal rendering backend" FORCE)
  set(SOLRA_ENABLE_COREML ON CACHE BOOL "Enable CoreML NPU backend" FORCE)
  set(CMAKE_OSX_DEPLOYMENT_TARGET "16.0" CACHE STRING "Minimum macOS/iOS deployment target")
endif()

if(SOLRA_PLATFORM_ANDROID)
  set(SOLRA_ENABLE_VULKAN ON CACHE BOOL "Enable Vulkan rendering backend" FORCE)
  set(SOLRA_ENABLE_OPENGLES ON CACHE BOOL "Enable OpenGL ES fallback" FORCE)
  set(SOLRA_ENABLE_NNAPI ON CACHE BOOL "Enable Android NNAPI backend" FORCE)
endif()

if(SOLRA_PLATFORM_WINDOWS)
  set(SOLRA_ENABLE_VULKAN ON CACHE BOOL "Enable Vulkan rendering backend" FORCE)
endif()

# ============================================================
# Output Directories
# ============================================================
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)

# ============================================================
# Dependencies via vcpkg + platform-specific
# ============================================================
include(cmake/FindDependencies.cmake)

# ============================================================
# Public Headers
# ============================================================
set(SOLRA_PUBLIC_HEADERS
  include/solra/solra_core.h
  include/solra/solra_types.h
  include/solra/solra_render.h
  include/solra/solra_inference.h
  include/solra/solra_streaming.h
  include/solra/solra_webrtc.h
  include/solra/solra_animation.h
)

# ============================================================
# Source Modules
# ============================================================
# Core (always built)
add_subdirectory(src/core)

# Rendering Engine
if(SOLRA_BUILD_RENDER)
  add_subdirectory(src/render)
endif()

# On-device Inference Engine
if(SOLRA_BUILD_INFERENCE)
  add_subdirectory(src/inference)
endif()

# Streaming / Asset Loading Engine
if(SOLRA_BUILD_STREAMING)
  add_subdirectory(src/streaming)
endif()

# WebRTC Integration
if(SOLRA_BUILD_WEBRTC)
  add_subdirectory(src/webrtc)
endif()

# Animation System
if(SOLRA_BUILD_ANIMATION)
  add_subdirectory(src/animation)
endif()

# Platform Abstraction
add_subdirectory(src/platform)

# ============================================================
# Tests
# ============================================================
if(SOLRA_BUILD_TESTS)
  enable_testing()
  add_subdirectory(tests/unit)
  add_subdirectory(tests/benchmark)
endif()

# ============================================================
# Install
# ============================================================
include(GNUInstallDirs)

install(DIRECTORY include/solra
  DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
  FILES_MATCHING PATTERN "*.h"
)

# Collect all built targets for installation
set(SOLRA_INSTALL_TARGETS solra_core_core)

if(TARGET solra_core_render)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_render)
endif()
if(TARGET solra_core_inference)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_inference)
endif()
if(TARGET solra_core_streaming)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_streaming)
endif()
if(TARGET solra_core_webrtc)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_webrtc)
endif()
if(TARGET solra_core_animation)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_animation)
endif()
if(TARGET solra_core_platform)
  list(APPEND SOLRA_INSTALL_TARGETS solra_core_platform)
endif()

install(TARGETS ${SOLRA_INSTALL_TARGETS}
  EXPORT solraCoreTargets
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)

install(EXPORT solraCoreTargets
  FILE solraCoreTargets.cmake
  NAMESPACE solra::
  DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/solra
)
