#pragma once
/// @file inference_benchmark.hpp
/// @brief 端侧推理性能基准测试框架 —— 多场景基准收集与SLO验证
/// @ingroup core/inference
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <string>
#include <vector>
#include <chrono>
#include <memory>

namespace solra::core::inference {

// ============================================================================
// 设备性能等级
// ============================================================================

enum class InferenceDeviceTier : uint8_t {
  kLowEnd,      // 1-3B 模型，Q4_0 量化，CPU 4线程
  kMidRange,    // 3-7B 模型，Q4_K_M 量化，CPU 6线程 + GPU offload
  kHighEnd,     // 7-13B 模型，Q5_K_M 量化，GPU offload 全层
  kDesktop,     // 13B+ 模型，Q6_K/Q8_0 量化，桌面 GPU/CUDA
};

// ============================================================================
// 推理 SLO 基准 (Service Level Objective)
// ============================================================================

struct InferenceSLO {
  // 延迟目标
  double max_ttft_ms_low_end      = 1500.0;   // TTFT ≤ 1.5s 低端
  double max_ttft_ms_mid_range    = 800.0;    // TTFT ≤ 800ms 中端
  double max_ttft_ms_high_end     = 400.0;    // TTFT ≤ 400ms 高端
  double max_ttft_ms_desktop      = 200.0;    // TTFT ≤ 200ms 桌面

  double max_tpot_ms_low_end      = 80.0;     // TPOT ≤ 80ms 低端
  double max_tpot_ms_mid_range    = 50.0;     // TPOT ≤ 50ms 中端
  double max_tpot_ms_high_end     = 30.0;     // TPOT ≤ 30ms 高端
  double max_tpot_ms_desktop      = 15.0;     // TPOT ≤ 15ms 桌面

  double min_tokens_per_sec_low    = 10.0;    // ≥10 tok/s 低端
  double min_tokens_per_sec_mid    = 20.0;    // ≥20 tok/s 中端
  double min_tokens_per_sec_high   = 40.0;    // ≥40 tok/s 高端
  double min_tokens_per_sec_desk   = 80.0;    // ≥80 tok/s 桌面

  // 内存目标
  double max_memory_mb_low_end    = 512.0;    // ≤512MB 低端
  double max_memory_mb_mid_range  = 1024.0;   // ≤1GB 中端
  double max_memory_mb_high_end   = 2048.0;   // ≤2GB 高端
  double max_memory_mb_desktop    = 4096.0;   // ≤4GB 桌面

  // 上下文利用率
  double min_context_utilization  = 0.8;      // ≥80% 上下文可用
};

// ============================================================================
// 单次推理指标
// ============================================================================

struct InferenceMetrics {
  uint64_t request_id        = 0;

  // 延迟
  double ttft_ms             = 0.0;  // Time To First Token
  double tpot_ms             = 0.0;  // Time Per Output Token
  double total_duration_ms   = 0.0;  // 总耗时

  // 吞吐
  uint32_t prompt_tokens     = 0;    // 输入 token 数
  uint32_t generated_tokens  = 0;    // 输出 token 数
  double tokens_per_second   = 0.0;  // 生成吞吐

  // 内存
  size_t memory_before_bytes = 0;    // 推理前内存
  size_t memory_peak_bytes   = 0;    // 推理峰值内存
  size_t memory_after_bytes  = 0;    // 推理后内存
  size_t kv_cache_bytes      = 0;    // KV Cache 占用

  // 上下文
  double context_utilization = 0.0;  // 上下文窗口利用率

  // 质量
  int stopped_by_eos         = 0;    // 正常 EOS 停止
  int stopped_by_limit       = 0;    // 达到 max_tokens 限制
  int stopped_by_oom         = 0;    // OOM 终止

  // 卡顿检测
  int janky_token_intervals  = 0;    // token 间隔 > 2x TPOT 的次数
  double jank_rate_pct       = 0.0;  // 卡顿 token 比例
};

// ============================================================================
// 基准会话统计
// ============================================================================

struct InferenceBenchmarkStats {
  uint64_t total_requests        = 0;
  double total_duration_seconds   = 0.0;

