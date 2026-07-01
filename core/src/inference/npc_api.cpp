/*
 * Solra Core SDK - NPC Dialogue C API Bridge
 *
 * Bridges the C API (solra_npc.h) to the C++ NpcDialoguePipeline.
 */

#include <solra/solra_npc.h>
#include <solra/solra_types.h>
#include "npc_dialogue.hpp"

#include <spdlog/spdlog.h>
#include <cstring>

using namespace solra::core::inference;

// ============================================================================
// NPC Create
// ============================================================================

int solra_npc_create(const SolraNpcConfig *config) {
  if (!config || !config->npc_id || config->npc_id[0] == '\0') {
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  NpcPersonality personality;
  switch (config->preset) {
    case SOLRA_NPC_FRIENDLY_COMPANION:
      personality = NpcPersonality::FriendlyCompanion();
      break;
    case SOLRA_NPC_WISE_MENTOR:
      personality = NpcPersonality::WiseMentor();
      break;
    case SOLRA_NPC_MYSTERIOUS_STRANGER:
      personality = NpcPersonality::MysteriousStranger();
      break;
    case SOLRA_NPC_CHEERFUL_ASSISTANT:
      personality = NpcPersonality::CheerfulAssistant();
      break;
    case SOLRA_NPC_STOIC_GUARDIAN:
      personality = NpcPersonality::StoicGuardian();
      break;
    case SOLRA_NPC_CUSTOM:
      {
        std::vector<std::string> traits;
        if (config->traits && config->traits[0]) {
          std::string traits_str(config->traits);
          size_t pos = 0;
          while (pos < traits_str.size()) {
            size_t semi = traits_str.find(';', pos);
            std::string trait = traits_str.substr(pos, semi - pos);
            if (!trait.empty()) traits.push_back(trait);
            if (semi == std::string::npos) break;
            pos = semi + 1;
          }
        }
        personality = NpcPersonality::Custom(
            config->npc_id,
            config->background_story ? config->background_story : "",
            config->tone ? config->tone : "neutral",
            traits);
      }
      break;
    default:
      personality = NpcPersonality::FriendlyCompanion();
      break;
  }

  // 覆盖显示名
  if (config->display_name && config->display_name[0]) {
    personality.display_name = config->display_name;
  }

  auto* ctx = NpcDialoguePipeline::Instance().CreateNpc(config->npc_id, personality);
  return ctx ? SOLRA_OK : SOLRA_ERROR_UNKNOWN;
}

int solra_npc_remove(const char *npc_id) {
  if (!npc_id) return SOLRA_ERROR_INVALID_ARGUMENT;
  NpcDialoguePipeline::Instance().RemoveNpc(npc_id);
  return SOLRA_OK;
}

int solra_npc_exists(const char *npc_id) {
  if (!npc_id) return 0;
  return NpcDialoguePipeline::Instance().GetNpc(npc_id) ? 1 : 0;
}

int solra_npc_count(void) {
  return static_cast<int>(NpcDialoguePipeline::Instance().ListNpcs().size());
}

// ============================================================================
// NPC Dialogue
// ============================================================================

int solra_npc_send_message(const char *npc_id, const char *user_message,
                             SolraNpcResponse *response) {
  if (!npc_id || !user_message || !response) return SOLRA_ERROR_INVALID_ARGUMENT;

  auto result = NpcDialoguePipeline::Instance().SendMessage(npc_id, user_message);

  std::memset(response, 0, sizeof(SolraNpcResponse));
  std::strncpy(response->text, result.text.c_str(), sizeof(response->text) - 1);
  std::strncpy(response->expression, result.expression.c_str(), sizeof(response->expression) - 1);
  std::strncpy(response->gesture, result.gesture.c_str(), sizeof(response->gesture) - 1);
  std::strncpy(response->emotion_label, result.emotion_label.c_str(), sizeof(response->emotion_label) - 1);
  response->emotion_intensity = result.emotion_intensity;
  response->tokens_generated = result.tokens_generated;
  response->ttft_ms = result.ttft_ms;
  response->total_duration_ms = result.total_duration_ms;
  response->tokens_per_second = result.tokens_per_second;

  return SOLRA_OK;
}

int solra_npc_send_message_stream(const char *npc_id, const char *user_message,
                                    SolraNpcStreamCallback callback, void *user_data) {
  if (!npc_id || !user_message || !callback) return SOLRA_ERROR_INVALID_ARGUMENT;

  NpcDialoguePipeline::Instance().SendMessageStream(
      npc_id, user_message,
      [callback, user_data](const std::string& text, uint32_t token_id, int is_final) {
        callback(text.c_str(), token_id, is_final, user_data);
      });

  return SOLRA_OK;
}

void solra_npc_cancel_stream(void) {
  NpcDialoguePipeline::Instance().CancelStream();
}

// ============================================================================
// NPC State
// ============================================================================

int solra_npc_get_emotion(const char *npc_id, SolraNpcEmotion *emotion) {
  if (!npc_id || !emotion) return SOLRA_ERROR_INVALID_ARGUMENT;

  auto* ctx = NpcDialoguePipeline::Instance().GetNpc(npc_id);
  if (!ctx) return SOLRA_ERROR_NOT_INITIALIZED;

  std::memset(emotion, 0, sizeof(SolraNpcEmotion));
  emotion->joy = ctx->emotion.joy;
  emotion->curiosity = ctx->emotion.curiosity;
  emotion->coldness = ctx->emotion.coldness;
  emotion->jealousy = ctx->emotion.jealousy;
  emotion->sadness = ctx->emotion.sadness;
  std::strncpy(emotion->dominant,
               ctx->emotion.DominantEmotion().c_str(),
               sizeof(emotion->dominant) - 1);

  return SOLRA_OK;
}

int solra_npc_update_emotion(const char *npc_id, const char *event_type,
                               float intensity) {
  if (!npc_id || !event_type) return SOLRA_ERROR_INVALID_ARGUMENT;
  NpcDialoguePipeline::Instance().UpdateEmotion(npc_id, event_type, intensity);
  return SOLRA_OK;
}

int solra_npc_get_affection(const char *npc_id, SolraNpcAffection *affection) {
  if (!npc_id || !affection) return SOLRA_ERROR_INVALID_ARGUMENT;

  auto* ctx = NpcDialoguePipeline::Instance().GetNpc(npc_id);
  if (!ctx) return SOLRA_ERROR_NOT_INITIALIZED;

  std::memset(affection, 0, sizeof(SolraNpcAffection));
  affection->score = ctx->affection.score;
  affection->level = static_cast<SolraAffectionLevel>(
      static_cast<uint8_t>(ctx->affection.level));
  affection->interaction_count = ctx->affection.interaction_count;

  return SOLRA_OK;
}

int solra_npc_set_location(const char *npc_id, const char *location) {
  if (!npc_id || !location) return SOLRA_ERROR_INVALID_ARGUMENT;
  auto* ctx = NpcDialoguePipeline::Instance().GetNpc(npc_id);
  if (!ctx) return SOLRA_ERROR_NOT_INITIALIZED;
  ctx->current_location = location;
  return SOLRA_OK;
}

int solra_npc_set_time_of_day(const char *npc_id, const char *time_of_day) {
  if (!npc_id || !time_of_day) return SOLRA_ERROR_INVALID_ARGUMENT;
  auto* ctx = NpcDialoguePipeline::Instance().GetNpc(npc_id);
  if (!ctx) return SOLRA_ERROR_NOT_INITIALIZED;
  ctx->current_time_of_day = time_of_day;
  return SOLRA_OK;
}

void solra_npc_tick(double delta_seconds) {
  NpcDialoguePipeline::Instance().Tick(delta_seconds);
}

int solra_npc_clear_history(const char *npc_id) {
  if (!npc_id) return SOLRA_ERROR_INVALID_ARGUMENT;
  NpcDialoguePipeline::Instance().ClearHistory(npc_id);
  return SOLRA_OK;
}

void solra_npc_shutdown(void) {
  // 清空所有 NPC
  auto npcs = NpcDialoguePipeline::Instance().ListNpcs();
  for (const auto& id : npcs) {
    NpcDialoguePipeline::Instance().RemoveNpc(id);
  }
  spdlog::info("NPC system shutdown");
}
