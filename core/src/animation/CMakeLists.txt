# Solra Core SDK - Animation module

set(SOLRA_ANIMATION_SOURCES
  animation.cpp
  blendshape.cpp
  limb_ik.cpp
  lipsync.cpp
  skeletal_animation.cpp
)

add_library(solra_core_animation STATIC ${SOLRA_ANIMATION_SOURCES})
target_include_directories(solra_core_animation
  PUBLIC ${CMAKE_SOURCE_DIR}/include
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_animation
  PUBLIC
    solra_core_core
    glm::glm
    spdlog::spdlog
)
target_compile_features(solra_core_animation PUBLIC cxx_std_17)
