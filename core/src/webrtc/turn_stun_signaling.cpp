#include "turn_stun_signaling.hpp"
#include <cstring>

namespace solra::webrtc {

const char* iceStateToString(IceConnectionState state) {
    switch (state) {
    case IceConnectionState::New:           return "new";
    case IceConnectionState::Checking:      return "checking";
    case IceConnectionState::Connected:     return "connected";
    case IceConnectionState::Completed:     return "completed";
    case IceConnectionState::Failed:        return "failed";
    case IceConnectionState::Disconnected:  return "disconnected";
    case IceConnectionState::Closed:        return "closed";
    }
    return "unknown";
}

NatType detectNatType(const std::string& stunServer1, const std::string& stunServer2) {
    // Simplified RFC 5780 NAT type detection
    // Full implementation:
    // 1. Send Binding Request to STUN server 1 → get mapped address/port (M1)
    // 2. Send Binding Request to STUN server 2 → get mapped address/port (M2)
    // 3. Send Binding Request with CHANGE-REQUEST to STUN server 1
    //    → response comes from different IP:port
    //
    //   M1 == M2? → Endpoint-Independent Mapping (Cone)
    //       Can receive from different IP? → Full Cone
    //       Cannot? → Restricted / Port-Restricted Cone
    //   M1 != M2? → Symmetric NAT
    //
    (void)stunServer1; (void)stunServer2;
    return NatType::Unknown; // stub
}

} // namespace solra::webrtc
