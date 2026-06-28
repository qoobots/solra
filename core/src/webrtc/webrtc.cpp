/*
 * Solra Core SDK - WebRTC integration (stub)
 */

#include <solra/solra_webrtc.h>
#include <solra/solra_types.h>
#include <spdlog/spdlog.h>

static struct {
  SolraWebRTCConfig config;
  int initialized = 0;
} g_webrtc;

int solra_webrtc_init(const SolraWebRTCConfig *config) {
  if (g_webrtc.initialized) return SOLRA_ERROR_ALREADY_INITIALIZED;
  if (!config) return SOLRA_ERROR_INVALID_ARGUMENT;

  g_webrtc.config = *config;
  g_webrtc.initialized = 1;

  spdlog::info("WebRTC engine initialized");
  spdlog::info("  STUN: {}", config->stun_server ? config->stun_server : "none");
  spdlog::info("  Spatial audio: {}", config->enable_spatial_audio ? "enabled" : "disabled");
  return SOLRA_OK;
}

void solra_webrtc_shutdown(void) {
  g_webrtc.initialized = 0;
  spdlog::info("WebRTC engine shutdown");
}

SolraPeerConnectionHandle solra_webrtc_peer_create(void) {
  return nullptr;
}
void solra_webrtc_peer_set_state_callback(SolraPeerConnectionHandle, SolraWebRTCStateCallback, void *) {}
int solra_webrtc_peer_create_offer(SolraPeerConnectionHandle, char *sdp, size_t sdp_size) { return 0; }
int solra_webrtc_peer_set_local_description(SolraPeerConnectionHandle, const char *, const char *) { return 0; }
int solra_webrtc_peer_set_remote_description(SolraPeerConnectionHandle, const char *, const char *) { return 0; }
void solra_webrtc_peer_add_ice_candidate(SolraPeerConnectionHandle, const char *, const char *, int) {}
void solra_webrtc_peer_destroy(SolraPeerConnectionHandle) {}

SolraDataChannelHandle solra_webrtc_channel_create(SolraPeerConnectionHandle, const char *, int) { return nullptr; }
void solra_webrtc_channel_set_message_callback(SolraDataChannelHandle, SolraWebRTCMessageCallback, void *) {}
int solra_webrtc_channel_send(SolraDataChannelHandle, const void *, int, int) { return 0; }
SolraWebRTCState solra_webrtc_channel_get_state(SolraDataChannelHandle) { return SOLRA_WEBRTC_STATE_DISCONNECTED; }
void solra_webrtc_channel_close(SolraDataChannelHandle) {}
