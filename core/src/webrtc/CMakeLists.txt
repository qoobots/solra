# Solra Core SDK - WebRTC module

set(SOLRA_WEBRTC_SOURCES
  webrtc.cpp
  p2p_data_channel.cpp
  webrtc_session.cpp
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

# libwebrtc linkage (when available)
if(SOLRA_HAS_WEBRTC)
  target_compile_definitions(solra_core_webrtc PRIVATE SOLRA_HAS_WEBRTC=1)
  # Link platform-specific libwebrtc static library
  if(IOS)
    target_link_libraries(solra_core_webrtc PRIVATE
      ${WEBRTC_ROOT}/ios-arm64/libwebrtc.a
    )
  elseif(ANDROID)
    target_link_libraries(solra_core_webrtc PRIVATE
      ${WEBRTC_ROOT}/android-arm64-v8a/libwebrtc.a
    )
  elseif(WIN32)
    target_link_libraries(solra_core_webrtc PRIVATE
      ${WEBRTC_ROOT}/win-x64/webrtc.lib
    )
  elseif(APPLE)
    target_link_libraries(solra_core_webrtc PRIVATE
      ${WEBRTC_ROOT}/macos-universal/libwebrtc.a
    )
  endif()
endif()
