/*
 * Solra Core SDK - On-device inference engine implementation
 *
 * llama.cpp integration for lightweight LLM inference.
 * Supports quantized models with NPU/GPU acceleration.
 */

#include <solra/solra_inference.h>
#include <solra/solra_types.h>
#include "llama_integration.hpp"
#include <spdlog/spdlog.h>
#include <string>
#include <atomic>
#include <memory>
#include <mutex>
#include <thread>
#include <cstring>

using namespace solra::core::inference;

static struct {
  SolraInferenceConfig config;
  int initialized = 0;

  std::unique_ptr<LlamaEngine> engine;
  std::mutex engine_mutex;

  std::atomic<int> generating{0};
  std::atomic<int> stream_cancel{0};

  // Streaming generation thread
  std::thread stream_thread;
  std::mutex stream_mutex;
} g_inference;

/* ============================================================
 * Engine Lifecycle
 * ============================================================ */

int solra_inference_init(const SolraInferenceConfig *config) {
  if (g_inference.initialized) return SOLRA_ERROR_ALREADY_INITIALIZED;
  if (!config) return SOLRA_ERROR_INVALID_ARGUMENT;

  g_inference.config = *config;

  spdlog::info("Inference engine initializing...");
  spdlog::info("  Model: {}", config->model_path ? config->model_path : "not specified");
  spdlog::info("  Backend: {}", (int)config->backend);
  spdlog::info("  Context size: {}", config->context_size);
  spdlog::info("  GPU layers: {}", config->gpu_layers);

  // Create and configure LlamaEngine
  g_inference.engine = std::make_unique<LlamaEngine>();

  if (config->model_path && config->model_path[0] != '\0') {
    LlamaModelConfig modelCfg;
    modelCfg.model_path = config->model_path;
    modelCfg.context_size = config->context_size > 0
        ? static_cast<uint32_t>(config->context_size) : 4096;
    modelCfg.cpu_threads = config->cpu_threads > 0 ? config->cpu_threads : 4;
    modelCfg.gpu_layers = config->gpu_layers;
    modelCfg.batch_size = config->batch_size > 0
        ? static_cast<uint32_t>(config->batch_size) : 512;
    modelCfg.ubatch_size = modelCfg.batch_size;
    modelCfg.use_mmap = true;
    modelCfg.use_flash_attn = true;

    // Select quantization based on config
    modelCfg.quant_format = QuantFormat::kQ4_K_M; // default

    if (!g_inference.engine->LoadModel(modelCfg)) {
      spdlog::warn("Failed to load model '{}', engine will run in stub mode",
                   config->model_path);
    } else {
      spdlog::info("Model loaded successfully: {} ({} MB)",
                   config->model_path,
                   g_inference.engine->GetModelSize() / (1024 * 1024));
    }
  }

  g_inference.initialized = 1;
  return SOLRA_OK;
}

int solra_inference_is_backend_available(SolraInferenceBackend backend) {
  switch (backend) {
    case SOLRA_INFERENCE_BACKEND_CPU:
      return 1;
#ifdef __APPLE__
    case SOLRA_INFERENCE_BACKEND_METAL_GPU:
    case SOLRA_INFERENCE_BACKEND_COREML:
      return 1;
#endif
#ifdef __ANDROID__
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
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;

  std::memset(info, 0, sizeof(SolraModelInfo));

  if (g_inference.engine && g_inference.engine->IsLoaded()) {
    std::strncpy(info->name,
                 g_inference.config.model_path ? g_inference.config.model_path : "unknown",
                 sizeof(info->name) - 1);
    info->format = SOLRA_MODEL_FORMAT_GGUF;
    info->model_size_bytes = g_inference.engine->GetModelSize();
    info->context_length = g_inference.engine->GetContextSize();
  } else {
    std::strncpy(info->name, "(no model loaded)", sizeof(info->name) - 1);
    info->format = SOLRA_MODEL_FORMAT_GGUF;
    info->model_size_bytes = 0;
    info->context_length = 4096;
  }

  return SOLRA_OK;
}

/* ============================================================
 * Inference Pipeline
 * ============================================================ */

int solra_inference_generate(const char *prompt, char *response, size_t response_size) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!prompt || !response) return SOLRA_ERROR_INVALID_ARGUMENT;

  spdlog::debug("Inference: generate (sync), prompt length: {}", strlen(prompt));

  std::lock_guard<std::mutex> lock(g_inference.engine_mutex);

  if (g_inference.engine && g_inference.engine->IsLoaded()) {
    // Use the LlamaEngine for real inference
    LlamaSamplingParams params;
    params.temperature = g_inference.config.temperature > 0
        ? g_inference.config.temperature : 0.7f;
    params.top_p = g_inference.config.top_p > 0
        ? g_inference.config.top_p : 0.9f;
    params.repetition_penalty = g_inference.config.repeat_penalty > 0
        ? g_inference.config.repeat_penalty : 1.1f;
    params.max_tokens = g_inference.config.max_tokens > 0
        ? static_cast<uint32_t>(g_inference.config.max_tokens) : 256;

    auto result = g_inference.engine->Generate(prompt, params);

    size_t len = result.text.size();
    if (len >= response_size) len = response_size - 1;
    std::memcpy(response, result.text.c_str(), len);
    response[len] = '\0';

    spdlog::info("Inference generated {} tokens in {:.0f}ms ({:.1f} tok/s)",
                 result.tokens_generated, result.duration_ms, result.tokens_per_second);
    return static_cast<int>(len);
  }

  // Stub mode: return a placeholder response
  const char *stub = "[Solra Inference] No model loaded. Place a .gguf model file and reload.";
  size_t len = std::strlen(stub);
  if (len >= response_size) len = response_size - 1;
  std::memcpy(response, stub, len);
  response[len] = '\0';
  return static_cast<int>(len);
}

