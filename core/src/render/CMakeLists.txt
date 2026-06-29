# Solra Core SDK - Render module

set(SOLRA_RENDER_SOURCES
  render.cpp
  scene.cpp
  scene_graph.cpp
  gpu_abstraction.cpp
  shader_compiler.cpp
  pbr_material.cpp
  render_benchmark.cpp
)

add_library(solra_core_render STATIC ${SOLRA_RENDER_SOURCES})
target_include_directories(solra_core_render
  PUBLIC ${CMAKE_SOURCE_DIR}/include
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_render
  PUBLIC
    solra_core_core
    glm::glm
    spdlog::spdlog
    nlohmann_json::nlohmann_json
)

# Platform-specific backend flags (rendering backends resolved at runtime via gpu_abstraction)
if(SOLRA_ENABLE_METAL)
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_METAL=1)
endif()

if(SOLRA_ENABLE_VULKAN AND Vulkan_FOUND)
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_VULKAN=1)
endif()

if(SOLRA_ENABLE_OPENGLES)
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_OPENGLES=1)
endif()

target_compile_features(solra_core_render PUBLIC cxx_std_17)
