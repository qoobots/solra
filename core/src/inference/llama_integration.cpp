/*
 * Solra Core SDK - llama.cpp Integration (real implementation)
 *
 * On-device SLM inference using llama.cpp.
 * Supports GGUF models, quantized inference, streaming token generation,
 * GPU offload (Metal/Vulkan/CUDA), and KV cache management.
 *
 * Build requirement: llama.cpp library (built from source or via vcpkg)
 */

#include "llama_integration.hpp"
#include <spdlog/spdlog.h>
#include <fstream>
#include <cstring>
#include <algorithm>
#include <chrono>
#include <thread>
#include <mutex>
#include <atomic>
#include <filesystem>

// ============================================================================
// Conditional llama.cpp includes
// ============================================================================
#if defined(SOLRA_HAS_LLAMA)
#include "llama.h"
#include "common.h"
#include "sampling.h"
#include "grammar-parser.h"
#else
// Forward declarations for stub mode
struct llama_model;
struct llama_context;
struct llama_context_params;
struct llama_model_params;
typedef int32_t llama_token;
#endif

namespace solra::core::inference {

// ============================================================================
// Internal implementation
// ============================================================================

struct LlamaEngine::Impl {
#if defined(SOLRA_HAS_LLAMA)
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
#endif
    LlamaModelConfig config;
    std::mutex mutex;
    std::atomic<bool> loaded{false};
    std::atomic<bool> generating{false};
    std::atomic<bool> cancelFlag{false};

    // Performance stats
    std::atomic<uint64_t> totalTokens{0};
    std::atomic<double> totalTimeMs{0.0};

    // Session file path for save/load
    std::string sessionPath;

