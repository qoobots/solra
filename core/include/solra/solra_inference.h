/*
 * Solra Core SDK - On-device Inference API
 *
 * Lightweight LLM inference using llama.cpp integration.
 * Supports quantized 1-3B parameter models with NPU acceleration.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_INFERENCE_H
#define SOLRA_INFERENCE_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * Backend Types
 * ============================================================ */

typedef enum SolraInferenceBackend {
  SOLRA_INFERENCE_BACKEND_CPU = 0,
  SOLRA_INFERENCE_BACKEND_METAL_GPU = 1,   /* Apple GPU via Metal */
  SOLRA_INFERENCE_BACKEND_COREML = 2,      /* Apple Neural Engine */
  SOLRA_INFERENCE_BACKEND_VULKAN = 3,      /* Android GPU via Vulkan */
  SOLRA_INFERENCE_BACKEND_NNAPI = 4,       /* Android NPU */
} SolraInferenceBackend;

/* ============================================================
 * Model Types
 * ============================================================ */

typedef enum SolraModelFormat {
  SOLRA_MODEL_FORMAT_GGUF = 0,  /* llama.cpp GGUF format */
} SolraModelFormat;

typedef enum SolraQuantization {
  SOLRA_QUANT_NONE = 0,   /* fp16/fp32 */
  SOLRA_QUANT_Q4_0 = 1,   /* 4-bit integer, group 0 */
  SOLRA_QUANT_Q4_1 = 2,   /* 4-bit integer, group 1 */
  SOLRA_QUANT_Q5_0 = 3,   /* 5-bit integer */
  SOLRA_QUANT_Q8_0 = 4,   /* 8-bit integer */
} SolraQuantization;

/* ============================================================
 * Model Info
 * ============================================================ */

typedef struct SolraModelInfo {
  char name[256];
  char architecture[128];
  SolraModelFormat format;
  SolraQuantization quantization;
  int parameter_count_billions; /* e.g. 1.5 for 1.5B */
  size_t model_size_bytes;
  size_t context_length;
  int embedding_dim;
  int num_layers;
  int vocab_size;
} SolraModelInfo;

/* ============================================================
 * Inference Engine Configuration
 * ============================================================ */

typedef struct SolraInferenceConfig {
  /** Preferred backend (0 = best available) */
  SolraInferenceBackend backend;
  /** Path to GGUF model file */
  const char *model_path;
  /** Context size (number of tokens, 0 = model default) */
  int context_size;
  /** Number of CPU threads for CPU backend */
  int cpu_threads;
  /** GPU layers to offload (-1 = all, 0 = none) */
  int gpu_layers;
  /** Batch size for prompt processing */
  int batch_size;
  /** Maximum tokens to generate per response */
  int max_tokens;
  /** Temperature for sampling (0.0-2.0) */
  float temperature;
  /** Top-p sampling threshold */
  float top_p;
  /** Repeat penalty (>1.0 = penalize repetition) */
  float repeat_penalty;
} SolraInferenceConfig;

/* ============================================================
 * Token and Stream Types
 * ============================================================ */

typedef struct SolraToken {
  int token_id;
  char text[16];      /* Decoded text (UTF-8) */
  int is_special;     /* Whether this is a special token */
  float log_prob;     /* Log probability */
} SolraToken;

/**
 * Streaming callback for token generation.
 *
 * @param token The newly generated token.
 * @param is_final 1 if this is the last token.
 * @param user_data Opaque user data pointer.
 */
typedef void (*SolraTokenCallback)(const SolraToken *token, int is_final, void *user_data);

/* ============================================================
 * Inference Engine Lifecycle
 * ============================================================ */

/**
 * Initialize the inference engine.
 *
 * @param config Inference configuration.
 * @return 0 on success, negative error code on failure.
 */
SOLRA_API int solra_inference_init(const SolraInferenceConfig *config);

/**
 * Check if a GPU/NPU backend is available on this device.
 *
 * @param backend The backend to check.
 * @return 1 if available, 0 if not.
 */
SOLRA_API int solra_inference_is_backend_available(SolraInferenceBackend backend);

/**
 * Get the best available backend for this device.
 *
 * @return Best available backend.
 */
SOLRA_API SolraInferenceBackend solra_inference_get_best_backend(void);

/**
 * Get information about the loaded model.
 *
 * @param info Non-null pointer to receive model info.
 * @return 0 on success.
 */
SOLRA_API int solra_inference_get_model_info(SolraModelInfo *info);

/* ============================================================
 * Inference Pipeline
 * ============================================================ */

/**
 * Generate a response from a prompt (synchronous).
 *
 * Blocks until generation is complete.
 *
 * @param prompt Input text prompt (null-terminated).
 * @param response Non-null buffer to receive response text.
 * @param response_size Size of response buffer in bytes.
 * @return Number of bytes written to response (excluding null), or negative on error.
 */
SOLRA_API int solra_inference_generate(const char *prompt, char *response, size_t response_size);

/**
 * Generate a response from a prompt (streaming).
 *
 * Returns immediately. Tokens are delivered via callback from a background thread.
 *
 * @param prompt Input text prompt (null-terminated).
 * @param callback Function called for each generated token.
 * @param user_data Opaque pointer passed to callback.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_inference_generate_stream(
  const char *prompt,
  SolraTokenCallback callback,
  void *user_data
);

/**
 * Cancel an ongoing streaming generation.
 */
SOLRA_API void solra_inference_cancel_stream(void);

/**
 * Check if streaming generation is in progress.
 *
 * @return 1 if generating, 0 if idle.
 */
SOLRA_API int solra_inference_is_generating(void);

/* ============================================================
 * Model Hot-reload (OTA)
 * ============================================================ */

/**
 * Load/reload a model file at runtime.
 *
 * @param model_path Path to new GGUF model file.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_inference_reload_model(const char *model_path);

/**
 * Register a callback for model update notifications.
 *
 * @param callback Called when a new model version is available.
 * @param user_data Opaque pointer.
 */
typedef void (*SolraModelUpdateCallback)(const char *new_version, const char *changelog, void *user_data);
SOLRA_API void solra_inference_set_update_callback(SolraModelUpdateCallback callback, void *user_data);

/* ============================================================
 * Shutdown
 * ============================================================ */

/**
 * Shutdown the inference engine. Frees all memory and GPU resources.
 */
SOLRA_API void solra_inference_shutdown(void);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_INFERENCE_H */
