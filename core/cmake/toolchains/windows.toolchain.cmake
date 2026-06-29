# Solra Windows x64 Toolchain (MSVC)
# Usage: cmake -B build/windows -G "Visual Studio 17 2022" -A x64

set(CMAKE_SYSTEM_NAME Windows)
set(CMAKE_SYSTEM_PROCESSOR AMD64)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_definitions(-DSOLRA_PLATFORM_WINDOWS=1 -DSOLRA_GPU_VULKAN=1 -DNOMINMAX -DWIN32_LEAN_AND_MEAN)

# Enable AVX2 for inference acceleration
add_compile_options(/arch:AVX2 /MP)

# Linker optimizations
set(CMAKE_EXE_LINKER_FLAGS_RELEASE "${CMAKE_EXE_LINKER_FLAGS_RELEASE} /LTCG /OPT:REF /OPT:ICF")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} /LTCG /OPT:REF /OPT:ICF")

# MSVC specific: treat warnings as errors in CI
if(DEFINED ENV{CI})
    add_compile_options(/W4 /WX)
else()
    add_compile_options(/W3)
endif()
