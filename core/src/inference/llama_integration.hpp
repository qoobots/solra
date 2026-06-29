#pragma once
/// @file llama_integration.hpp
/// @brief llama.cpp 集成层 —— 端侧SLM量化推理
/// @ingroup core/inference
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <optional>

namespace solra::core::inference {

// ============================================================================
// 量化格式
// ============================================================================

enum class QuantFormat : uint8_t {
  kQ4_0,        // Q4_0: 4-bit量化, 零舍入, 最小尺寸
  kQ4_K_M,      // Q4_K_M: 4-bit K-Quants, 中等质量
  kQ5_K_M,      // Q5_K_M: 5-bit K-Quants, 更好质量
  kQ6_K,        // Q6_K: 6-bit K-Quants, 高质量
  kQ8_0,        // Q8_0: 8-bit量化, 接近无损
  kF16,         // FP16浮点
  kAuto,        // 自动选择(根据设备vRAM)
};

inline const char* QuantFormatName(QuantFormat f) {
  switch (f) {
    case QuantFormat::kQ4_0:   return "Q4_0 (4-bit, 最小)";
    case QuantFormat::kQ4_K_M: return "Q4_K_M (4-bit K-Quants)";
    case QuantFormat::kQ5_K_M: return "Q5_K_M (5-bit K-Quants)";
    case QuantFormat::kQ6_K:   return "Q6_K (6-bit)";
    case QuantFormat::kQ8_0:   return "Q8_0 (8-bit)";
    case QuantFormat::kF16:    return "F16 (半精度)";
    case QuantFormat::kAuto:   return "Auto 自动";
  }
  return "Unknown";
}

// ============================================================================
// 模型配置
// ============================================================================

struct LlamaModelConfig {
  std::string model_path;          // .gguf 文件路径
  QuantFormat quant_format = QuantFormat::kQ4_K_M;

  // 上下文窗口
  uint32_t context_size = 4096;    // n_ctx

  // 批处理
  uint32_t batch_size  = 512;      // n_batch
  uint32_t ubatch_size = 512;      // n_ubatch

  // 线程
  uint32_t cpu_threads = 4;

  // Flash Attention (需编译时开启)
  bool use_flash_attn   = true;
  bool use_mmap         = true;    // 内存映射加载
  bool use_mlock        = false;   // 锁定内存

  // GPU offload (Metal/Vulkan/CUDA)
  int32_t gpu_layers    = 0;       // 0 = auto, -1 = 全部CPU

  // KV Cache 量化
  enum class KvCacheType : uint8_t { kF16, kQ8_0, kQ4_0 };
  KvCacheType kv_cache_type = KvCacheType::kQ8_0;
};

// ============================================================================
// 采样参数
// ============================================================================

struct LlamaSamplingParams {
  float temperature    = 0.7f;      // 温度 (0=确定性, >1=随机)
  float top_p          = 0.9f;      // nucleus sampling
  float top_k          = 40;        // top-k 采样
  float min_p          = 0.05f;     // min-p 过滤
  float repetition_penalty = 1.1f;  // 重复惩罚

  int32_t mirostat     = 0;         // 0=关闭, 1=v1, 2=v2
  float mirostat_tau   = 5.0f;
  float mirostat_eta   = 0.1f;

  // 停止条件
  std::vector<std::string> stop_strings;
  uint32_t max_tokens   = 256;

  // 语法约束 (GGML BNF Grammar)
  std::optional<std::string> grammar;
};

// ============================================================================
// 生成结果
// ============================================================================

struct LlamaGenerationResult {
  std::string text;
  std::vector<uint32_t> tokens;

  uint32_t tokens_generated   = 0;
  uint32_t total_tokens       = 0;   // prompt + generated
  double   duration_ms        = 0.0;
  double   tokens_per_second  = 0.0;

  double   time_to_first_token_ms = 0.0;  // TTFT
  double   time_per_output_token_ms = 0.0; // TPOT

  bool     stopped_by_limit   = false;
  bool     stopped_by_eos     = false;
};

// ============================================================================
// 回调类型
// ============================================================================

using TokenCallback = std::function<void(const std::string& token_text, uint32_t token_id)>;
using ProgressCallback = std::function<void(uint32_t tokens_generated, uint32_t max_tokens)>;

// ============================================================================
// llama.cpp 集成引擎
// ============================================================================

class LlamaEngine {
 public:
  LlamaEngine();
  ~LlamaEngine();

  // 禁止拷贝
  LlamaEngine(const LlamaEngine&) = delete;
  LlamaEngine& operator=(const LlamaEngine&) = delete;

  // 生命周期
  bool LoadModel(const LlamaModelConfig& config);
  void UnloadModel();
  bool IsLoaded() const;

  // 推理
  LlamaGenerationResult Generate(
      const std::string& prompt,
      const LlamaSamplingParams& params = {},
      TokenCallback on_token = nullptr,
      ProgressCallback on_progress = nullptr);

  // 流式生成 (回调模式)
  void GenerateStream(
      const std::string& prompt,
      TokenCallback on_token,
      const LlamaSamplingParams& params = {},
      ProgressCallback on_progress = nullptr);

  // 对话接口 (带系统提示)
  struct ChatMessage {
    std::string role;     // "system" | "user" | "assistant"
    std::string content;
  };

  LlamaGenerationResult Chat(
      const std::vector<ChatMessage>& messages,
      const LlamaSamplingParams& params = {},
      TokenCallback on_token = nullptr);

  // Token 化
  std::vector<uint32_t> Tokenize(const std::string& text);
  std::string Detokenize(const std::vector<uint32_t>& tokens);

  // 状态查询
  size_t GetModelSize() const;          // 模型文件大小 (bytes)
  size_t GetMemoryUsage() const;        // 当前内存占用
  uint32_t GetContextSize() const;
  uint32_t GetMaxContextSize() const;

  // KV Cache 管理
  void ClearKvCache();
  void SaveSession(const std::string& path);
  bool LoadSession(const std::string& path);

  // 版本信息
  static std::string GetLlamaCppVersion();
  static std::string GetBackendName();  // "Metal" / "Vulkan" / "CPU"

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// 多模型管理器
// ============================================================================

class ModelManager {
 public:
  static ModelManager& Instance();

  // 注册模型
  bool RegisterModel(const std::string& name,
                     const LlamaModelConfig& config);

  // 激活模型 (切换当前推理模型)
  bool ActivateModel(const std::string& name);

  // 获取引擎
  LlamaEngine* GetEngine(const std::string& name);
  LlamaEngine* GetActiveEngine();

  // 列出已注册模型
  std::vector<std::string> ListModels() const;

  // 预加载/卸载
  void PreloadModel(const std::string& name);
  void UnloadModel(const std::string& name);

  // 推荐量化格式 (根据设备内存)
  static QuantFormat RecommendQuantForDevice(size_t device_ram_gb);

 private:
  ModelManager();
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

} // namespace solra::core::inference
