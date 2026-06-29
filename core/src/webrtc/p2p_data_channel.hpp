#pragma once
// P2P Data Channel: low-latency state synchronization via WebRTC DataChannel
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <optional>

namespace solra::webrtc {

// ---- Data channel configuration ----
enum class DataChannelReliability {
    Reliable,        // TCP-like: ordered, guaranteed delivery
    Unreliable,      // UDP-like: unordered, no retransmission
    SemiReliable,    // maxRetransmits or maxPacketLifeTime
};

struct DataChannelConfig {
    std::string label;
    DataChannelReliability reliability = DataChannelReliability::Reliable;
    int maxRetransmits = -1;          // -1 = unlimited (reliable mode)
    int maxPacketLifeTimeMs = -1;     // -1 = unlimited
    bool negotiated = false;
    int negotiatedId = -1;            // -1 = auto-assign
    bool ordered = true;
};

// ---- Message types ----
struct DataMessage {
    std::string type;                 // "state_update", "input_event", "avatar_anim", etc.
    std::vector<uint8_t> payload;
    uint64_t sequence = 0;            // monotonic sequence number
    uint64_t timestampUs = 0;         // sender timestamp
    bool isBinary = true;
};

// ---- Data channel events ----
enum class DataChannelState { Connecting, Open, Closing, Closed };

class DataChannel {
public:
    virtual ~DataChannel() = default;

    virtual std::string label() const = 0;
    virtual DataChannelState state() const = 0;
    virtual bool send(const DataMessage& msg) = 0;
    virtual bool sendRaw(const uint8_t* data, size_t size, bool binary = true) = 0;

    using MessageCallback = std::function<void(const DataMessage&)>;
    using StateCallback = std::function<void(DataChannelState)>;
    virtual void onMessage(MessageCallback cb) = 0;
    virtual void onStateChange(StateCallback cb) = 0;

    virtual uint64_t bytesSent() const = 0;
    virtual uint64_t bytesReceived() const = 0;
};

// ---- P2P Session ----
struct PeerInfo {
    std::string peerId;
    std::string displayName;
};

class P2PSession {
public:
    virtual ~P2PSession() = default;

    // Connection management
    virtual bool connect(const PeerInfo& peer) = 0;
    virtual void disconnect() = 0;
    virtual bool isConnected() const = 0;

    // Data channels
    virtual std::shared_ptr<DataChannel> createDataChannel(const DataChannelConfig& config) = 0;
    virtual std::vector<std::shared_ptr<DataChannel>> dataChannels() const = 0;

    // Events
    using PeerCallback = std::function<void(const PeerInfo&)>;
    using DataChannelCallback = std::function<void(std::shared_ptr<DataChannel>)>;
    virtual void onPeerConnected(PeerCallback cb) = 0;
    virtual void onPeerDisconnected(PeerCallback cb) = 0;
    virtual void onDataChannel(DataChannelCallback cb) = 0;

    // Stats
    struct P2PStats {
        uint64_t packetsSent, packetsReceived, bytesSent, bytesReceived;
        uint32_t rttMs;
        std::string localCandidateType, remoteCandidateType; // "host", "srflx", "relay"
        std::string transportType; // "udp", "tcp", "tls"
    };
    virtual P2PStats stats() const = 0;
};

// ---- Factory ----
std::shared_ptr<P2PSession> createP2PSession();

} // namespace solra::webrtc
