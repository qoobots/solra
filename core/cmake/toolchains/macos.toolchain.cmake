# Solra macOS arm64 + x86_64 Universal Toolchain
# Usage: cmake -B build/macos -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/macos.toolchain.cmake

set(CMAKE_SYSTEM_NAME Darwin)
set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64" CACHE STRING "Universal binary")
set(CMAKE_OSX_DEPLOYMENT_TARGET "13.0")

set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_definitions(SOLRA_PLATFORM_MACOS=1)
add_compile_definitions(SOLRA_ENABLE_METAL=1)
add_compile_definitions(SOLRA_ENABLE_COREML=1)

# Universal binary flags - build fat binary
set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64")

# Enable LTO for release
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -flto")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -flto")

# Address sanitizer for debug builds
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -fsanitize=address -fno-omit-frame-pointer")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -fsanitize=address -fno-omit-frame-pointer")
