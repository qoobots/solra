#include "p2p_data_channel.hpp"

namespace solra::webrtc {

// Stub: wraps libwebrtc PeerConnection + DataChannel API
// Full implementation requires linked libwebrtc.a

std::shared_ptr<P2PSession> createP2PSession() {
#if defined(SOLRA_HAS_WEBRTC)
    // return std::make_shared<WebRTCSession>();
    return nullptr;
#else
    return nullptr; // WebRTC not compiled
#endif
}

} // namespace solra::webrtc
