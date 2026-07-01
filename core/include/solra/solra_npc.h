/*
 * Solra Core SDK - On-device NPC Dialogue API
 *
 * Personality-driven NPC conversation system using local LLM inference.
 * Supports emotion model, affection system, behavior trees, and expression output.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_NPC_H
#define SOLRA_NPC_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * NPC Personality Types
 * ============================================================ */

typedef enum SolraNpcPreset {
  SOLRA_NPC_FRIENDLY_COMPANION = 0,
  SOLRA_NPC_WISE_MENTOR        = 1,
  SOLRA_NPC_MYSTERIOUS_STRANGER = 2,
  SOLRA_NPC_CHEERFUL_ASSISTANT  = 3,
  SOLRA_NPC_STOIC_GUARDIAN      = 4,
  SOLRA_NPC_CUSTOM              = 99,
} SolraNpcPreset;

typedef struct SolraNpcConfig {
  /** NPC unique identifier */
  const char *npc_id;
  /** Display name (null = use preset default) */
  const char *display_name;
  /** Preset personality type */
  SolraNpcPreset preset;
  /** Custom background story (for SOLRA_NPC_CUSTOM, null-terminated) */
  const char *background_story;
  /** Custom speaking tone (null = default) */
  const char *tone;
  /** Custom personality traits, semicolon-separated (null = default) */
  const char *traits;
} SolraNpcConfig;

/* ============================================================
 * NPC Emotion
 * ============================================================ */

typedef struct SolraNpcEmotion {
  float joy;          /** 愉悦 [0,1] */
  float curiosity;    /** 好奇 [0,1] */
  float coldness;     /** 冷漠 [0,1] */
  float jealousy;     /** 嫉妒 [0,1] */
  float sadness;      /** 悲伤 [0,1] */
  char dominant[32];  /** 主导情绪名称 */
} SolraNpcEmotion;

/* ============================================================
 * NPC Affection
 * ============================================================ */

typedef enum SolraAffectionLevel {
  SOLRA_AFFECTION_L1_STRANGER   = 1,
  SOLRA_AFFECTION_L2_ACQUAINTED = 2,
  SOLRA_AFFECTION_L3_FAMILIAR   = 3,
  SOLRA_AFFECTION_L4_FRIENDLY   = 4,
  SOLRA_AFFECTION_L5_CLOSE      = 5,
  SOLRA_AFFECTION_L6_TRUSTED    = 6,
  SOLRA_AFFECTION_L7_INTIMATE   = 7,
  SOLRA_AFFECTION_L8_DEVOTED    = 8,
  SOLRA_AFFECTION_L9_SOULMATE   = 9,
  SOLRA_AFFECTION_L10_BONDED    = 10,
} SolraAffectionLevel;

typedef struct SolraNpcAffection {
  float score;                       /** 好感度分数 [0, 1000] */
  SolraAffectionLevel level;         /** 好感度等级 */
  uint64_t interaction_count;        /** 交互次数 */
} SolraNpcAffection;

/* ============================================================
 * NPC Response
 * ============================================================ */

typedef struct SolraNpcResponse {
  char text[4096];               /** NPC 回复文本 */
  char expression[64];           /** 推荐表情名称 */
  char gesture[64];              /** 推荐手势名称 */
  char emotion_label[32];        /** 情感标签 */
  float emotion_intensity;       /** 情感强度 [0,1] */
  uint32_t tokens_generated;     /** 生成 token 数 */
  double ttft_ms;                /** Time To First Token (ms) */
  double total_duration_ms;      /** 总耗时 (ms) */
  double tokens_per_second;      /** 吞吐 (tok/s) */
} SolraNpcResponse;

/* ============================================================
 * NPC Stream Callback
 * ============================================================ */

typedef void (*SolraNpcStreamCallback)(
    const char *token_text, uint32_t token_id, int is_final, void *user_data);

/* ============================================================
 * NPC Lifecycle
 * ============================================================ */

