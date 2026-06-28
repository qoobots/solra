# Solra Core SDK - CMake toolchain for iOS cross-compilation
# Usage: cmake -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/ios.cmake -DPLATFORM=OS64 ...

if(NOT DEFINED CMAKE_SYSTEM_NAME)
  set(CMAKE_SYSTEM_NAME iOS)
endif()

if(NOT DEFINED CMAKE_OSX_DEPLOYMENT_TARGET)
  set(CMAKE_OSX_DEPLOYMENT_TARGET "14.0")
endif()

# Platform selection
set(PLATFORM "OS64" CACHE STRING "iOS platform: OS64 (arm64 device), SIMULATOR64 (arm64 simulator), SIMULATORARM64")

if(PLATFORM STREQUAL "OS64")
  set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "" FORCE)
  set(CMAKE_SYSTEM_PROCESSOR aarch64)
elseif(PLATFORM STREQUAL "SIMULATOR64")
  set(CMAKE_OSX_SYSROOT iphonesimulator)
  set(CMAKE_OSX_ARCHITECTURES "arm64" CACHE STRING "" FORCE)
  set(CMAKE_SYSTEM_PROCESSOR aarch64)
endif()

set(CMAKE_XCODE_ATTRIBUTE_CODE_SIGN_IDENTITY "iPhone Developer")
set(CMAKE_XCODE_ATTRIBUTE_DEVELOPMENT_TEAM "")

# Vcpkg triplet
set(VCPKG_TARGET_TRIPLET "arm64-ios" CACHE STRING "")
set(VCPKG_OSX_DEPLOYMENT_TARGET "${CMAKE_OSX_DEPLOYMENT_TARGET}")

# Set up solra platform defines
add_compile_definitions(SOLRA_PLATFORM_IOS=1)
add_compile_definitions(SOLRA_ENABLE_METAL=1)
add_compile_definitions(SOLRA_ENABLE_COREML=1)
