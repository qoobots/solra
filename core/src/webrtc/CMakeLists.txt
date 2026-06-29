# Solra Core SDK - WebRTC module

set(SOLRA_WEBRTC_SOURCES
  webrtc.cpp
  p2p_data_channel.cpp
  spatial_audio.cpp
  turn_stun_signaling.cpp
)

add_library(solra_core_webrtc STATIC ${SOLRA_WEBRTC_SOURCES})
target_include_directories(solra_core_webrtc
  PUBLIC ${CMAKE_SOURCE_DIR}/include
  PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)
target_link_libraries(solra_core_webrtc
  PUBLIC
    solra_core_core
    spdlog::spdlog
    OpenSSL::SSL
    OpenSSL::Crypto
)
target_compile_features(solra_core_webrtc PUBLIC cxx_std_17)