/**
 * Create a new NPC with the given configuration.
 *
 * @param config NPC configuration.
 * @return 0 on success, negative error code on failure.
 */
SOLRA_API int solra_npc_create(const SolraNpcConfig *config);

/**
 * Remove an NPC and free its resources.
 *
 * @param npc_id NPC identifier.
 * @return 0 on success.
 */
SOLRA_API int solra_npc_remove(const char *npc_id);

/**
 * Check if an NPC exists.
 *
 * @param npc_id NPC identifier.
 * @return 1 if exists, 0 if not.
 */
SOLRA_API int solra_npc_exists(const char *npc_id);

/**
 * Get the number of active NPCs.
 *
 * @return NPC count.
 */
SOLRA_API int solra_npc_count(void);

/* ============================================================
 * NPC Dialogue
 * ============================================================ */

/**
 * Send a message to an NPC and get a synchronous response.
 *
 * @param npc_id NPC identifier.
 * @param user_message User's message text (null-terminated).
 * @param response Non-null pointer to receive NPC response.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_npc_send_message(const char *npc_id,
                                      const char *user_message,
                                      SolraNpcResponse *response);

/**
 * Send a message to an NPC and get a streaming response.
 *
 * @param npc_id NPC identifier.
 * @param user_message User's message text.
 * @param callback Function called for each generated token.
 * @param user_data Opaque pointer passed to callback.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_npc_send_message_stream(const char *npc_id,
                                              const char *user_message,
                                              SolraNpcStreamCallback callback,
                                              void *user_data);

/**
 * Cancel an ongoing streaming NPC dialogue.
 */
SOLRA_API void solra_npc_cancel_stream(void);

/* ============================================================
 * NPC State
 * ============================================================ */

/**
 * Get the current emotion state of an NPC.
 *
 * @param npc_id NPC identifier.
 * @param emotion Non-null pointer to receive emotion state.
 * @return 0 on success.
 */
SOLRA_API int solra_npc_get_emotion(const char *npc_id,
                                     SolraNpcEmotion *emotion);

/**
 * Update NPC emotion with an event.
 *
 * @param npc_id NPC identifier.
 * @param event_type Event type (e.g. "compliment", "insult", "surprise").
 * @param intensity Event intensity [0,1].
 * @return 0 on success.
 */
SOLRA_API int solra_npc_update_emotion(const char *npc_id,
                                         const char *event_type,
                                         float intensity);

/**
 * Get the current affection state of an NPC.
 *
 * @param npc_id NPC identifier.
 * @param affection Non-null pointer to receive affection state.
 * @return 0 on success.
 */
SOLRA_API int solra_npc_get_affection(const char *npc_id,
                                        SolraNpcAffection *affection);

/**
 * Set the NPC's current location (for context-aware dialogue).
 *
 * @param npc_id NPC identifier.
 * @param location Location description string.
 * @return 0 on success.
 */
SOLRA_API int solra_npc_set_location(const char *npc_id,
                                       const char *location);

/**
 * Set the NPC's current time of day (for context-aware dialogue).
 *
 * @param npc_id NPC identifier.
 * @param time_of_day Time description (e.g. "morning", "evening").
 * @return 0 on success.
 */
SOLRA_API int solra_npc_set_time_of_day(const char *npc_id,
                                          const char *time_of_day);

/**
 * Advance NPC simulation by delta seconds (emotion decay, affection decay).
 *
 * @param delta_seconds Time elapsed in seconds.
 */
SOLRA_API void solra_npc_tick(double delta_seconds);

/**
 * Clear dialogue history for an NPC.
 *
 * @param npc_id NPC identifier.
 * @return 0 on success.
 */
SOLRA_API int solra_npc_clear_history(const char *npc_id);

/**
 * Shutdown the NPC system and free all resources.
 */
SOLRA_API void solra_npc_shutdown(void);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_NPC_H */
