# Solra Core SDK - Core module
# SDK initialization, lifecycle management, and main loop

set(SOLRA_CORE_SOURCES
  core.cpp
  version.cpp
)

add_library(solra_core_core STATIC ${SOLRA_CORE_SOURCES})
target_include_directories(solra_core_core
  PUBLIC $<BUILD_INTERFACE:${CMAKE_SOURCE_DIR}/include>
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_core
  PUBLIC
    spdlog::spdlog
    nlohmann_json::nlohmann_json
)
target_compile_features(solra_core_core PUBLIC cxx_std_17)
