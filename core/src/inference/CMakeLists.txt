# Solra Core SDK - Inference module

set(SOLRA_INFERENCE_SOURCES
  inference.cpp
  inference_pipeline.cpp
  llama_integration.cpp
  model_ota.cpp
  npu_backend.cpp
)

add_library(solra_core_inference STATIC ${SOLRA_INFERENCE_SOURCES})
target_include_directories(solra_core_inference
  PUBLIC $<BUILD_INTERFACE:${CMAKE_SOURCE_DIR}/include>
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
  PRIVATE ${CMAKE_SOURCE_DIR}/third_party/llama.cpp
)
target_link_libraries(solra_core_inference
  PUBLIC
    solra_core_core
    spdlog::spdlog
)
target_compile_features(solra_core_inference PUBLIC cxx_std_17)