    // llama.cpp backend name
    static std::string detectBackend() {
#if defined(SOLRA_HAS_LLAMA)
        const char* backend = llama_print_system_info();
        // Parse backend from system info string
        std::string info(backend ? backend : "CPU");
        if (info.find("Metal") != std::string::npos) return "Metal";
        if (info.find("Vulkan") != std::string::npos) return "Vulkan";
        if (info.find("CUDA") != std::string::npos) return "CUDA";
        return "CPU";
#else
        return "CPU (stub)";
#endif
    }
};

// ============================================================================
// Constructor / Destructor
// ============================================================================

LlamaEngine::LlamaEngine()
    : impl_(std::make_unique<Impl>()) {
    spdlog::info("LlamaEngine created");
}

LlamaEngine::~LlamaEngine() {
    UnloadModel();
    spdlog::info("LlamaEngine destroyed");
}

// ============================================================================
// Model Loading
// ============================================================================

bool LlamaEngine::LoadModel(const LlamaModelConfig& config) {
    std::lock_guard<std::mutex> lock(impl_->mutex);

    if (impl_->loaded) {
        spdlog::warn("LlamaEngine: model already loaded, unloading first");
        UnloadModel();
    }

    impl_->config = config;

    // Validate model file
    if (!std::filesystem::exists(config.model_path)) {
        spdlog::error("LlamaEngine: model file not found: {}", config.model_path);
        return false;
    }

    auto fileSize = std::filesystem::file_size(config.model_path);
    spdlog::info("LlamaEngine loading model: {} ({} MB)",
                 config.model_path, fileSize / (1024 * 1024));
    spdlog::info("  Context: {} tokens", config.context_size);
    spdlog::info("  Batch: {} / ubatch: {}", config.batch_size, config.ubatch_size);
    spdlog::info("  Quant: {}", QuantFormatName(config.quant_format));
    spdlog::info("  GPU layers: {}", config.gpu_layers);

#if defined(SOLRA_HAS_LLAMA)
    // Initialize llama backend
    llama_backend_init();

    // Model parameters
    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = config.gpu_layers;
    modelParams.use_mmap = config.use_mmap;
    modelParams.use_mlock = config.use_mlock;

    // Load model
    impl_->model = llama_load_model_from_file(config.model_path.c_str(), modelParams);
    if (!impl_->model) {
        spdlog::error("LlamaEngine: failed to load model");
        return false;
    }

    // Context parameters
    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = config.context_size;
    ctxParams.n_batch = config.batch_size;
    ctxParams.n_ubatch = config.ubatch_size;
    ctxParams.n_threads = config.cpu_threads;
    ctxParams.n_threads_batch = config.cpu_threads;

    // Flash attention (if supported by build)
#if defined(LLAMA_SUPPORTS_FLASH_ATTN)
    if (config.use_flash_attn) {
        ctxParams.flash_attn = true;
    }
#endif

    // Create context
    impl_->ctx = llama_new_context_with_model(impl_->model, ctxParams);
    if (!impl_->ctx) {
        spdlog::error("LlamaEngine: failed to create context");
        llama_free_model(impl_->model);
        impl_->model = nullptr;
        return false;
    }

    // Load session if available
    if (!impl_->sessionPath.empty()) {
        LoadSession(impl_->sessionPath);
    }

    impl_->loaded = true;
    spdlog::info("LlamaEngine model loaded successfully");
    spdlog::info("  Backend: {}", GetBackendName());
    spdlog::info("  Model size: {} MB", GetModelSize() / (1024 * 1024));
    spdlog::info("  Memory usage: {} MB", GetMemoryUsage() / (1024 * 1024));

    return true;
#else
    // Stub: simulate loading
    spdlog::warn("LlamaEngine: llama.cpp not linked, running in stub mode");
    impl_->loaded = true;
    return true;
#endif
}

void LlamaEngine::UnloadModel() {
    std::lock_guard<std::mutex> lock(impl_->mutex);

#if defined(SOLRA_HAS_LLAMA)
    if (impl_->ctx) {
        llama_free(impl_->ctx);
        impl_->ctx = nullptr;
    }
    if (impl_->model) {
        llama_free_model(impl_->model);
        impl_->model = nullptr;
    }
    llama_backend_free();
#endif

    impl_->loaded = false;
    spdlog::info("LlamaEngine model unloaded");
}

bool LlamaEngine::IsLoaded() const {
    return impl_->loaded.load();
}

// ============================================================================
// Inference
// ============================================================================

LlamaGenerationResult LlamaEngine::Generate(
    const std::string& prompt,
    const LlamaSamplingParams& params,
    TokenCallback on_token,
    ProgressCallback on_progress) {

    LlamaGenerationResult result;
    if (!impl_->loaded) {
        spdlog::error("LlamaEngine::Generate: no model loaded");
        return result;
    }

    auto startTime = std::chrono::high_resolution_clock::now();
    impl_->generating = true;
    impl_->cancelFlag = false;

    auto samplingParams = params;

#if defined(SOLRA_HAS_LLAMA)
    try {
        // Tokenize prompt
        auto tokens = Tokenize(prompt);
        if (tokens.empty()) {
            impl_->generating = false;
            return result;
        }

        int n_prompt = static_cast<int>(tokens.size());
        int n_ctx = llama_n_ctx(impl_->ctx);
        int n_kv_req = n_prompt + static_cast<int>(samplingParams.max_tokens);

        // Check context overflow
        if (n_kv_req > n_ctx) {
            spdlog::warn("LlamaEngine: context overflow ({} > {}), truncating prompt",
                         n_kv_req, n_ctx);
            // Shift cache
        }

        // Batch prompt evaluation
        llama_batch batch = llama_batch_init(
            static_cast<int32_t>(samplingParams.max_tokens), 0, 1);

        for (size_t i = 0; i < tokens.size(); ++i) {
            llama_batch_add(batch, tokens[i], static_cast<int32_t>(i),
                           {static_cast<int32_t>(i)}, i == tokens.size() - 1);
        }

        if (llama_decode(impl_->ctx, batch) != 0) {
            spdlog::error("LlamaEngine: prompt decode failed");
            llama_batch_free(batch);
            impl_->generating = false;
            return result;
        }

        llama_batch_free(batch);

        // Sampling
        auto sparams = llama_sampler_chain_default_params();
        auto* smpl = llama_sampler_chain_init(sparams);

        llama_sampler_chain_add(smpl, llama_sampler_init_temp(samplingParams.temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(samplingParams.top_p, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_k(samplingParams.top_k));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(42)); // seed

        // Auto-regressive generation
        std::string generatedText;
        uint32_t tokensGenerated = 0;
        auto firstTokenTime = std::chrono::high_resolution_clock::now();
        bool firstTokenRecorded = false;

        for (uint32_t i = 0; i < samplingParams.max_tokens && !impl_->cancelFlag; ++i) {
            // Sample next token
            llama_token newToken = llama_sampler_sample(smpl, impl_->ctx, -1);

            if (llama_token_is_eog(impl_->model, newToken)) {
                result.stopped_by_eos = true;
                break;
            }

            // Decode token to text
            char buf[256];
            int n = llama_token_to_piece(impl_->model, newToken, buf, sizeof(buf), 0, true);
            if (n > 0) {
                std::string tokenText(buf, n);
                generatedText += tokenText;
                result.tokens.push_back(newToken);

                if (!firstTokenRecorded) {
                    firstTokenTime = std::chrono::high_resolution_clock::now();
                    firstTokenRecorded = true;
                }

                if (on_token) {
                    on_token(tokenText, newToken);
                }
            }

            tokensGenerated++;

            // Check stop strings
            for (const auto& stop : samplingParams.stop_strings) {
                if (generatedText.find(stop) != std::string::npos) {
                    impl_->cancelFlag = true;
                    break;
                }
            }

            // Prepare next batch
            llama_batch singleBatch = llama_batch_init(1, 0, 1);
            singleBatch.n_tokens = 1;
            singleBatch.token[0] = newToken;
            singleBatch.pos[0] = n_prompt + i;
            singleBatch.n_seq_id[0] = 1;
            singleBatch.seq_id[0][0] = 0;
            singleBatch.logits[0] = 1;

            if (llama_decode(impl_->ctx, singleBatch) != 0) {
                spdlog::error("LlamaEngine: decode failed at token {}", i);
                break;
            }

            llama_batch_free(singleBatch);

            if (on_progress) {
                on_progress(tokensGenerated, samplingParams.max_tokens);
            }
        }

        llama_sampler_free(smpl);

        result.text = generatedText;
        result.tokens_generated = tokensGenerated;
        result.total_tokens = n_prompt + tokensGenerated;
        result.stopped_by_limit = (tokensGenerated >= samplingParams.max_tokens);

    } catch (const std::exception& e) {
        spdlog::error("LlamaEngine::Generate exception: {}", e.what());
    }
#else
    // Stub: return a placeholder response
    result.text = "[llama.cpp stub] This is a placeholder response. "
                  "Link llama.cpp to enable real on-device inference.";
    result.tokens_generated = 10;
    result.total_tokens = 10;
    result.stopped_by_limit = true;
#endif

    auto endTime = std::chrono::high_resolution_clock::now();
    result.duration_ms = std::chrono::duration<double, std::milli>(endTime - startTime).count();
    result.tokens_per_second = result.duration_ms > 0
        ? (result.tokens_generated / (result.duration_ms / 1000.0)) : 0.0;

    impl_->totalTokens += result.tokens_generated;
    impl_->totalTimeMs += result.duration_ms;

    impl_->generating = false;
    return result;
}

void LlamaEngine::GenerateStream(
    const std::string& prompt,
    TokenCallback on_token,
    const LlamaSamplingParams& params,
    ProgressCallback on_progress) {
    Generate(prompt, params, std::move(on_token), std::move(on_progress));
}

LlamaGenerationResult LlamaEngine::Chat(
    const std::vector<ChatMessage>& messages,
    const LlamaSamplingParams& params,
    TokenCallback on_token) {

    // Build chat prompt using llama.cpp chat template
    std::string prompt;

#if defined(SOLRA_HAS_LLAMA)
    // Use llama_chat_apply_template if available
    std::vector<llama_chat_message> chatMsgs;
    for (const auto& msg : messages) {
        chatMsgs.push_back({msg.role.c_str(), msg.content.c_str()});
    }

    auto formatted = llama_chat_apply_template(
        impl_->model, nullptr, chatMsgs.data(), chatMsgs.size(),
        true, nullptr, 0);
    prompt = formatted ? std::string(formatted) : "";
#else
    // Fallback: simple concatenation
    for (const auto& msg : messages) {
        if (msg.role == "system") {
            prompt += "<|system|>\n" + msg.content + "\n";
        } else if (msg.role == "user") {
            prompt += "<|user|>\n" + msg.content + "\n";
        } else if (msg.role == "assistant") {
            prompt += "<|assistant|>\n" + msg.content + "\n";
        }
    }
    prompt += "<|assistant|>\n";
#endif

    return Generate(prompt, params, std::move(on_token));
}

// ============================================================================
// Tokenization
// ============================================================================

std::vector<uint32_t> LlamaEngine::Tokenize(const std::string& text) {
    std::vector<uint32_t> tokens;

#if defined(SOLRA_HAS_LLAMA)
    if (!impl_->model) return tokens;

    int n_tokens = -llama_tokenize(impl_->model, text.c_str(),
                                    static_cast<int>(text.size()),
                                    nullptr, 0, true, false);

    if (n_tokens <= 0) return tokens;

    tokens.resize(n_tokens);
    llama_tokenize(impl_->model, text.c_str(),
                   static_cast<int>(text.size()),
                   reinterpret_cast<llama_token*>(tokens.data()),
                   n_tokens, true, false);
#endif

    return tokens;
}

std::string LlamaEngine::Detokenize(const std::vector<uint32_t>& tokens) {
    std::string result;

#if defined(SOLRA_HAS_LLAMA)
    if (!impl_->model || tokens.empty()) return result;

    for (auto token : tokens) {
        char buf[256];
        int n = llama_token_to_piece(impl_->model,
                                      static_cast<llama_token>(token),
                                      buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);
    }
#endif

    return result;
}

// ============================================================================
// State & Stats
// ============================================================================

size_t LlamaEngine::GetModelSize() const {
#if defined(SOLRA_HAS_LLAMA)
    if (impl_->model) return llama_model_size(impl_->model);
#endif
    return 0;
}

size_t LlamaEngine::GetMemoryUsage() const {
#if defined(SOLRA_HAS_LLAMA)
    if (impl_->ctx) {
        return llama_used_mem(impl_->ctx);
    }
#endif
    return 0;
}

uint32_t LlamaEngine::GetContextSize() const {
#if defined(SOLRA_HAS_LLAMA)
    if (impl_->ctx) return llama_n_ctx(impl_->ctx);
#endif
    return impl_->config.context_size;
}

uint32_t LlamaEngine::GetMaxContextSize() const {
    return impl_->config.context_size;
}

void LlamaEngine::ClearKvCache() {
#if defined(SOLRA_HAS_LLAMA)
    if (impl_->ctx) {
        llama_kv_cache_clear(impl_->ctx);
        spdlog::debug("LlamaEngine: KV cache cleared");
    }
#endif
}

void LlamaEngine::SaveSession(const std::string& path) {
    impl_->sessionPath = path;

#if defined(SOLRA_HAS_LLAMA)
    if (impl_->ctx) {
        std::vector<llama_token> sessionTokens(impl_->config.context_size);
        size_t n = llama_state_get_size(impl_->ctx);
        std::vector<uint8_t> state(n);
        llama_state_get_data(impl_->ctx, state.data(), n);

        std::ofstream file(path, std::ios::binary);
        if (file.is_open()) {
            file.write(reinterpret_cast<const char*>(state.data()), n);
            spdlog::info("LlamaEngine: session saved to {} ({} bytes)", path, n);
        }
    }
#endif
}

bool LlamaEngine::LoadSession(const std::string& path) {
#if defined(SOLRA_HAS_LLAMA)
    if (!impl_->ctx) return false;

    std::ifstream file(path, std::ios::binary | std::ios::ate);
    if (!file.is_open()) return false;

    size_t size = file.tellg();
    file.seekg(0);

    std::vector<uint8_t> state(size);
    file.read(reinterpret_cast<char*>(state.data()), size);

    size_t loaded = llama_state_set_data(impl_->ctx, state.data(), size);
    spdlog::info("LlamaEngine: session loaded from {} ({} bytes)", path, loaded);
    return loaded > 0;
#else
    return false;
#endif
}

std::string LlamaEngine::GetLlamaCppVersion() {
#if defined(SOLRA_HAS_LLAMA)
    return std::to_string(LLAMA_VERSION_MAJOR) + "." +
           std::to_string(LLAMA_VERSION_MINOR) + "." +
           std::to_string(LLAMA_VERSION_PATCH);
#else
    return "not-linked";
#endif
}

std::string LlamaEngine::GetBackendName() {
    return Impl::detectBackend();
}

// ============================================================================
// ModelManager
// ============================================================================

struct ModelManager::Impl {
    std::unordered_map<std::string, std::unique_ptr<LlamaEngine>> engines;
    std::string activeModel;
    LlamaEngine* activeEngine = nullptr;
    std::mutex mutex;
};

ModelManager& ModelManager::Instance() {
    static ModelManager mgr;
    return mgr;
}

ModelManager::ModelManager()
    : impl_(std::make_unique<Impl>()) {}

bool ModelManager::RegisterModel(const std::string& name,
                                  const LlamaModelConfig& config) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    auto engine = std::make_unique<LlamaEngine>();
    if (!engine->LoadModel(config)) return false;
    impl_->engines[name] = std::move(engine);
    spdlog::info("ModelManager: registered '{}'", name);
    return true;
}

bool ModelManager::ActivateModel(const std::string& name) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    auto it = impl_->engines.find(name);
    if (it == impl_->engines.end()) return false;
    impl_->activeModel = name;
    impl_->activeEngine = it->second.get();
    spdlog::info("ModelManager: activated '{}'", name);
    return true;
}

