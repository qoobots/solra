# Solra Core SDK - Inference module

set(SOLRA_INFERENCE_SOURCES
  inference.cpp
  inference_pipeline.cpp
  llama_integration.cpp
  model_ota.cpp
  npu_backend.cpp
  onnx_backend.cpp
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
    nlohmann_json::nlohmann_json
)
target_compile_features(solra_core_inference PUBLIC cxx_std_17)

# ONNX Runtime linkage (when available)
if(SOLRA_HAS_ONNX)
  target_compile_definitions(solra_core_inference PRIVATE SOLRA_HAS_ONNX=1)
  find_package(onnxruntime CONFIG QUIET)
  if(onnxruntime_FOUND)
    target_link_libraries(solra_core_inference PRIVATE
      onnxruntime::onnxruntime
    )
  else()
    # Manual linking via vcpkg/system path
    if(WIN32)
      target_link_libraries(solra_core_inference PRIVATE
        onnxruntime.lib
      )
    else()
      target_link_libraries(solra_core_inference PRIVATE
        onnxruntime
      )
    endif()
  endif()
endif()

# llama.cpp linkage (when available)
if(SOLRA_HAS_LLAMA)
  target_compile_definitions(solra_core_inference PRIVATE SOLRA_HAS_LLAMA=1)
  target_link_libraries(solra_core_inference PRIVATE
    llama
    ggml
  )
endif()
