# Solra Core SDK - Platform abstraction module

set(SOLRA_PLATFORM_SOURCES)

if(SOLRA_PLATFORM_APPLE)
  list(APPEND SOLRA_PLATFORM_SOURCES ios/platform_ios.mm)
  if(SOLRA_PLATFORM_APPLE_SILICON)
    list(APPEND SOLRA_PLATFORM_SOURCES macos/platform_macos.mm)
  endif()
elseif(SOLRA_PLATFORM_ANDROID)
  list(APPEND SOLRA_PLATFORM_SOURCES android/platform_android.cpp)
elseif(SOLRA_PLATFORM_WINDOWS)
  list(APPEND SOLRA_PLATFORM_SOURCES windows/platform_windows.cpp)
elseif(SOLRA_PLATFORM_LINUX)
  list(APPEND SOLRA_PLATFORM_SOURCES linux/platform_linux.cpp)
endif()

add_library(solra_core_platform STATIC ${SOLRA_PLATFORM_SOURCES})
target_include_directories(solra_core_platform
  PUBLIC ${CMAKE_SOURCE_DIR}/include
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_platform
  PUBLIC
    solra_core_core
    spdlog::spdlog
)
target_compile_features(solra_core_platform PUBLIC cxx_std_17)
