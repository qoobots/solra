/*
 * Solra Core SDK - Signaling Client
 *
 * WebSocket-based signaling client for WebRTC session establishment.
 * Handles room join/leave, SDP exchange, ICE candidate relay, and
 * heartbeat/keepalive with the signaling server.
 *
 * Protocol: JSON over WebSocket
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <map>
#include <atomic>
#include <mutex>
#include <thread>
#include <chrono>

namespace solra::webrtc {

// ============================================================
// Signaling Message Types
// ============================================================

enum class SignalingMessageType : uint8_t {
    kJoin,              // Client → Server: join a room
    kLeave,             // Client → Server: leave a room
    kOffer,             // Peer → Peer: SDP offer
    kAnswer,            // Peer → Peer: SDP answer
    kIceCandidate,      // Peer → Peer: ICE candidate
    kPeerJoined,        // Server → Client: new peer joined
    kPeerLeft,          // Server → Client: peer left
    kRoomState,         // Server → Client: full room state
    kHeartbeat,         // Bidirectional: keepalive ping/pong
    kError,             // Server → Client: error
    kMute,              // Client → Server: mute/unmute audio/video
    kCustom,            // Application-defined message
};

// ============================================================
// Signaling Message
// ============================================================

struct SignalingMessage {
    SignalingMessageType type = SignalingMessageType::kCustom;
    std::string room_id;
    std::string sender_id;      // Peer ID (server-assigned or client-chosen)
    std::string target_id;      // Target peer ID (empty = broadcast)
    std::string sdp;            // SDP body (for offer/answer)
    std::string sdp_type;       // "offer" or "answer"
    std::string candidate;      // ICE candidate string
    std::string sdp_mid;        // ICE media stream ID
    int sdp_mline_index = 0;    // ICE media line index
    std::string payload;        // Generic JSON payload (for custom messages)
    int64_t timestamp = 0;      // Unix millisecond timestamp
    int error_code = 0;         // Error code (for kError)
    std::string error_message;  // Error description

    // Serialize to JSON
    std::string toJson() const;

    // Deserialize from JSON
    static SignalingMessage fromJson(const std::string& json);
};

// ============================================================
// Room Peer Info
// ============================================================

struct RoomPeerInfo {
    std::string peer_id;
    std::string display_name;
    bool audio_enabled = true;
    bool video_enabled = false;
    bool is_host = false;
    std::chrono::steady_clock::time_point joined_at;
    std::map<std::string, std::string> metadata;
};

// ============================================================
// Signaling Client Config
// ============================================================

struct SignalingConfig {
    /// WebSocket URL for signaling server
    std::string server_url = "wss://signal.solra.io/ws";

    /// Client-generated peer ID (empty = server assigns)
    std::string peer_id;

    /// Display name for the room
    std::string display_name = "Solra Client";

    /// Authentication token (JWT or API key)
    std::string auth_token;

    /// Heartbeat interval (seconds)
    uint32_t heartbeat_interval_sec = 15;

    /// Reconnect delay (seconds)
    uint32_t reconnect_delay_sec = 3;

    /// Max reconnect attempts (0 = unlimited)
    uint32_t max_reconnect_attempts = 10;

    /// Connection timeout (seconds)
    uint32_t connect_timeout_sec = 10;

    /// Enable automatic reconnection
    bool auto_reconnect = true;

    /// Enable verbose logging
    bool verbose_logging = false;
};

// ============================================================
// Signaling Client
// ============================================================

class SignalingClient {
public:
    explicit SignalingClient(const SignalingConfig& config = {});
    ~SignalingClient();

    // === Lifecycle ===
    /// Connect to the signaling server
    /// @return true if connection initiated (async)
    bool connect();

    /// Disconnect from the signaling server
    void disconnect();

    /// Check if connected
    bool isConnected() const;

    /// Get the assigned peer ID
    std::string getPeerId() const;

    // === Room Management ===
    /// Join a room
    /// @param room_id Room identifier
    /// @return true if join request sent
    bool joinRoom(const std::string& room_id);

    /// Leave current room
    void leaveRoom();

    /// Get current room ID (empty if not in a room)
    std::string getRoomId() const;

    /// Get list of peers in current room
    std::vector<RoomPeerInfo> getRoomPeers() const;

    // === SDP Exchange ===
    /// Send SDP offer to a peer
    void sendOffer(const std::string& target_peer, const std::string& sdp);

    /// Send SDP answer to a peer
    void sendAnswer(const std::string& target_peer, const std::string& sdp);

    // === ICE Candidates ===
    /// Send ICE candidate to a peer
    void sendIceCandidate(const std::string& target_peer,
                          const std::string& candidate,
                          const std::string& sdp_mid,
                          int sdp_mline_index);

    // === Media Control ===
    /// Set audio mute state (notifies server and peers)
    void setAudioMuted(bool muted);

    /// Set video mute state
    void setVideoMuted(bool muted);

    // === Custom Messages ===
    /// Send a custom JSON message to a peer or broadcast
    void sendCustomMessage(const std::string& target_peer, const std::string& json_payload);

    // === Callbacks ===
    using MessageCallback = std::function<void(const SignalingMessage& msg)>;
    using PeerCallback = std::function<void(const std::string& peer_id, const RoomPeerInfo& info)>;
    using StateCallback = std::function<void(bool connected)>;
    using ErrorCallback = std::function<void(int code, const std::string& message)>;

    void onOffer(MessageCallback cb);
    void onAnswer(MessageCallback cb);
    void onIceCandidate(MessageCallback cb);
    void onPeerJoined(PeerCallback cb);
    void onPeerLeft(PeerCallback cb);
    void onRoomState(MessageCallback cb);
    void onConnectionStateChange(StateCallback cb);
    void onError(ErrorCallback cb);
    void onCustomMessage(MessageCallback cb);

    // === Stats ===
    struct SignalingStats {
        uint64_t messages_sent = 0;
        uint64_t messages_received = 0;
        uint64_t bytes_sent = 0;
        uint64_t bytes_received = 0;
        uint32_t reconnect_count = 0;
        uint64_t last_heartbeat_ms = 0;
        uint64_t round_trip_ms = 0;
    };
    SignalingStats getStats() const;

private:
    SignalingConfig config_;
    std::string assigned_peer_id_;
    std::string current_room_id_;
    std::map<std::string, RoomPeerInfo> room_peers_;
    std::atomic<bool> connected_{false};
    std::atomic<bool> running_{false};

    // WebSocket connection (platform-specific, using libwebsockets or platform API)
    void* ws_handle_ = nullptr;

    // Threads
    std::thread io_thread_;
    std::thread heartbeat_thread_;
    std::chrono::steady_clock::time_point last_heartbeat_;
    uint32_t reconnect_attempts_ = 0;

    // Callbacks
    MessageCallback on_offer_cb_;
    MessageCallback on_answer_cb_;
    MessageCallback on_ice_candidate_cb_;
    PeerCallback on_peer_joined_cb_;
    PeerCallback on_peer_left_cb_;
    MessageCallback on_room_state_cb_;
    StateCallback on_connection_cb_;
    ErrorCallback on_error_cb_;
    MessageCallback on_custom_cb_;

    // Stats
    mutable std::mutex stats_mutex_;
    SignalingStats stats_;

    mutable std::mutex mutex_;

    // Internal
    void ioLoop();
    void heartbeatLoop();
    void handleMessage(const std::string& json);
    void sendMessage(const SignalingMessage& msg);
    void scheduleReconnect();
    bool openWebSocket();
    void closeWebSocket();
    bool sendWebSocketFrame(const std::string& data);
    std::string receiveWebSocketFrame();
};

// ============================================================
// Audio/Video Stream Manager
// ============================================================

struct AudioStreamConfig {
    uint32_t sample_rate = 48000;
    uint32_t channels = 1;          // Mono for spatial audio
    uint32_t bitrate_bps = 64000;   // Opus @ 64kbps
    bool echo_cancellation = true;
    bool noise_suppression = true;
    bool auto_gain_control = true;
    bool stereo_spatial = false;    // Stereo input for HRTF
};

struct VideoStreamConfig {
    uint32_t width = 1280;
    uint32_t height = 720;
    uint32_t fps = 30;
    uint32_t bitrate_bps = 2'500'000; // VP9/H.265 @ 2.5Mbps
    bool simulcast = false;           // Send multiple quality layers
    bool svc_scalability = true;      // Temporal scalability
    std::string codec = "vp9";        // "vp8", "vp9", "h264", "h265", "av1"
};

enum class StreamDirection {
    kSendOnly,
    kReceiveOnly,
    kSendReceive,
    kInactive,
};

class AudioVideoStreamManager {
public:
    explicit AudioVideoStreamManager(void* peerConnectionFactory = nullptr);
    ~AudioVideoStreamManager();

    // === Audio ===
    /// Create an audio track for sending
    /// @return Track ID, or empty string on failure
    std::string createAudioTrack(const AudioStreamConfig& config = {});

    /// Add an audio track to a peer connection
    bool addAudioTrack(void* peerConnection, const std::string& track_id,
                       StreamDirection direction = StreamDirection::kSendReceive);

    /// Remove an audio track
    void removeAudioTrack(const std::string& track_id);

    /// Set audio input data (PCM samples from mic or file)
    void pushAudioData(const std::string& track_id,
                       const int16_t* samples, uint32_t sample_count);

    /// Get audio output data (received from remote peer)
    bool pullAudioData(const std::string& track_id,
                       int16_t* buffer, uint32_t max_samples, uint32_t* out_count);

    /// Check if audio track is active
    bool isAudioActive(const std::string& track_id) const;

    // === Video ===
    /// Create a video track for sending
    std::string createVideoTrack(const VideoStreamConfig& config = {});

    /// Add a video track to a peer connection
    bool addVideoTrack(void* peerConnection, const std::string& track_id,
                       StreamDirection direction = StreamDirection::kSendReceive);

    /// Remove a video track
    void removeVideoTrack(const std::string& track_id);

    /// Push video frame (RGBA or NV12)
    void pushVideoFrame(const std::string& track_id,
                        const uint8_t* rgba_data, uint32_t width, uint32_t height);

    /// Get received video frame
    bool pullVideoFrame(const std::string& track_id,
                        uint8_t* buffer, uint32_t buffer_size,
                        uint32_t* out_width, uint32_t* out_height);

    /// Check if video track is active
    bool isVideoActive(const std::string& track_id) const;

    // === Codec Control ===
    /// Set preferred audio codec
    void setAudioCodec(const std::string& codec); // "opus", "pcmu", "pcma"

    /// Set preferred video codec
    void setVideoCodec(const std::string& codec); // "vp8", "vp9", "h264", "h265", "av1"

    /// Get negotiated codec for a track
    std::string getNegotiatedCodec(const std::string& track_id) const;

    // === Bitrate Control ===
    /// Dynamically adjust bitrate (congestion control)
    void setAudioBitrate(const std::string& track_id, uint32_t bitrate_bps);
    void setVideoBitrate(const std::string& track_id, uint32_t bitrate_bps);

    // === Stats ===
    struct StreamStats {
        uint32_t audio_tracks = 0;
        uint32_t video_tracks = 0;
        uint64_t audio_bytes_sent = 0;
        uint64_t audio_bytes_received = 0;
        uint64_t video_bytes_sent = 0;
        uint64_t video_bytes_received = 0;
        float audio_packet_loss = 0.0f;
        float video_packet_loss = 0.0f;
        uint32_t audio_jitter_ms = 0;
        uint32_t video_jitter_ms = 0;
    };
    StreamStats getStats() const;

    // === Lifecycle ===
    void shutdown();

private:
    void* factory_ = nullptr;
    std::map<std::string, void*> audio_tracks_;    // track_id → audio track handle
    std::map<std::string, void*> video_tracks_;    // track_id → video track handle
    std::map<std::string, AudioStreamConfig> audio_configs_;
    std::map<std::string, VideoStreamConfig> video_configs_;
    mutable std::mutex mutex_;
};

} // namespace solra::webrtc