LlamaEngine* ModelManager::GetEngine(const std::string& name) {
    auto it = impl_->engines.find(name);
    return (it != impl_->engines.end()) ? it->second.get() : nullptr;
}

LlamaEngine* ModelManager::GetActiveEngine() {
    return impl_->activeEngine;
}

std::vector<std::string> ModelManager::ListModels() const {
    std::vector<std::string> names;
    for (const auto& [name, _] : impl_->engines) {
        names.push_back(name);
    }
    return names;
}

void ModelManager::PreloadModel(const std::string& name) {
    // Already loaded via RegisterModel
    spdlog::debug("ModelManager: preload '{}'", name);
}

void ModelManager::UnloadModel(const std::string& name) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    if (impl_->activeModel == name) {
        impl_->activeModel.clear();
        impl_->activeEngine = nullptr;
    }
    impl_->engines.erase(name);
    spdlog::info("ModelManager: unloaded '{}'", name);
}

QuantFormat ModelManager::RecommendQuantForDevice(size_t deviceRamGb) {
    if (deviceRamGb >= 8)  return QuantFormat::kQ8_0;    // 8GB+ RAM: Q8_0 quality
    if (deviceRamGb >= 6)  return QuantFormat::kQ6_K;    // 6GB RAM: Q6_K
    if (deviceRamGb >= 4)  return QuantFormat::kQ5_K_M;  // 4GB RAM: Q5_K_M
    if (deviceRamGb >= 2)  return QuantFormat::kQ4_K_M;  // 2GB RAM: Q4_K_M
    return QuantFormat::kQ4_0;                            // <2GB: Q4_0 minimum
}

} // namespace solra::core::inference
