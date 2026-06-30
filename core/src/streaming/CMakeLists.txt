# Solra Core SDK - Streaming / Asset Loading module

set(SOLRA_STREAMING_SOURCES
  streaming.cpp
  cache.cpp
  lod_streaming.cpp
  predictive_preload.cpp
  quic_transport.cpp
  resumable_download.cpp
)

add_library(solra_core_streaming STATIC ${SOLRA_STREAMING_SOURCES})
target_include_directories(solra_core_streaming
  PUBLIC $<BUILD_INTERFACE:${CMAKE_SOURCE_DIR}/include>
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_streaming
  PUBLIC
    spdlog::spdlog
    ZLIB::ZLIB
)
target_compile_features(solra_core_streaming PUBLIC cxx_std_17)
