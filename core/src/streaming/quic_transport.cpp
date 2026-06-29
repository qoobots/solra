#include "quic_transport.hpp"
#include <stdexcept>

namespace solra::streaming {

// Stub: msquic / Cronet / Network.framework integration
// Final build links platform-specific QUIC stack:
//   - Windows/Linux: msquic (libmsquic.so / msquic.dll)
//   - iOS/macOS:     Network.framework (NWConnection + NWMultiplexGroup)
//   - Android:        Cronet (Java_com.google.net.cronet)
//
// The abstraction provides a uniform C++ API regardless of platform.

std::unique_ptr<QuicTransport> createQuicTransport() {
#if defined(SOLRA_PLATFORM_IOS) || defined(SOLRA_PLATFORM_MACOS)
    // return std::make_unique<AppleQuicTransport>();
#elif defined(SOLRA_PLATFORM_ANDROID)
    // return std::make_unique<CronetQuicTransport>();
#else
    // return std::make_unique<MsQuicTransport>();
#endif
    return nullptr; // stub
}

} // namespace solra::streaming
