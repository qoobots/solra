# Solra Core SDK - Dependency discovery module
# Finds and configures all third-party dependencies for the build

# ============================================================
# GLM - OpenGL Mathematics (header-only)
# ============================================================
find_package(glm CONFIG REQUIRED)
message(STATUS "GLM found: ${glm_VERSION}")

# ============================================================
# spdlog - Fast C++ logging library
# ============================================================
find_package(spdlog CONFIG REQUIRED)
message(STATUS "spdlog found: ${spdlog_VERSION}")

# ============================================================
# nlohmann_json - JSON for Modern C++
# ============================================================
find_package(nlohmann_json CONFIG REQUIRED)
message(STATUS "nlohmann_json found: ${nlohmann_json_VERSION}")

# ============================================================
# Protocol Buffers
# ============================================================
find_package(Protobuf REQUIRED)
message(STATUS "Protobuf found: ${Protobuf_VERSION}")

# ============================================================
# ZLIB
# ============================================================
find_package(ZLIB REQUIRED)
message(STATUS "ZLIB found: ${ZLIB_VERSION}")

# ============================================================
# OpenSSL
# ============================================================
find_package(OpenSSL REQUIRED)
message(STATUS "OpenSSL found: ${OPENSSL_VERSION}")

# ============================================================
# Optional: Google Test
# ============================================================
if(SOLRA_BUILD_TESTS)
  find_package(GTest CONFIG REQUIRED)
  message(STATUS "GTest found: ${GTest_VERSION}")
  enable_testing()
endif()

# ============================================================
# Optional: Google Benchmark
# ============================================================
if(SOLRA_BUILD_TESTS)
  find_package(benchmark CONFIG REQUIRED)
  message(STATUS "Google Benchmark found: ${benchmark_VERSION}")
endif()

# ============================================================
# Platform-specific dependencies
# ============================================================

# iOS/macOS Metal
if(SOLRA_ENABLE_METAL)
  find_library(METAL_LIBRARY Metal REQUIRED)
  find_library(METALKIT_LIBRARY MetalKit REQUIRED)
  find_library(QUARTZCORE_LIBRARY QuartzCore REQUIRED)
  message(STATUS "Metal rendering enabled")
endif()

# Android Vulkan
if(SOLRA_ENABLE_VULKAN)
  find_package(Vulkan)
  if(NOT Vulkan_FOUND)
    # On Android, Vulkan is part of NDK
    message(STATUS "Vulkan not found as package, using NDK native lib")
  else()
    message(STATUS "Vulkan found: ${Vulkan_VERSION}")
  endif()
endif()

# OpenGL ES
if(SOLRA_ENABLE_OPENGLES)
  find_package(OpenGL REQUIRED COMPONENTS OpenGL EGL)
  message(STATUS "OpenGL ES enabled")
endif()

# ============================================================
# stb - Single-file public domain libraries (header-only)
# ============================================================
# stb is optional; not currently used by any source files.
# When needed, vendor the required stb header(s) directly.
set(STB_AVAILABLE FALSE)
message(STATUS "stb libraries: not required, skipping FetchContent")