  // TTFT 统计
  double avg_ttft_ms             = 0.0;
  double p50_ttft_ms             = 0.0;
  double p95_ttft_ms             = 0.0;
  double p99_ttft_ms             = 0.0;

  // TPOT 统计
  double avg_tpot_ms             = 0.0;
  double p50_tpot_ms             = 0.0;
  double p95_tpot_ms             = 0.0;
  double p99_tpot_ms             = 0.0;

  // 吞吐
  double avg_tokens_per_second   = 0.0;
  double total_tokens_generated  = 0.0;

  // 内存
  double avg_peak_memory_mb      = 0.0;
  double max_peak_memory_mb      = 0.0;

  // 卡顿
  double avg_jank_rate_pct       = 0.0;
  uint64_t total_janky_requests  = 0;

  // 质量
  uint64_t eos_completions       = 0;
  uint64_t limit_completions     = 0;
  uint64_t oom_completions       = 0;

  bool meets_slo                 = false;
  std::string slo_violations;
};

// ============================================================================
// 推理基准场景
// ============================================================================

struct InferenceBenchmarkScene {
  std::string name;
  std::string description;

  std::string prompt;                    // 输入 prompt
  uint32_t max_tokens         = 256;     // 最大生成 token
  uint32_t num_requests       = 10;      // 请求重复次数
  uint32_t warmup_requests    = 2;       // 预热请求数

  float temperature           = 0.7f;    // 采样温度
  bool measure_memory         = true;    // 是否测量内存

  // 预定义场景
  static InferenceBenchmarkScene ShortPrompt();      // 短提示 (<50 tokens)
  static InferenceBenchmarkScene MediumPrompt();     // 中等提示 (~200 tokens)
  static InferenceBenchmarkScene LongPrompt();       // 长提示 (~1000 tokens)
  static InferenceBenchmarkScene MultiTurn();        // 多轮对话
  static InferenceBenchmarkScene StreamingStress();  // 流式生成压力
};

// ============================================================================
// 推理性能基准测试器
// ============================================================================

class InferenceBenchmark {
 public:
  explicit InferenceBenchmark(InferenceDeviceTier tier);
  ~InferenceBenchmark();

  // 配置
  void SetDeviceTier(InferenceDeviceTier tier) { tier_ = tier; }
  void SetSLO(const InferenceSLO& slo) { slo_ = slo; }

  // 会话管理
  void BeginSession(const std::string& session_name);
  void EndSession();

  // 单次推理记录
  void RecordInference(const InferenceMetrics& metrics);

  // 场景基准测试
  InferenceBenchmarkStats RunScene(
      const InferenceBenchmarkScene& scene,
      std::function<void(const std::string& text)> stream_cb = {},
      std::function<void(float)> progress_cb = {});

  // 完整基准套件
  struct FullBenchmarkResult {
    InferenceDeviceTier device_tier;
    std::string backend_name;
    std::string model_name;

    InferenceBenchmarkStats short_prompt;
    InferenceBenchmarkStats medium_prompt;
    InferenceBenchmarkStats long_prompt;
    InferenceBenchmarkStats multi_turn;
    InferenceBenchmarkStats streaming_stress;

    bool all_slo_pass;
    double overall_score;  // 0-100

    std::string ToJson() const;
    std::string ToMarkdown() const;
  };

  FullBenchmarkResult RunFullSuite(
      std::function<void(const std::string&)> stream_cb = {});

  // 结果查询
  const InferenceBenchmarkStats& GetCurrentStats() const { return stats_; }
  const std::vector<InferenceMetrics>& GetHistory() const { return history_; }

  // 环境检测
  static InferenceDeviceTier DetectDeviceTier();
  static std::string GetBackendName();
  static size_t GetTotalMemoryMB();

  // 运行时卡顿检测工具
  static bool IsJankyInterval(double interval_ms, double avg_tpot_ms);
  static double ComputeJankRate(const std::vector<double>& token_intervals_ms,
                                 double avg_tpot_ms);

 private:
  InferenceDeviceTier tier_;
  InferenceSLO slo_;

  InferenceBenchmarkStats stats_;
  std::vector<InferenceMetrics> history_;

  std::chrono::steady_clock::time_point session_start_;

  void ComputeStats();
  bool CheckSLO();
};

} // namespace solra::core::inference
