/*
 * Solra Core SDK - On-device inference engine implementation (stub)
 *
 * llama.cpp integration for lightweight LLM inference.
 */

#include <solra/solra_inference.h>
#include <solra/solra_types.h>
#include <spdlog/spdlog.h>
#include <string>
#include <atomic>

static struct {
  SolraInferenceConfig config;
  int initialized = 0;
  std::atomic<int> generating{0};
  std::atomic<int> stream_cancel{0};
} g_inference;

/* ============================================================
 * Engine Lifecycle
 * ============================================================ */

int solra_inference_init(const SolraInferenceConfig *config) {
  if (g_inference.initialized) return SOLRA_ERROR_ALREADY_INITIALIZED;
  if (!config) return SOLRA_ERROR_INVALID_ARGUMENT;

  g_inference.config = *config;

  spdlog::info("Inference engine initialized");
  spdlog::info("  Model: {}", config->model_path ? config->model_path : "not specified");
  spdlog::info("  Backend: {}", (int)config->backend);
  spdlog::info("  Context size: {}", config->context_size);
  spdlog::info("  GPU layers: {}", config->gpu_layers);

  /* TODO: Initialize llama.cpp context with model */

  g_inference.initialized = 1;
  return SOLRA_OK;
}

int solra_inference_is_backend_available(SolraInferenceBackend backend) {
  switch (backend) {
    case SOLRA_INFERENCE_BACKEND_CPU:
      return 1;
#ifdef SOLRA_PLATFORM_APPLE
    case SOLRA_INFERENCE_BACKEND_METAL_GPU:
    case SOLRA_INFERENCE_BACKEND_COREML:
      return 1;
#endif
#ifdef SOLRA_PLATFORM_ANDROID
    case SOLRA_INFERENCE_BACKEND_VULKAN:
    case SOLRA_INFERENCE_BACKEND_NNAPI:
      return 1;
#endif
    default:
      return 0;
  }
}

SolraInferenceBackend solra_inference_get_best_backend(void) {
  if (solra_inference_is_backend_available(SOLRA_INFERENCE_BACKEND_COREML))
    return SOLRA_INFERENCE_BACKEND_COREML;
  if (solra_inference_is_backend_available(SOLRA_INFERENCE_BACKEND_METAL_GPU))
    return SOLRA_INFERENCE_BACKEND_METAL_GPU;
  if (solra_inference_is_backend_available(SOLRA_INFERENCE_BACKEND_NNAPI))
    return SOLRA_INFERENCE_BACKEND_NNAPI;
  if (solra_inference_is_backend_available(SOLRA_INFERENCE_BACKEND_VULKAN))
    return SOLRA_INFERENCE_BACKEND_VULKAN;
  return SOLRA_INFERENCE_BACKEND_CPU;
}

int solra_inference_get_model_info(SolraModelInfo *info) {
  if (!info) return SOLRA_ERROR_INVALID_ARGUMENT;
  /* TODO: Populate from loaded model */
  return SOLRA_OK;
}

/* ============================================================
 * Inference Pipeline
 * ============================================================ */

int solra_inference_generate(const char *prompt, char *response, size_t response_size) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!prompt || !response) return SOLRA_ERROR_INVALID_ARGUMENT;

  spdlog::debug("Inference: generate (sync), prompt length: {}", strlen(prompt));

  /* TODO: Run llama.cpp inference */
  const char *stub_response = "[Solra Inference Stub] Response not available - llama.cpp not loaded";
  size_t len = strlen(stub_response);
  if (len >= response_size) len = response_size - 1;
  memcpy(response, stub_response, len);
  response[len] = '\0';
  return (int)len;
}

int solra_inference_generate_stream(
  const char *prompt, SolraTokenCallback callback, void *user_data
) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!prompt || !callback) return SOLRA_ERROR_INVALID_ARGUMENT;

  g_inference.generating.store(1);
  g_inference.stream_cancel.store(0);

  spdlog::debug("Inference: generate (stream)");

  /* TODO: Run streaming llama.cpp inference with callback per token */

  g_inference.generating.store(0);
  return SOLRA_OK;
}

void solra_inference_cancel_stream(void) {
  g_inference.stream_cancel.store(1);
}

int solra_inference_is_generating(void) {
  return g_inference.generating.load();
}

/* ============================================================
 * Model Hot-reload
 * ============================================================ */

int solra_inference_reload_model(const char *model_path) {
  spdlog::info("Inference: reloading model from '{}'", model_path);
  /* TODO: Unload current model, load new one atomically */
  return SOLRA_OK;
}

void solra_inference_set_update_callback(SolraModelUpdateCallback callback, void *user_data) {
  /* TODO: Store callback */
}

/* ============================================================
 * Shutdown
 * ============================================================ */

void solra_inference_shutdown(void) {
  solra_inference_cancel_stream();
  g_inference.initialized = 0;
  spdlog::info("Inference engine shutdown");
}