int solra_inference_generate_stream(
  const char *prompt, SolraTokenCallback callback, void *user_data
) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!prompt || !callback) return SOLRA_ERROR_INVALID_ARGUMENT;

  g_inference.generating.store(1);
  g_inference.stream_cancel.store(0);

  spdlog::debug("Inference: generate (stream), prompt length: {}", strlen(prompt));

  // Run generation in background thread
  {
    std::lock_guard<std::mutex> lock(g_inference.stream_mutex);
    if (g_inference.stream_thread.joinable()) {
      g_inference.stream_thread.join();
    }

    g_inference.stream_thread = std::thread([prompt_str = std::string(prompt),
                                              cb = callback, ud = user_data]() {
      std::lock_guard<std::mutex> engine_lock(g_inference.engine_mutex);

      if (g_inference.engine && g_inference.engine->IsLoaded()) {
        LlamaSamplingParams params;
        params.temperature = g_inference.config.temperature > 0
            ? g_inference.config.temperature : 0.7f;
        params.top_p = g_inference.config.top_p > 0
            ? g_inference.config.top_p : 0.9f;
        params.max_tokens = g_inference.config.max_tokens > 0
            ? static_cast<uint32_t>(g_inference.config.max_tokens) : 256;

        g_inference.engine->GenerateStream(
            prompt_str,
            [cb, ud](const std::string& text, uint32_t token_id) {
              if (g_inference.stream_cancel.load()) return;
              SolraToken token;
              token.token_id = static_cast<int>(token_id);
              std::strncpy(token.text, text.c_str(), sizeof(token.text) - 1);
              token.text[sizeof(token.text) - 1] = '\0';
              token.is_special = 0;
              token.log_prob = 0.0f;
              cb(&token, 0, ud);
            },
            params);
      } else {
        // Stub streaming
        SolraToken token;
        token.token_id = 0;
        token.is_special = 0;
        token.log_prob = 0.0f;

        const char* stub_parts[] = {
          "[Solra]", " Inference", " Engine", " - ", "no", " model", " loaded", "." };
        for (const auto* part : stub_parts) {
          if (g_inference.stream_cancel.load()) break;
          std::strncpy(token.text, part, sizeof(token.text) - 1);
          cb(&token, 0, ud);
          std::this_thread::sleep_for(std::chrono::milliseconds(80));
        }
      }

      // Final token
      if (!g_inference.stream_cancel.load()) {
        SolraToken final_token;
        final_token.token_id = -1;
        std::strncpy(final_token.text, "", sizeof(final_token.text) - 1);
        final_token.is_special = 1;
        final_token.log_prob = 0.0f;
        cb(&final_token, 1, ud);
      }

      g_inference.generating.store(0);
    });
  }

  return SOLRA_OK;
}

void solra_inference_cancel_stream(void) {
  g_inference.stream_cancel.store(1);
  std::lock_guard<std::mutex> lock(g_inference.stream_mutex);
  if (g_inference.stream_thread.joinable()) {
    g_inference.stream_thread.join();
  }
}

int solra_inference_is_generating(void) {
  return g_inference.generating.load();
}

/* ============================================================
 * Model Hot-reload
 * ============================================================ */

