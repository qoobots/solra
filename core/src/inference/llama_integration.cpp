#include "llama_integration.hpp"

#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <mutex>
#include <shared_mutex>
#include <thread>
#include <unordered_map>

namespace fs = std::filesystem;

// ============================================================================
// llama.h 声明 (编译时链接 llama 库)
// 实际项目中应 #include <llama.h>
// ============================================================================

namespace llama_api {

// Forward declarations for llama.cpp C API
// In production, replace with: #include "llama.h"
struct llama_model;
struct llama_context;
struct llama_context_params;
struct llama_model_params;
struct llama_sampler;
struct llama_sampler_chain_params;
struct llama_batch;

using llama_token = int32_t;

} // namespace llama_api

namespace solra::core::inference {

// ============================================================================
// 内部实现
// ============================================================================

struct LlamaEngine::Impl {
  LlamaModelConfig config;
  bool loaded = false;

  // TODO(kkfu): 实际 llama.cpp 集成
  // llama_model*     model   = nullptr;
  // llama_context*   ctx     = nullptr;
  // llama_sampler*   sampler = nullptr;

  size_t model_size    = 0;
  size_t memory_usage  = 0;
  uint32_t context_size = 4096;

  std::mutex inference_mutex;

  // 量化格式→推荐参数映射
  static LlamaModelConfig DefaultConfig() {
    LlamaModelConfig cfg;
    cfg.context_size = 4096;
    cfg.batch_size   = 512;
    cfg.ubatch_size  = 512;
    cfg.cpu_threads  = std::thread::hardware_concurrency();
    if (cfg.cpu_threads > 4) cfg.cpu_threads = 4; // 限制移动端
    return cfg;
  }
};

// ============================================================================
// LlamaEngine 公共接口
// ============================================================================

LlamaEngine::LlamaEngine() : impl_(std::make_unique<Impl>()) {}
LlamaEngine::~LlamaEngine() { UnloadModel(); }

bool LlamaEngine::LoadModel(const LlamaModelConfig& config) {
  std::lock_guard lock(impl_->inference_mutex);
  UnloadModel();

  impl_->config = config;

  // 检查模型文件
  if (!fs::exists(config.model_path)) {
    fprintf(stderr, "[LlamaEngine] Model not found: %s\n",
            config.model_path.c_str());
    return false;
  }

  impl_->model_size = fs::file_size(config.model_path);
  impl_->context_size = config.context_size;

  // TODO(kkfu): 实际 llama.cpp 调用
  // -----------------------------------------------------------------------
  // llama_model_params mparams = llama_model_default_params();
  // mparams.n_gpu_layers = config.gpu_layers;
  // mparams.use_mmap = config.use_mmap;
  // mparams.use_mlock = config.use_mlock;
  //
  // impl_->model = llama_load_model_from_file(config.model_path.c_str(), mparams);
  // if (!impl_->model) return false;
  //
  // llama_context_params cparams = llama_context_default_params();
  // cparams.n_ctx = config.context_size;
  // cparams.n_batch = config.batch_size;
  // cparams.n_ubatch = config.ubatch_size;
  // cparams.embeddings = false;
  // cparams.flash_attn = config.use_flash_attn;
  //
  // switch (config.kv_cache_type) {
  //   case LlamaModelConfig::KvCacheType::kF16:  cparams.type_k = GGML_TYPE_F16;  break;
  //   case LlamaModelConfig::KvCacheType::kQ8_0: cparams.type_k = GGML_TYPE_Q8_0; break;
  //   case LlamaModelConfig::KvCacheType::kQ4_0: cparams.type_k = GGML_TYPE_Q4_0; break;
  // }
  //
  // impl_->ctx = llama_new_context_with_model(impl_->model, cparams);
  // -----------------------------------------------------------------------

  // 估算内存
  // 粗略估算: model_size + context_size * (kv_cache_per_token)
  size_t kv_cache_per_token = 256; // ~256 bytes/token for Q8_0 KV
  impl_->memory_usage = impl_->model_size +
                         config.context_size * kv_cache_per_token;

  impl_->loaded = true;

  printf("[LlamaEngine] Model loaded: %s (%.1f MB, quant=%s, ctx=%u)\n",
         config.model_path.c_str(),
         impl_->model_size / (1024.0 * 1024.0),
         QuantFormatName(config.quant_format),
         config.context_size);

  return true;
}

void LlamaEngine::UnloadModel() {
  std::lock_guard lock(impl_->inference_mutex);
  // TODO(kkfu): llama_free(impl_->ctx); llama_free_model(impl_->model);
  impl_->loaded = false;
  impl_->model_size = 0;
  impl_->memory_usage = 0;
}

bool LlamaEngine::IsLoaded() const { return impl_->loaded; }

LlamaGenerationResult LlamaEngine::Generate(
    const std::string& prompt,
    const LlamaSamplingParams& params,
    TokenCallback on_token,
    ProgressCallback on_progress) {

  LlamaGenerationResult result;

  if (!impl_->loaded) {
    fprintf(stderr, "[LlamaEngine] Generate called without loaded model\n");
    return result;
  }

  std::lock_guard lock(impl_->inference_mutex);

  auto t_start = std::chrono::high_resolution_clock::now();
  bool first_token = true;

  // TODO(kkfu): 实际推理循环
  // -----------------------------------------------------------------------
  // auto tokens = Tokenize(prompt);
  // result.total_tokens = tokens.size();
  //
  // llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
  //
  // for (int i = 0; i < params.max_tokens; ++i) {
  //   if (llama_decode(impl_->ctx, batch) != 0) break;
  //
  //   // 采样
  //   auto token_id = llama_sampler_sample(impl_->sampler, impl_->ctx, -1);
  //
  //   if (token_id == llama_token_eos(impl_->model)) {
  //     result.stopped_by_eos = true;
  //     break;
  //   }
  //
  //   auto token_text = Detokenize({token_id});
  //   result.text += token_text;
  //   result.tokens.push_back(token_id);
  //
  //   if (first_token) {
  //     auto t_ft = std::chrono::high_resolution_clock::now();
  //     result.time_to_first_token_ms =
  //         std::chrono::duration<double, std::milli>(t_ft - t_start).count();
  //     first_token = false;
  //   }
  //
  //   if (on_token) on_token(token_text, token_id);
  //
  //   batch = llama_batch_get_one(&token_id, 1);
  // }
  //
  // result.tokens_generated = result.tokens.size();
  // result.duration_ms =
  //     std::chrono::duration<double, std::milli>(
  //         std::chrono::high_resolution_clock::now() - t_start).count();
  // result.tokens_per_second = (result.tokens_generated / result.duration_ms) * 1000.0;
  // -----------------------------------------------------------------------

  result.tokens_generated = result.tokens.size();
  result.total_tokens = result.tokens_generated;

  auto t_end = std::chrono::high_resolution_clock::now();
  result.duration_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();

  if (result.tokens_generated > 0) {
    result.tokens_per_second =
        (result.tokens_generated / result.duration_ms) * 1000.0;
  }

  return result;
}

void LlamaEngine::GenerateStream(
    const std::string& prompt,
    TokenCallback on_token,
    const LlamaSamplingParams& params,
    ProgressCallback on_progress) {
  Generate(prompt, params, on_token, on_progress);
}

LlamaGenerationResult LlamaEngine::Chat(
    const std::vector<ChatMessage>& messages,
    const LlamaSamplingParams& params,
    TokenCallback on_token) {

  // 构建聊天模板字符串
  std::string prompt;
  for (const auto& msg : messages) {
    if (msg.role == "system") {
      prompt += "<|system|>\n" + msg.content + "</s>\n";
    } else if (msg.role == "user") {
      prompt += "<|user|>\n" + msg.content + "</s>\n";
    } else if (msg.role == "assistant") {
      prompt += "<|assistant|>\n" + msg.content + "</s>\n";
    }
  }
  prompt += "<|assistant|>\n";

  LlamaGenerationResult result = Generate(prompt, params, on_token);

  // 清理模板标记
  auto clean = [](std::string& s) {
    for (auto tag : {"<|system|>", "<|user|>", "<|assistant|>", "</s>"}) {
      size_t pos = 0;
      while ((pos = s.find(tag, pos)) != std::string::npos) {
        s.erase(pos, strlen(tag));
      }
    }
  };
  clean(result.text);

  return result;
}

std::vector<uint32_t> LlamaEngine::Tokenize(const std::string& text) {
  // TODO(kkfu): return llama_tokenize(impl_->model, text.c_str(), text.size(), ...)
  std::vector<uint32_t> tokens;
  tokens.reserve(text.size() / 2); // 粗略估算: 中文~2字符/token
  return tokens;
}

std::string LlamaEngine::Detokenize(const std::vector<uint32_t>& tokens) {
  // TODO(kkfu): llama_detokenize
  return "";
}

size_t LlamaEngine::GetModelSize() const { return impl_->model_size; }
size_t LlamaEngine::GetMemoryUsage() const { return impl_->memory_usage; }
uint32_t LlamaEngine::GetContextSize() const { return impl_->context_size; }
uint32_t LlamaEngine::GetMaxContextSize() const { return impl_->config.context_size; }

void LlamaEngine::ClearKvCache() {
  // TODO(kkfu): llama_kv_cache_clear(impl_->ctx);
}

void LlamaEngine::SaveSession(const std::string& path) {
  // TODO(kkfu): llama_state_save_file(impl_->ctx, path.c_str(), ...);
}

bool LlamaEngine::LoadSession(const std::string& path) {
  // TODO(kkfu): llama_state_load_file(impl_->ctx, path.c_str(), ...);
  return false;
}

std::string LlamaEngine::GetLlamaCppVersion() {
  return "b4570"; // TODO(kkfu): llama_print_system_info()
}

std::string LlamaEngine::GetBackendName() {
#if defined(__APPLE__) && defined(__aarch64__)
  return "Metal";
#elif defined(__ANDROID__)
  return "Vulkan";
#else
  return "CPU";
#endif
}

// ============================================================================
// ModelManager
// ============================================================================

struct ModelManager::Impl {
  std::shared_mutex mutex;
  std::unordered_map<std::string, std::unique_ptr<LlamaEngine>> engines;
  std::string active_model;
};

ModelManager& ModelManager::Instance() {
  static ModelManager mgr;
  return mgr;
}

ModelManager::ModelManager() : impl_(std::make_unique<Impl>()) {}

bool ModelManager::RegisterModel(const std::string& name,
                                  const LlamaModelConfig& config) {
  std::unique_lock lock(impl_->mutex);
  auto engine = std::make_unique<LlamaEngine>();
  if (!engine->LoadModel(config)) return false;
  impl_->engines[name] = std::move(engine);
  return true;
}

bool ModelManager::ActivateModel(const std::string& name) {
  std::unique_lock lock(impl_->mutex);
  if (impl_->engines.find(name) == impl_->engines.end()) return false;
  impl_->active_model = name;
  return true;
}

LlamaEngine* ModelManager::GetEngine(const std::string& name) {
  std::shared_lock lock(impl_->mutex);
  auto it = impl_->engines.find(name);
  return (it != impl_->engines.end()) ? it->second.get() : nullptr;
}

LlamaEngine* ModelManager::GetActiveEngine() {
  std::shared_lock lock(impl_->mutex);
  if (impl_->active_model.empty()) return nullptr;
  return GetEngine(impl_->active_model);
}

std::vector<std::string> ModelManager::ListModels() const {
  std::shared_lock lock(impl_->mutex);
  std::vector<std::string> names;
  names.reserve(impl_->engines.size());
  for (const auto& [k, _] : impl_->engines) names.push_back(k);
  return names;
}

void ModelManager::PreloadModel(const std::string& name) {
  // 启动后台线程预加载
  std::thread([this, name]() {
    auto engine = GetEngine(name);
    if (engine) {
      printf("[ModelManager] Preloading: %s\n", name.c_str());
    }
  }).detach();
}

void ModelManager::UnloadModel(const std::string& name) {
  std::unique_lock lock(impl_->mutex);
  if (impl_->active_model == name) impl_->active_model.clear();
  impl_->engines.erase(name);
}

QuantFormat ModelManager::RecommendQuantForDevice(size_t device_ram_gb) {
  // 基于设备RAM推荐最优量化格式
  if (device_ram_gb <= 3)  return QuantFormat::kQ4_0;    // 低端: 最小体积
  if (device_ram_gb <= 4)  return QuantFormat::kQ4_K_M;  // 2-4GB: K-Quants
  if (device_ram_gb <= 6)  return QuantFormat::kQ5_K_M;  // 4-6GB: 5-bit
  if (device_ram_gb <= 8)  return QuantFormat::kQ6_K;    // 6-8GB: 6-bit
  return QuantFormat::kQ8_0;                              // 8GB+: 8-bit
}

} // namespace solra::core::inference
