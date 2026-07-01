/*
 * Solra Core SDK - Signaling Client & Audio/Video Stream Implementation
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "signaling_client.hpp"
#include <spdlog/spdlog.h>
#include <nlohmann/json.hpp>
#include <cstring>
#include <sstream>
#include <algorithm>

namespace solra::webrtc {

using json = nlohmann::json;

// ============================================================
// SignalingMessage serialization
// ============================================================

static const char* msgTypeToString(SignalingMessageType type) {
    switch (type) {
        case SignalingMessageType::kJoin:         return "join";
        case SignalingMessageType::kLeave:        return "leave";
        case SignalingMessageType::kOffer:        return "offer";
        case SignalingMessageType::kAnswer:       return "answer";
        case SignalingMessageType::kIceCandidate: return "ice_candidate";
        case SignalingMessageType::kPeerJoined:   return "peer_joined";
        case SignalingMessageType::kPeerLeft:     return "peer_left";
        case SignalingMessageType::kRoomState:    return "room_state";
        case SignalingMessageType::kHeartbeat:    return "heartbeat";
        case SignalingMessageType::kError:        return "error";
        case SignalingMessageType::kMute:         return "mute";
        case SignalingMessageType::kCustom:       return "custom";
        default: return "unknown";
    }
}

static SignalingMessageType stringToMsgType(const std::string& s) {
    if (s == "join")          return SignalingMessageType::kJoin;
    if (s == "leave")         return SignalingMessageType::kLeave;
    if (s == "offer")         return SignalingMessageType::kOffer;
    if (s == "answer")        return SignalingMessageType::kAnswer;
    if (s == "ice_candidate") return SignalingMessageType::kIceCandidate;
    if (s == "peer_joined")   return SignalingMessageType::kPeerJoined;
    if (s == "peer_left")     return SignalingMessageType::kPeerLeft;
    if (s == "room_state")    return SignalingMessageType::kRoomState;
    if (s == "heartbeat")     return SignalingMessageType::kHeartbeat;
    if (s == "error")         return SignalingMessageType::kError;
    if (s == "mute")          return SignalingMessageType::kMute;
    return SignalingMessageType::kCustom;
}

std::string SignalingMessage::toJson() const {
    json j;
    j["type"] = msgTypeToString(type);

    if (!room_id.empty())    j["room_id"] = room_id;
    if (!sender_id.empty())  j["sender_id"] = sender_id;
    if (!target_id.empty())  j["target_id"] = target_id;
    if (!sdp.empty())        j["sdp"] = sdp;
    if (!sdp_type.empty())   j["sdp_type"] = sdp_type;
    if (!candidate.empty())  j["candidate"] = candidate;
    if (!sdp_mid.empty())    j["sdp_mid"] = sdp_mid;
    if (sdp_mline_index > 0) j["sdp_mline_index"] = sdp_mline_index;
    if (!payload.empty())    j["payload"] = payload;
    j["timestamp"] = timestamp > 0 ? timestamp :
        std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();

    if (type == SignalingMessageType::kError) {
        j["error_code"] = error_code;
        if (!error_message.empty()) j["error_message"] = error_message;
    }

    return j.dump();
}

SignalingMessage SignalingMessage::fromJson(const std::string& jsonStr) {
    SignalingMessage msg;
    try {
        json j = json::parse(jsonStr);

        msg.type = stringToMsgType(j.value("type", "custom"));
        msg.room_id = j.value("room_id", "");
        msg.sender_id = j.value("sender_id", "");
        msg.target_id = j.value("target_id", "");
        msg.sdp = j.value("sdp", "");
        msg.sdp_type = j.value("sdp_type", "");
        msg.candidate = j.value("candidate", "");
        msg.sdp_mid = j.value("sdp_mid", "");
        msg.sdp_mline_index = j.value("sdp_mline_index", 0);
        msg.payload = j.value("payload", "");
        msg.timestamp = j.value("timestamp", 0LL);
        msg.error_code = j.value("error_code", 0);
        msg.error_message = j.value("error_message", "");

    } catch (const json::exception& e) {
        spdlog::error("SignalingMessage parse error: {}", e.what());
    }
    return msg;
}

// ============================================================
// SignalingClient
// ============================================================

SignalingClient::SignalingClient(const SignalingConfig& config)
    : config_(config) {}

SignalingClient::~SignalingClient() {
    disconnect();
}

bool SignalingClient::connect() {
    if (connected_) return true;

    if (config_.verbose_logging) {
        spdlog::info("SignalingClient: connecting to {}", config_.server_url);
    }

    if (!openWebSocket()) {
        spdlog::error("SignalingClient: failed to open WebSocket to {}", config_.server_url);
        if (on_error_cb_) on_error_cb_(1, "WebSocket connection failed");
        return false;
    }

    connected_ = true;
    running_ = true;
    reconnect_attempts_ = 0;

    // Start IO thread
    io_thread_ = std::thread(&SignalingClient::ioLoop, this);

    // Start heartbeat thread
    heartbeat_thread_ = std::thread(&SignalingClient::heartbeatLoop, this);

    if (on_connection_cb_) on_connection_cb_(true);

    spdlog::info("SignalingClient: connected to {}", config_.server_url);
    return true;
}

void SignalingClient::disconnect() {
    if (!connected_ && !running_) return;

    running_ = false;
    connected_ = false;

    closeWebSocket();

    if (io_thread_.joinable()) io_thread_.join();
    if (heartbeat_thread_.joinable()) heartbeat_thread_.join();

    if (on_connection_cb_) on_connection_cb_(false);

    spdlog::info("SignalingClient: disconnected");
}

bool SignalingClient::isConnected() const {
    return connected_.load();
}

std::string SignalingClient::getPeerId() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return assigned_peer_id_.empty() ? config_.peer_id : assigned_peer_id_;
}

// ============================================================
// Room Management
// ============================================================

bool SignalingClient::joinRoom(const std::string& room_id) {
    if (!connected_) return false;

    SignalingMessage msg;
    msg.type = SignalingMessageType::kJoin;
    msg.room_id = room_id;
    msg.sender_id = getPeerId();
    msg.payload = json{{"display_name", config_.display_name}}.dump();

    sendMessage(msg);

    {
        std::lock_guard<std::mutex> lock(mutex_);
        current_room_id_ = room_id;
    }

    spdlog::info("SignalingClient: joining room '{}'", room_id);
    return true;
}

void SignalingClient::leaveRoom() {
    if (!connected_) return;

    SignalingMessage msg;
    msg.type = SignalingMessageType::kLeave;
    msg.room_id = current_room_id_;
    msg.sender_id = getPeerId();

    sendMessage(msg);

    {
        std::lock_guard<std::mutex> lock(mutex_);
        current_room_id_.clear();
        room_peers_.clear();
    }
}

std::string SignalingClient::getRoomId() const {
    std::lock_guard<std::mutex> lock(mutex_);
    return current_room_id_;
}

std::vector<RoomPeerInfo> SignalingClient::getRoomPeers() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::vector<RoomPeerInfo> peers;
    peers.reserve(room_peers_.size());
    for (const auto& [id, info] : room_peers_) {
        peers.push_back(info);
    }
    return peers;
}

// ============================================================
// SDP & ICE Exchange
// ============================================================

void SignalingClient::sendOffer(const std::string& target_peer, const std::string& sdp) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kOffer;
    msg.target_id = target_peer;
    msg.sdp = sdp;
    msg.sdp_type = "offer";
    sendMessage(msg);
}

void SignalingClient::sendAnswer(const std::string& target_peer, const std::string& sdp) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kAnswer;
    msg.target_id = target_peer;
    msg.sdp = sdp;
    msg.sdp_type = "answer";
    sendMessage(msg);
}

void SignalingClient::sendIceCandidate(const std::string& target_peer,
                                        const std::string& candidate,
                                        const std::string& sdp_mid,
                                        int sdp_mline_index) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kIceCandidate;
    msg.target_id = target_peer;
    msg.candidate = candidate;
    msg.sdp_mid = sdp_mid;
    msg.sdp_mline_index = sdp_mline_index;
    sendMessage(msg);
}

void SignalingClient::setAudioMuted(bool muted) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kMute;
    msg.payload = json{{"audio", muted}}.dump();
    sendMessage(msg);
}

void SignalingClient::setVideoMuted(bool muted) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kMute;
    msg.payload = json{{"video", muted}}.dump();
    sendMessage(msg);
}

void SignalingClient::sendCustomMessage(const std::string& target_peer, const std::string& json_payload) {
    SignalingMessage msg;
    msg.type = SignalingMessageType::kCustom;
    msg.target_id = target_peer;
    msg.payload = json_payload;
    sendMessage(msg);
}

// ============================================================
// Callbacks
// ============================================================

void SignalingClient::onOffer(MessageCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_offer_cb_ = std::move(cb);
}

void SignalingClient::onAnswer(MessageCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_answer_cb_ = std::move(cb);
}

void SignalingClient::onIceCandidate(MessageCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_ice_candidate_cb_ = std::move(cb);
}

void SignalingClient::onPeerJoined(PeerCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_peer_joined_cb_ = std::move(cb);
}

void SignalingClient::onPeerLeft(PeerCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_peer_left_cb_ = std::move(cb);
}

void SignalingClient::onRoomState(MessageCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_room_state_cb_ = std::move(cb);
}

void SignalingClient::onConnectionStateChange(StateCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_connection_cb_ = std::move(cb);
}

void SignalingClient::onError(ErrorCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_error_cb_ = std::move(cb);
}

void SignalingClient::onCustomMessage(MessageCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    on_custom_cb_ = std::move(cb);
}

// ============================================================
// Stats
// ============================================================

SignalingClient::SignalingStats SignalingClient::getStats() const {
    std::lock_guard<std::mutex> lock(stats_mutex_);
    return stats_;
}

// ============================================================
// Internal: Message dispatch
// ============================================================

void SignalingClient::handleMessage(const std::string& jsonStr) {
    auto msg = SignalingMessage::fromJson(jsonStr);

    {
        std::lock_guard<std::mutex> lock(stats_mutex_);
        stats_.messages_received++;
        stats_.bytes_received += jsonStr.size();
    }

    switch (msg.type) {
        case SignalingMessageType::kOffer:
            if (on_offer_cb_) on_offer_cb_(msg);
            break;

        case SignalingMessageType::kAnswer:
            if (on_answer_cb_) on_answer_cb_(msg);
            break;

        case SignalingMessageType::kIceCandidate:
            if (on_ice_candidate_cb_) on_ice_candidate_cb_(msg);
            break;

        case SignalingMessageType::kPeerJoined: {
            RoomPeerInfo info;
            info.peer_id = msg.sender_id;
            info.joined_at = std::chrono::steady_clock::now();

            // Parse metadata from payload
            try {
                json p = json::parse(msg.payload);
                info.display_name = p.value("display_name", msg.sender_id);
            } catch (...) {
                info.display_name = msg.sender_id;
            }

            {
                std::lock_guard<std::mutex> lock(mutex_);
                room_peers_[msg.sender_id] = info;
            }

            if (on_peer_joined_cb_) on_peer_joined_cb_(msg.sender_id, info);
            break;
        }

        case SignalingMessageType::kPeerLeft: {
            {
                std::lock_guard<std::mutex> lock(mutex_);
                room_peers_.erase(msg.sender_id);
            }
            if (on_peer_left_cb_) {
                RoomPeerInfo info;
                info.peer_id = msg.sender_id;
                on_peer_left_cb_(msg.sender_id, info);
            }
            break;
        }

        case SignalingMessageType::kRoomState:
            if (on_room_state_cb_) on_room_state_cb_(msg);

            // Update assigned peer ID
            if (!msg.target_id.empty()) {
                std::lock_guard<std::mutex> lock(mutex_);
                assigned_peer_id_ = msg.target_id;
            }
            break;

        case SignalingMessageType::kHeartbeat: {
            last_heartbeat_ = std::chrono::steady_clock::now();
            std::lock_guard<std::mutex> lock(stats_mutex_);
            stats_.last_heartbeat_ms =
                std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::steady_clock::now().time_since_epoch()).count();

            // Calculate RTT from server timestamp
            if (msg.timestamp > 0) {
                int64_t now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::system_clock::now().time_since_epoch()).count();
                stats_.round_trip_ms = static_cast<uint64_t>(now_ms - msg.timestamp);
            }
            break;
        }

        case SignalingMessageType::kError:
            spdlog::warn("SignalingClient: server error [{}] {}", msg.error_code, msg.error_message);
            if (on_error_cb_) on_error_cb_(msg.error_code, msg.error_message);
            break;

        case SignalingMessageType::kCustom:
            if (on_custom_cb_) on_custom_cb_(msg);
            break;

        default:
            break;
    }
}

void SignalingClient::sendMessage(const SignalingMessage& msg) {
    if (!connected_) return;

    std::string jsonStr = msg.toJson();
    if (!sendWebSocketFrame(jsonStr)) {
        spdlog::warn("SignalingClient: failed to send message");
        return;
    }

    {
        std::lock_guard<std::mutex> lock(stats_mutex_);
        stats_.messages_sent++;
        stats_.bytes_sent += jsonStr.size();
    }
}

// ============================================================
// Internal: WebSocket (stub using platform socket)
// ============================================================

bool SignalingClient::openWebSocket() {
    // In production, this would use libwebsockets or platform-native WebSocket API
    // For now, provide the integration point:
    //
    // libwebsockets:
    //   lws_create_context() → lws_client_connect_via_info() → callback protocol
    //
    // Platform APIs:
    //   iOS/macOS: NSURLSessionWebSocketTask
    //   Android: OkHttp WebSocket
    //   Windows: WinHTTP WebSocket
    //
    // The ws_handle_ stores the platform-specific connection handle

    if (config_.verbose_logging) {
        spdlog::info("SignalingClient: WebSocket stub — integration with libwebsockets pending");
    }

    // Placeholder: mark connection as established
    ws_handle_ = reinterpret_cast<void*>(1); // non-null sentinel
    return true;
}

void SignalingClient::closeWebSocket() {
    if (ws_handle_) {
        // Close platform-specific WebSocket connection
        ws_handle_ = nullptr;
    }
}

bool SignalingClient::sendWebSocketFrame(const std::string& data) {
    if (!ws_handle_) return false;

    // Platform-specific send:
    // libwebsockets: lws_write() with LWS_WRITE_TEXT
    // iOS: [webSocketTask sendMessage:...]
    // etc.

    return true;
}

std::string SignalingClient::receiveWebSocketFrame() {
    // Platform-specific receive with timeout
    // Returns empty string if no data available
    return "";
}

// ============================================================
// Internal: IO Loop
// ============================================================

void SignalingClient::ioLoop() {
    spdlog::debug("SignalingClient: IO loop started");

    while (running_) {
        // Read messages from WebSocket
        std::string data = receiveWebSocketFrame();
        if (!data.empty()) {
            handleMessage(data);
        }

        // Sleep to avoid busy-waiting (platform APIs typically use callbacks instead)
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    spdlog::debug("SignalingClient: IO loop stopped");
}

void SignalingClient::heartbeatLoop() {
    spdlog::debug("SignalingClient: heartbeat loop started");

    while (running_) {
        std::this_thread::sleep_for(std::chrono::seconds(config_.heartbeat_interval_sec));

        if (!connected_) continue;

        SignalingMessage msg;
        msg.type = SignalingMessageType::kHeartbeat;
        sendMessage(msg);

        // Check heartbeat timeout
        auto now = std::chrono::steady_clock::now();
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(
            now - last_heartbeat_).count();

        if (elapsed > static_cast<int64_t>(config_.heartbeat_interval_sec * 3)) {
            spdlog::warn("SignalingClient: heartbeat timeout, reconnecting...");
            connected_ = false;
            scheduleReconnect();
        }
    }
}

void SignalingClient::scheduleReconnect() {
    if (!config_.auto_reconnect) return;

    if (config_.max_reconnect_attempts > 0 &&
        reconnect_attempts_ >= config_.max_reconnect_attempts) {
        spdlog::error("SignalingClient: max reconnect attempts ({}) reached",
                      config_.max_reconnect_attempts);
        if (on_error_cb_) on_error_cb_(2, "Max reconnect attempts reached");
        return;
    }

    reconnect_attempts_++;
    {
        std::lock_guard<std::mutex> lock(stats_mutex_);
        stats_.reconnect_count++;
    }

    spdlog::info("SignalingClient: reconnecting in {}s (attempt {}/{})",
                 config_.reconnect_delay_sec, reconnect_attempts_,
                 config_.max_reconnect_attempts);

    std::this_thread::sleep_for(std::chrono::seconds(config_.reconnect_delay_sec));

    if (running_) {
        closeWebSocket();
        connect();
    }
}

// ============================================================
// AudioVideoStreamManager
// ============================================================

AudioVideoStreamManager::AudioVideoStreamManager(void* peerConnectionFactory)
    : factory_(peerConnectionFactory) {}

AudioVideoStreamManager::~AudioVideoStreamManager() {
    shutdown();
}

void AudioVideoStreamManager::shutdown() {
    std::lock_guard<std::mutex> lock(mutex_);
    audio_tracks_.clear();
    video_tracks_.clear();
    audio_configs_.clear();
    video_configs_.clear();
}

// ============================================================
// Audio Track Management
// ============================================================

std::string AudioVideoStreamManager::createAudioTrack(const AudioStreamConfig& config) {
    std::lock_guard<std::mutex> lock(mutex_);

    // Generate unique track ID
    std::string track_id = "audio_" + std::to_string(audio_tracks_.size() + 1);

    // In production, this creates a libwebrtc AudioTrack:
    // auto audioSource = factory->CreateAudioSource(cricket::AudioOptions{});
    // auto track = factory->CreateAudioTrack(track_id, audioSource);

    audio_configs_[track_id] = config;
    audio_tracks_[track_id] = reinterpret_cast<void*>(1); // placeholder

    spdlog::info("AudioVideoStreamManager: created audio track '{}' ({}Hz, {}ch, {}kbps)",
                 track_id, config.sample_rate, config.channels, config.bitrate_bps / 1000);
    return track_id;
}

bool AudioVideoStreamManager::addAudioTrack(void* peerConnection, const std::string& track_id,
                                              StreamDirection direction) {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = audio_tracks_.find(track_id);
    if (it == audio_tracks_.end()) {
        spdlog::error("AudioVideoStreamManager: audio track '{}' not found", track_id);
        return false;
    }

    // In production:
    // auto pc = static_cast<webrtc::PeerConnectionInterface*>(peerConnection);
    // pc->AddTrack(track, {"stream_id"});
    // Set direction via RtpTransceiver

    spdlog::info("AudioVideoStreamManager: added audio track '{}' to peer connection", track_id);
    return true;
}

void AudioVideoStreamManager::removeAudioTrack(const std::string& track_id) {
    std::lock_guard<std::mutex> lock(mutex_);
    audio_tracks_.erase(track_id);
    audio_configs_.erase(track_id);
}

void AudioVideoStreamManager::pushAudioData(const std::string& track_id,
                                              const int16_t* samples, uint32_t sample_count) {
    // In production: feed PCM samples to the audio source for encoding
    // audioSource->OnData(samples, 16, sample_rate, channels, sample_count);
    (void)track_id;
    (void)samples;
    (void)sample_count;
}

bool AudioVideoStreamManager::pullAudioData(const std::string& track_id,
                                              int16_t* buffer, uint32_t max_samples,
                                              uint32_t* out_count) {
    // In production: read decoded PCM from the audio track's sink
    (void)track_id;
    (void)buffer;
    (void)max_samples;
    if (out_count) *out_count = 0;
    return false;
}

bool AudioVideoStreamManager::isAudioActive(const std::string& track_id) const {
    std::lock_guard<std::mutex> lock(mutex_);
    return audio_tracks_.find(track_id) != audio_tracks_.end();
}

// ============================================================
// Video Track Management
// ============================================================

std::string AudioVideoStreamManager::createVideoTrack(const VideoStreamConfig& config) {
    std::lock_guard<std::mutex> lock(mutex_);

    std::string track_id = "video_" + std::to_string(video_tracks_.size() + 1);

    // In production:
    // auto videoSource = factory->CreateVideoSource(...);
    // auto track = factory->CreateVideoTrack(track_id, videoSource);

    video_configs_[track_id] = config;
    video_tracks_[track_id] = reinterpret_cast<void*>(1); // placeholder

    spdlog::info("AudioVideoStreamManager: created video track '{}' ({}x{}@{}fps, {}kbps)",
                 track_id, config.width, config.height, config.fps, config.bitrate_bps / 1000);
    return track_id;
}

bool AudioVideoStreamManager::addVideoTrack(void* peerConnection, const std::string& track_id,
                                              StreamDirection direction) {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = video_tracks_.find(track_id);
    if (it == video_tracks_.end()) {
        spdlog::error("AudioVideoStreamManager: video track '{}' not found", track_id);
        return false;
    }

    spdlog::info("AudioVideoStreamManager: added video track '{}' to peer connection", track_id);
    return true;
}

void AudioVideoStreamManager::removeVideoTrack(const std::string& track_id) {
    std::lock_guard<std::mutex> lock(mutex_);
    video_tracks_.erase(track_id);
    video_configs_.erase(track_id);
}

void AudioVideoStreamManager::pushVideoFrame(const std::string& track_id,
                                               const uint8_t* rgba_data,
                                               uint32_t width, uint32_t height) {
    // In production: feed RGBA frame to video source for encoding
    (void)track_id;
    (void)rgba_data;
    (void)width;
    (void)height;
}

bool AudioVideoStreamManager::pullVideoFrame(const std::string& track_id,
                                               uint8_t* buffer, uint32_t buffer_size,
                                               uint32_t* out_width, uint32_t* out_height) {
    (void)track_id;
    (void)buffer;
    (void)buffer_size;
    if (out_width) *out_width = 0;
    if (out_height) *out_height = 0;
    return false;
}

bool AudioVideoStreamManager::isVideoActive(const std::string& track_id) const {
    std::lock_guard<std::mutex> lock(mutex_);
    return video_tracks_.find(track_id) != video_tracks_.end();
}

// ============================================================
// Codec Control
// ============================================================

void AudioVideoStreamManager::setAudioCodec(const std::string& codec) {
    spdlog::info("AudioVideoStreamManager: preferred audio codec set to '{}'", codec);
}

void AudioVideoStreamManager::setVideoCodec(const std::string& codec) {
    spdlog::info("AudioVideoStreamManager: preferred video codec set to '{}'", codec);
}

std::string AudioVideoStreamManager::getNegotiatedCodec(const std::string& track_id) const {
    (void)track_id;
    return "opus"; // default
}

// ============================================================
// Bitrate Control
// ============================================================

void AudioVideoStreamManager::setAudioBitrate(const std::string& track_id, uint32_t bitrate_bps) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = audio_configs_.find(track_id);
    if (it != audio_configs_.end()) {
        it->second.bitrate_bps = bitrate_bps;
    }
}

void AudioVideoStreamManager::setVideoBitrate(const std::string& track_id, uint32_t bitrate_bps) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = video_configs_.find(track_id);
    if (it != video_configs_.end()) {
        it->second.bitrate_bps = bitrate_bps;
    }
}

// ============================================================
// Stats
// ============================================================

AudioVideoStreamManager::StreamStats AudioVideoStreamManager::getStats() const {
    std::lock_guard<std::mutex> lock(mutex_);
    StreamStats stats;
    stats.audio_tracks = static_cast<uint32_t>(audio_tracks_.size());
    stats.video_tracks = static_cast<uint32_t>(video_tracks_.size());
    return stats;
}

} // namespace solra::webrtc