int solra_inference_reload_model(const char *model_path) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;

  spdlog::info("Inference: reloading model from '{}'", model_path);

  std::lock_guard<std::mutex> lock(g_inference.engine_mutex);

  if (g_inference.engine) {
    g_inference.engine->UnloadModel();

    if (model_path && model_path[0] != '\0') {
      LlamaModelConfig modelCfg;
      modelCfg.model_path = model_path;
      modelCfg.context_size = static_cast<uint32_t>(g_inference.config.context_size > 0
          ? g_inference.config.context_size : 4096);
      modelCfg.cpu_threads = g_inference.config.cpu_threads > 0
          ? g_inference.config.cpu_threads : 4;
      modelCfg.gpu_layers = g_inference.config.gpu_layers;
      modelCfg.use_mmap = true;

      if (!g_inference.engine->LoadModel(modelCfg)) {
        spdlog::error("Failed to reload model: {}", model_path);
        return SOLRA_ERROR_UNKNOWN;
      }

      spdlog::info("Model reloaded: {} ({} MB)",
                   model_path,
                   g_inference.engine->GetModelSize() / (1024 * 1024));
    }
  }

  return SOLRA_OK;
}

void solra_inference_set_update_callback(SolraModelUpdateCallback callback, void *user_data) {
  (void)callback;
  (void)user_data;
  // TODO: Integrate with model_ota.hpp OTA system
  spdlog::debug("Inference: model update callback registered");
}

/* ============================================================
 * Tokenization API
 * ============================================================ */

int solra_inference_tokenize(const char *text, int *tokens_out, int max_tokens) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!text || !tokens_out || max_tokens <= 0) return SOLRA_ERROR_INVALID_ARGUMENT;

  std::lock_guard<std::mutex> lock(g_inference.engine_mutex);

  if (!g_inference.engine || !g_inference.engine->IsLoaded()) {
    // Stub mode: return simple character-based token IDs
    spdlog::debug("Inference: tokenize in stub mode");
    int count = 0;
    const char *p = text;
    while (*p && count < max_tokens) {
      tokens_out[count++] = static_cast<int>(static_cast<unsigned char>(*p));
      ++p;
    }
    return count;
  }

  auto tokens = g_inference.engine->Tokenize(text);
  int count = static_cast<int>(std::min(tokens.size(), static_cast<size_t>(max_tokens)));
  for (int i = 0; i < count; ++i) {
    tokens_out[i] = static_cast<int>(tokens[i]);
  }

  spdlog::debug("Inference: tokenized {} chars → {} tokens", strlen(text), count);
  return count;
}

int solra_inference_detokenize(const int *tokens, int token_count,
                                char *text_out, size_t text_size) {
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;
  if (!tokens || token_count <= 0 || !text_out || text_size == 0)
    return SOLRA_ERROR_INVALID_ARGUMENT;

  std::lock_guard<std::mutex> lock(g_inference.engine_mutex);

  if (!g_inference.engine || !g_inference.engine->IsLoaded()) {
    // Stub mode: cast token IDs back to characters
    spdlog::debug("Inference: detokenize in stub mode");
    size_t count = static_cast<size_t>(std::min(token_count, static_cast<int>(text_size - 1)));
    for (size_t i = 0; i < count; ++i) {
      text_out[i] = static_cast<char>(tokens[i] & 0xFF);
    }
    text_out[count] = '\0';
    return static_cast<int>(count);
  }

  std::vector<uint32_t> tks;
  tks.reserve(static_cast<size_t>(token_count));
  for (int i = 0; i < token_count; ++i) {
    tks.push_back(static_cast<uint32_t>(tokens[i]));
  }

  auto result = g_inference.engine->Detokenize(tks);
  size_t len = std::min(result.size(), text_size - 1);
  std::memcpy(text_out, result.c_str(), len);
  text_out[len] = '\0';

  return static_cast<int>(len);
}

int solra_inference_get_vocab_size(void) {
  if (!g_inference.initialized) return 0;
  if (g_inference.engine && g_inference.engine->IsLoaded()) {
    return static_cast<int>(g_inference.engine->GetContextSize());
  }
  return 0;
}

/* ============================================================
 * Performance Statistics
 * ============================================================ */

int solra_inference_get_stats(SolraInferenceStats *stats) {
  if (!stats) return SOLRA_ERROR_INVALID_ARGUMENT;
  if (!g_inference.initialized) return SOLRA_ERROR_NOT_INITIALIZED;

  std::memset(stats, 0, sizeof(SolraInferenceStats));

  if (g_inference.engine) {
    stats->current_memory_bytes = g_inference.engine->GetMemoryUsage();
  }

  return SOLRA_OK;
}

/* ============================================================
 * Shutdown
 * ============================================================ */

void solra_inference_shutdown(void) {
  solra_inference_cancel_stream();

  {
    std::lock_guard<std::mutex> lock(g_inference.engine_mutex);
    if (g_inference.engine) {
      g_inference.engine->UnloadModel();
      g_inference.engine.reset();
    }
  }

  g_inference.initialized = 0;
  spdlog::info("Inference engine shutdown");
}
