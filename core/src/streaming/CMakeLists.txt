# Solra Core SDK - Streaming / Asset Loading module

set(SOLRA_STREAMING_SOURCES
  streaming.cpp
  cache.cpp
  lod_streaming.cpp
  predictive_preload.cpp
  quic_transport.cpp
  resumable_download.cpp
  http_downloader.cpp
  progressive_lod.cpp
)

add_library(solra_core_streaming STATIC ${SOLRA_STREAMING_SOURCES})
target_include_directories(solra_core_streaming
  PUBLIC $<BUILD_INTERFACE:${CMAKE_SOURCE_DIR}/include>
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_streaming
  PUBLIC
    solra_core_core
    spdlog::spdlog
    nlohmann_json::nlohmann_json
    ZLIB::ZLIB
)
target_compile_features(solra_core_streaming PUBLIC cxx_std_17)

# libcurl linkage (for HTTP downloads)
if(SOLRA_HAS_CURL)
  target_compile_definitions(solra_core_streaming PRIVATE SOLRA_HAS_CURL=1)
  find_package(CURL REQUIRED)
  target_link_libraries(solra_core_streaming PRIVATE CURL::libcurl)
endif()
