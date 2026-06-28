/*
 * Solra Core SDK - WebRTC API
 *
 * P2P data channel integration using libwebrtc for low-latency
 * state synchronization and audio streaming.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_WEBRTC_H
#define SOLRA_WEBRTC_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * WebRTC Configuration
 * ============================================================ */

typedef struct SolraWebRTCConfig {
  /** STUN server URL (e.g. "stun:stun.l.google.com:19302") */
  const char *stun_server;
  /** TURN server URL (for NAT traversal fallback) */
  const char *turn_server;
  /** TURN server username */
  const char *turn_username;
  /** TURN server credential */
  const char *turn_credential;
  /** Enable spatial audio processing */
  int enable_spatial_audio;
  /** Max data channel bandwidth in kbps */
  int max_bandwidth_kbps;
} SolraWebRTCConfig;

/* ============================================================
 * Connection State
 * ============================================================ */

typedef enum SolraWebRTCState {
  SOLRA_WEBRTC_STATE_DISCONNECTED = 0,
  SOLRA_WEBRTC_STATE_CONNECTING = 1,
  SOLRA_WEBRTC_STATE_CONNECTED = 2,
  SOLRA_WEBRTC_STATE_FAILED = 3,
  SOLRA_WEBRTC_STATE_CLOSED = 4,
} SolraWebRTCState;

/** Opaque handle to a WebRTC peer connection */
typedef struct SolraPeerConnection *SolraPeerConnectionHandle;

/** Opaque handle to a data channel */
typedef struct SolraDataChannel *SolraDataChannelHandle;

/* ============================================================
 * Callbacks
 * ============================================================ */

/**
 * Connection state change callback.
 */
typedef void (*SolraWebRTCStateCallback)(SolraWebRTCState state, void *user_data);

/**
 * Data channel message received callback.
 *
 * @param data Received data buffer.
 * @param size Size of data in bytes.
 * @param is_binary 1 if binary, 0 if text.
 * @param user_data Opaque user data.
 */
typedef void (*SolraWebRTCMessageCallback)(const void *data, int size, int is_binary, void *user_data);

/* ============================================================
 * Engine Lifecycle
 * ============================================================ */

/**
 * Initialize the WebRTC subsystem.
 *
 * @param config WebRTC configuration.
 * @return 0 on success, negative on error.
 */
int solra_webrtc_init(const SolraWebRTCConfig *config);

/**
 * Shutdown the WebRTC subsystem.
 */
void solra_webrtc_shutdown(void);

/* ============================================================
 * Peer Connection Management
 * ============================================================ */

/**
 * Create a peer connection.
 *
 * @return Peer connection handle, or NULL on failure.
 */
SolraPeerConnectionHandle solra_webrtc_peer_create(void);

/**
 * Set state change callback for a peer connection.
 */
void solra_webrtc_peer_set_state_callback(
  SolraPeerConnectionHandle peer,
  SolraWebRTCStateCallback callback,
  void *user_data
);

/**
 * Create an offer SDP.
 *
 * @param peer Peer connection.
 * @param sdp Non-null buffer to receive SDP string.
 * @param sdp_size Size of sdp buffer.
 * @return SDP string length, or negative on error.
 */
int solra_webrtc_peer_create_offer(SolraPeerConnectionHandle peer, char *sdp, size_t sdp_size);

/**
 * Set local description from SDP.
 *
 * @param peer Peer connection.
 * @param sdp SDP string.
 * @param type "offer" or "answer".
 * @return 0 on success.
 */
int solra_webrtc_peer_set_local_description(SolraPeerConnectionHandle peer, const char *sdp, const char *type);

/**
 * Set remote description from SDP.
 *
 * @param peer Peer connection.
 * @param sdp SDP string.
 * @param type "offer" or "answer".
 * @return 0 on success.
 */
int solra_webrtc_peer_set_remote_description(SolraPeerConnectionHandle peer, const char *sdp, const char *type);

/**
 * Add an ICE candidate.
 *
 * @param peer Peer connection.
 * @param candidate ICE candidate string.
 * @param sdp_mid SDP media stream ID.
 * @param sdp_mline_index SDP media line index.
 */
void solra_webrtc_peer_add_ice_candidate(
  SolraPeerConnectionHandle peer,
  const char *candidate,
  const char *sdp_mid,
  int sdp_mline_index
);

/**
 * Close and destroy a peer connection.
 */
void solra_webrtc_peer_destroy(SolraPeerConnectionHandle peer);

/* ============================================================
 * Data Channel Management
 * ============================================================ */

/**
 * Create a data channel on a peer connection.
 *
 * @param peer Peer connection.
 * @param label Channel label/name.
 * @param ordered Whether to preserve message order.
 * @return Data channel handle, or NULL on failure.
 */
SolraDataChannelHandle solra_webrtc_channel_create(
  SolraPeerConnectionHandle peer,
  const char *label,
  int ordered
);

/**
 * Set message callback for a data channel.
 */
void solra_webrtc_channel_set_message_callback(
  SolraDataChannelHandle channel,
  SolraWebRTCMessageCallback callback,
  void *user_data
);

/**
 * Send a message on a data channel.
 *
 * @param channel Data channel.
 * @param data Data to send.
 * @param size Size of data in bytes.
 * @param is_binary 1 if binary, 0 if text (UTF-8).
 * @return 0 on success, negative on error.
 */
int solra_webrtc_channel_send(SolraDataChannelHandle channel, const void *data, int size, int is_binary);

/**
 * Get the connection state of a data channel.
 */
SolraWebRTCState solra_webrtc_channel_get_state(SolraDataChannelHandle channel);

/**
 * Close a data channel.
 */
void solra_webrtc_channel_close(SolraDataChannelHandle channel);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_WEBRTC_H */
