#pragma once
// WebRTC Integration: libwebrtc static linking + CMake build configuration
// This header defines the platform-specific include path selection for libwebrtc.
// The actual WebRTC features are used through the public C++ API in src/webrtc/.
#include <cstdint>
#include <string>

namespace solra::webrtc {

// ---- Build configuration constants ----
// These reflect how libwebrtc was built for each target platform.

struct WebRTCBuildInfo {
    std::string version;        // e.g. "m124" (branch-heads/6367)
    std::string revision;       // git hash
    std::string buildConfig;    // "release" or "debug"
    std::string targetCpu;      // "arm64", "x64"
    std::string targetOs;       // "ios", "android", "mac", "win"
    bool rttiEnabled = false;   // WebRTC builds with -fno-rtti by default
    bool exceptionsEnabled = false;
};

// ---- Library linkage ----
// libwebrtc is built as a monolithic static library (libwebrtc.a / webrtc.lib).
// We trim unnecessary modules to reduce binary size:
//
//   INCLUDED:
//     ✓ pc (PeerConnection)
//     ✓ api (public C++ API)
//     ✓ media (audio/video codecs: Opus, H.264, VP8/VP9)
//     ✓ p2p (ICE/STUN/TURN)
//     ✓ rtc_base (threading, logging, networking)
//     ✓ call (media call control)
//
//   TRIMMED:
//     ✗ video_capture (camera capture - not needed)
//     ✗ video_render (desktop rendering - not needed)
//     ✗ desktop_capture (screen sharing - P3)
//     ✗ test framework
//     ✗ examples
//
// Result: ~8-12 MB per platform (arm64 release)

// ---- CMake integration snippet (for reference) ----
/*
# In core/CMakeLists.txt:
option(SOLRA_USE_WEBRTC "Enable WebRTC integration" ON)

if(SOLRA_USE_WEBRTC)
    set(WEBRTC_ROOT "${CMAKE_SOURCE_DIR}/third_party/libwebrtc")

    if(IOS)
        set(WEBRTC_LIB "${WEBRTC_ROOT}/ios-arm64/libwebrtc.a")
        set(WEBRTC_INCLUDE "${WEBRTC_ROOT}/include")
    elseif(ANDROID)
        set(WEBRTC_LIB "${WEBRTC_ROOT}/android-arm64-v8a/libwebrtc.a")
        set(WEBRTC_INCLUDE "${WEBRTC_ROOT}/include")
    elseif(WIN32)
        set(WEBRTC_LIB "${WEBRTC_ROOT}/win-x64/webrtc.lib")
        set(WEBRTC_INCLUDE "${WEBRTC_ROOT}/include")
    elseif(APPLE)
        set(WEBRTC_LIB "${WEBRTC_ROOT}/macos-universal/libwebrtc.a")
        set(WEBRTC_INCLUDE "${WEBRTC_ROOT}/include")
    endif()

    target_include_directories(solra_core PRIVATE ${WEBRTC_INCLUDE})
    target_link_libraries(solra_core PRIVATE ${WEBRTC_LIB})

    target_compile_definitions(solra_core PRIVATE
        SOLRA_HAS_WEBRTC=1
        WEBRTC_POSIX
        WEBRTC_MAC  # or WEBRTC_WIN / WEBRTC_ANDROID
    )
endif()
*/

// ---- Version check helper ----
inline bool isWebrtcAvailable() {
#if defined(SOLRA_HAS_WEBRTC)
    return true;
#else
    return false;
#endif
}

inline const char* webrtcVersionString() {
#if defined(SOLRA_HAS_WEBRTC)
    return "m124"; // Update to match prebuilt library version
#else
    return "not-linked";
#endif
}

} // namespace solra::webrtc
