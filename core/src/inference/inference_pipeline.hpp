#pragma once
/// @file inference_pipeline.hpp
/// @brief 端侧推理流水线 —— Token生成→后处理→回调调度
/// @ingroup core/inference
/// @priority P0 (工程底线——原型H1必须就绪)

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <deque>
#include <functional>
#include <future>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <thread>
#include <vector>

namespace solra::core::inference {

// ============================================================================
// 推理任务优先级
// ============================================================================

enum class InferencePriority : uint8_t {
  kRealtime   = 0,  // 实时对话 (最高)
  kInteractive = 1,  // 交互式 (推荐/分析)
  kBackground  = 2,  // 后台 (预处理/索引)
};

// ============================================================================
// 推理请求
// ============================================================================

struct InferenceRequest {
  std::string request_id;

  // 输入
  std::string prompt;
  std::vector<std::string> history;  // 多轮对话历史
  std::string system_prompt;

  // 参数
  uint32_t max_tokens       = 256;
  float    temperature      = 0.7f;
  float    top_p            = 0.9f;
  InferencePriority priority = InferencePriority::kInteractive;

  // 超时
  std::chrono::milliseconds timeout{30000}; // 30s 默认

  // 用户上下文 (可选, 透传至回调)
  void* user_data = nullptr;

  // 时间戳
  std::chrono::steady_clock::time_point submit_time;
};

// ============================================================================
// 推理结果
// ============================================================================

struct InferenceResult {
  std::string request_id;
  std::string text;
  std::vector<uint32_t> tokens;

  // 性能指标
  double time_to_first_token_ms  = 0.0;  // TTFT
  double time_per_output_token_ms = 0.0; // TPOT
  double total_duration_ms        = 0.0;
  uint32_t tokens_generated       = 0;
  double tokens_per_second        = 0.0;

  // 质量和状态
  float   confidence         = 1.0f;   // 推理置信度 (0-1)
  bool    success            = true;
  bool    timeout_occurred   = false;
  std::string error_message;

  // 后处理结果
  struct PostProcessResult {
    bool   safety_pass      = true;
    float  safety_score     = 1.0f;
    bool   sensitive_blocked = false;
    std::string filtered_text;
  };
  std::optional<PostProcessResult> post_process;
};

// ============================================================================
// 回调类型
// ============================================================================

// 流式 token 回调
using StreamTokenCallback = std::function<void(
    const std::string& request_id,
    const std::string& token_text,
    uint32_t token_index)>;

// 完成回调
using InferenceCallback = std::function<void(const InferenceResult& result)>;

// 错误回调
using ErrorCallback = std::function<void(
    const std::string& request_id, const std::string& error)>;

// ============================================================================
// 后处理器
// ============================================================================

class IPostProcessor {
 public:
  virtual ~IPostProcessor() = default;

  // 名称
  virtual std::string Name() const = 0;

  // 处理 (在推理线程中同步调用)
  virtual InferenceResult::PostProcessResult Process(
      const InferenceResult& result) = 0;
};

// 安全过滤后处理器
class SafetyPostProcessor : public IPostProcessor {
 public:
  std::string Name() const override { return "SafetyFilter"; }
  InferenceResult::PostProcessResult Process(const InferenceResult& result) override;

  // 关键词列表
  void SetBlockedKeywords(const std::vector<std::string>& keywords);
  void SetBlockedRegex(const std::string& pattern);

 private:
  std::vector<std::string> blocked_keywords_;
  // std::regex blocked_regex_;
};

// 格式化后处理器
class FormatPostProcessor : public IPostProcessor {
 public:
  std::string Name() const override { return "FormatCleaner"; }
  InferenceResult::PostProcessResult Process(const InferenceResult& result) override;
};

// ============================================================================
// 推理流水线
// ============================================================================

class InferencePipeline {
 public:
  explicit InferencePipeline(size_t worker_threads = 1);
  ~InferencePipeline();

  // 禁止拷贝
  InferencePipeline(const InferencePipeline&) = delete;
  InferencePipeline& operator=(const InferencePipeline&) = delete;

  // === 任务提交 ===

  // 同步推理 (阻塞调用)
  InferenceResult Infer(const InferenceRequest& request);

  // 异步推理 (返回 future)
  std::future<InferenceResult> InferAsync(const InferenceRequest& request);

  // 流式推理 (通过回调推送每个 token)
  void InferStream(const InferenceRequest& request,
                   StreamTokenCallback on_token,
                   InferenceCallback on_complete = nullptr,
                   ErrorCallback on_error = nullptr);

  // === 流水线控制 ===

  // 取消指定请求
  bool CancelRequest(const std::string& request_id);

  // 等待所有任务完成 (带超时)
  void WaitAll(std::chrono::milliseconds timeout = std::chrono::milliseconds(60000));

  // 暂停/恢复
  void Pause();
  void Resume();

  // 清空队列
  void ClearQueue();

  // === 后处理器 ===
  void AddPostProcessor(std::unique_ptr<IPostProcessor> processor);
  void RemovePostProcessor(const std::string& name);
  void SetPostProcessingEnabled(bool enabled);

  // === 状态查询 ===

  struct PipelineStats {
    uint64_t total_requests      = 0;
    uint64_t completed_requests  = 0;
    uint64_t failed_requests     = 0;
    uint64_t rejected_requests   = 0;

    size_t   queue_depth         = 0;
    uint32_t active_workers      = 0;

    double   avg_latency_ms      = 0.0;
    double   avg_ttft_ms         = 0.0;
    double   avg_tpot_ms         = 0.0;
    double   avg_tokens_per_sec  = 0.0;

    bool     is_paused           = false;
  };

  PipelineStats GetStats() const;

  // === 模型管理 ===
  void SetModelName(const std::string& name);
  std::string GetModelName() const;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;

  void WorkerLoop();
};

} // namespace solra::core::inference
