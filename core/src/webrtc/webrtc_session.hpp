#pragma once
// WebRTC Session: real libwebrtc PeerConnection + DataChannel implementation
// This replaces the stub p2p_data_channel.cpp when SOLRA_HAS_WEBRTC is defined.

#include "p2p_data_channel.hpp"
#include "turn_stun_signaling.hpp"
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <mutex>
#include <atomic>
#include <thread>

namespace solra::webrtc {

// ============================================================================
// WebRTCDataChannel — wraps webrtc::DataChannelInterface
// ============================================================================
class WebRTCDataChannel : public DataChannel {
public:
    explicit WebRTCDataChannel(void* nativeChannel, const DataChannelConfig& cfg);
    ~WebRTCDataChannel() override;

    std::string label() const override;
    DataChannelState state() const override;
    bool send(const DataMessage& msg) override;
    bool sendRaw(const uint8_t* data, size_t size, bool binary = true) override;
    void onMessage(MessageCallback cb) override;
    void onStateChange(StateCallback cb) override;
    uint64_t bytesSent() const override;
    uint64_t bytesReceived() const override;

    // Called from the WebRTC signaling thread
    void onNativeMessage(const uint8_t* data, size_t size, bool binary);
    void onNativeStateChange(int newState);

private:
    void* nativeChannel_; // webrtc::DataChannelInterface*
    DataChannelConfig config_;
    DataChannelState state_{DataChannelState::Connecting};
    MessageCallback messageCallback_;
    StateCallback stateCallback_;
    std::mutex mutex_;
    std::atomic<uint64_t> bytesSent_{0};
    std::atomic<uint64_t> bytesReceived_{0};
};

// ============================================================================
// WebRTCSession — wraps webrtc::PeerConnectionInterface
// ============================================================================
class WebRTCSession : public P2PSession {
public:
    WebRTCSession();
    ~WebRTCSession() override;

    bool connect(const PeerInfo& peer) override;
    void disconnect() override;
    bool isConnected() const override;

    std::shared_ptr<DataChannel> createDataChannel(const DataChannelConfig& config) override;
    std::vector<std::shared_ptr<DataChannel>> dataChannels() const override;

    void onPeerConnected(PeerCallback cb) override;
    void onPeerDisconnected(PeerCallback cb) override;
    void onDataChannel(DataChannelCallback cb) override;

    P2PStats stats() const override;

    // SDP operations
    std::string createOffer();
    std::string createAnswer();
    bool setLocalDescription(const std::string& sdp, const std::string& type);
    bool setRemoteDescription(const std::string& sdp, const std::string& type);
    bool addIceCandidate(const std::string& candidate, const std::string& sdpMid, int sdpMlineIndex);

    // ICE events (called from signaling thread)
    void onIceCandidate(const std::string& candidate, const std::string& sdpMid, int sdpMlineIndex);
    void onIceConnectionChange(IceConnectionState state);
    void onIceGatheringComplete();

    // Callback for externally collected ICE candidates
    using IceCandidateCallback = std::function<void(const IceCandidate&)>;
    void setIceCandidateCallback(IceCandidateCallback cb);

    // Configure ICE servers
    void setIceConfig(const IceConfig& config);

private:
    void* nativePeerConnection_; // webrtc::PeerConnectionInterface*
    std::string peerId_;
    PeerInfo peerInfo_;
    std::atomic<bool> connected_{false};

    PeerCallback onPeerConnectedCb_;
    PeerCallback onPeerDisconnectedCb_;
    DataChannelCallback onDataChannelCb_;
    IceCandidateCallback onIceCandidateCb_;

    std::vector<std::shared_ptr<WebRTCDataChannel>> channels_;
    mutable std::mutex mutex_;

    // Stats
    mutable std::atomic<uint64_t> packetsSent_{0};
    mutable std::atomic<uint64_t> packetsReceived_{0};
    mutable std::atomic<uint64_t> bytesSent_{0};
    mutable std::atomic<uint64_t> bytesReceived_{0};
    mutable std::atomic<uint32_t> rttMs_{0};

    IceConnectionState iceState_{IceConnectionState::New};
};

// ============================================================================
// WebRTCFactory — singleton WebRTC global initialization
// ============================================================================
class WebRTCFactory {
public:
    static WebRTCFactory& instance();

    bool initialize(const IceConfig& iceConfig);
    void shutdown();
    bool isInitialized() const;

    // Create a new PeerConnection with configured ICE servers
    std::shared_ptr<WebRTCSession> createSession();

private:
    WebRTCFactory() = default;
    ~WebRTCFactory();
    WebRTCFactory(const WebRTCFactory&) = delete;
    WebRTCFactory& operator=(const WebRTCFactory&) = delete;

    bool initialized_{false};
    IceConfig iceConfig_;
    std::mutex mutex_;

    // Native objects
    void* threadManager_{nullptr};     // rtc::ThreadManager
    void* signalingThread_{nullptr};   // rtc::Thread
    void* workerThread_{nullptr};      // rtc::Thread
    void* networkThread_{nullptr};     // rtc::Thread
    void* peerConnectionFactory_{nullptr}; // webrtc::PeerConnectionFactoryInterface
};

// Factory function — replaces the stub in p2p_data_channel.cpp
std::shared_ptr<P2PSession> createP2PSession();

} // namespace solra::webrtc
