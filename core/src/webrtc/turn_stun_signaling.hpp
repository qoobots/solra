#pragma once
// TURN/STUN Signaling: NAT traversal + TURN relay fallback
#include <cstdint>
#include <string>
#include <vector>

namespace solra::webrtc {

// ---- ICE Server Configuration ----
struct IceServer {
    std::string uri;            // "stun:stun.l.google.com:19302" | "turn:turn.example.com:3478?transport=tcp"
    std::string username;
    std::string credential;    // password or HMAC key
    enum class Type { STUN, TURN, TURNS };
    Type type = Type::STUN;
};

struct IceConfig {
    std::vector<IceServer> iceServers;
    std::string iceTransportPolicy = "all";  // "all" | "relay" (relay-only = no P2P)
    uint32_t iceCandidatePoolSize = 1;
    uint32_t stunKeepaliveIntervalMs = 15'000;
    bool enableIceTcp = true;                // TCP candidates for restrictive firewalls
    bool enableIpv6 = false;
};

// ---- Default STUN/TURN servers ----
inline IceConfig defaultIceConfig() {
    IceConfig cfg;
    cfg.iceServers = {
        // Google public STUN (free, best effort)
        {"stun:stun.l.google.com:19302", "", "", IceServer::Type::STUN},
        {"stun:stun1.l.google.com:19302", "", "", IceServer::Type::STUN},
    };
    return cfg;
}

inline IceConfig productionIceConfig() {
    // Solra production: self-hosted coturn cluster
    // External: turn.solra.io (public IP) → coturn → internal relay
    // Internal: turn-internal.solra.cluster (VPC-only, lower latency)
    IceConfig cfg;
    cfg.iceServers = {
        {"turn:turn.solra.io:3478?transport=udp", "solra", "{{TURN_SECRET}}", IceServer::Type::TURN},
        {"turn:turn.solra.io:3478?transport=tcp", "solra", "{{TURN_SECRET}}", IceServer::Type::TURN},
        {"turns:turn.solra.io:5349?transport=tcp", "solra", "{{TURN_SECRET}}", IceServer::Type::TURNS},
        // Internal (VPC)
        {"turn:turn-internal.solra.cluster:3478", "solra", "{{TURN_SECRET}}", IceServer::Type::TURN},
    };
    cfg.iceCandidatePoolSize = 2;
    return cfg;
}

// ---- ICE Candidate ----
struct IceCandidate {
    std::string sdpMid;
    int sdpMLineIndex = 0;
    std::string candidate; // "candidate:... 1 udp 2122252543 192.168.1.1 54321 typ host ..."
    enum class Type { Host, ServerReflexive, PeerReflexive, Relay };
    Type type = Type::Host;
    std::string ip;
    uint16_t port = 0;
    std::string protocol;  // "udp", "tcp"
    uint32_t priority = 0;
};

// ---- ICE Connection State ----
enum class IceConnectionState {
    New, Checking, Connected, Completed, Failed, Disconnected, Closed
};

const char* iceStateToString(IceConnectionState state);

// ---- NAT type detection (for diagnostics) ----
enum class NatType {
    Unknown,
    Open,               // Full Cone NAT
    RestrictedCone,     // Restricted Cone NAT
    PortRestrictedCone, // Port Restricted Cone NAT
    Symmetric,          // Symmetric NAT (hardest for P2P)
    UdpBlocked          // Firewall blocks UDP entirely
};

// Simplified NAT type detection (requires 2 STUN servers)
NatType detectNatType(const std::string& stunServer1, const std::string& stunServer2);

} // namespace solra::webrtc
