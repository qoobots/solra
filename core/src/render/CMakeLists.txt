# Solra Core SDK - Render module

set(SOLRA_RENDER_SOURCES
  render.cpp
  scene.cpp
)

# Platform-specific backend sources
if(SOLRA_ENABLE_METAL)
  list(APPEND SOLRA_RENDER_SOURCES metal/renderer_metal.cpp)
endif()

if(SOLRA_ENABLE_VULKAN)
  list(APPEND SOLRA_RENDER_SOURCES vulkan/renderer_vulkan.cpp)
endif()

if(SOLRA_ENABLE_OPENGLES)
  list(APPEND SOLRA_RENDER_SOURCES opengl/renderer_gles.cpp)
endif()

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

# Metal backend
if(SOLRA_ENABLE_METAL)
  target_link_libraries(solra_core_render
    PRIVATE
      "${METAL_LIBRARY}"
      "${METALKIT_LIBRARY}"
      "${QUARTZCORE_LIBRARY}"
  )
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_METAL=1)
  # Add Objective-C++ support for Metal
  set_source_files_properties(metal/renderer_metal.cpp PROPERTIES
    COMPILE_FLAGS "-x objective-c++"
    LANGUAGE OBJCXX
  )
endif()

# Vulkan backend
if(SOLRA_ENABLE_VULKAN AND Vulkan_FOUND)
  target_link_libraries(solra_core_render PRIVATE Vulkan::Vulkan)
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_VULKAN=1)
endif()

# OpenGL ES backend
if(SOLRA_ENABLE_OPENGLES)
  target_link_libraries(solra_core_render PRIVATE OpenGL::OpenGL OpenGL::EGL)
  target_compile_definitions(solra_core_render PRIVATE SOLRA_RENDER_OPENGLES=1)
endif()

target_compile_features(solra_core_render PUBLIC cxx_std_17)
