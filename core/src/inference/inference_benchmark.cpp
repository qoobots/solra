#include "inference_benchmark.hpp"

#include <algorithm>
#include <cmath>
#include <numeric>
#include <sstream>
#include <cstdio>
#include <map>
#include <chrono>
#include <thread>

#if defined(__APPLE__)
  #include <sys/sysctl.h>
  #include <mach/mach.h>
#elif defined(_WIN32)
  #include <windows.h>
#elif defined(__ANDROID__)
  #include <fstream>
#endif

namespace solra::core::inference {

// ============================================================================
// InferenceBenchmarkScene 预定义场景
// ============================================================================

InferenceBenchmarkScene InferenceBenchmarkScene::ShortPrompt() {
  return {"ShortPrompt", "短提示基准 (<50 tokens)",
          "What is the capital of France?", 64, 10, 2, 0.7f, true};
}

InferenceBenchmarkScene InferenceBenchmarkScene::MediumPrompt() {
  return {"MediumPrompt", "中等提示基准 (~200 tokens)",
          "Explain the concept of machine learning in simple terms. "
          "Include examples of supervised, unsupervised, and reinforcement learning. "
          "Keep the explanation accessible to someone with no technical background.",
          256, 10, 2, 0.7f, true};
}

InferenceBenchmarkScene InferenceBenchmarkScene::LongPrompt() {
  return {"LongPrompt", "长提示基准 (~1000 tokens)",
          "Write a detailed technical specification for a cross-platform 3D rendering engine. "
          "The engine must support: (1) Vulkan, Metal, and DirectX 12 backends; "
          "(2) Physically-based rendering with GGX BRDF; (3) Cascaded shadow maps with 4 splits; "
          "(4) Screen-space ambient occlusion (SSAO) and screen-space reflections (SSR); "
          "(5) GPU-driven rendering with indirect draw and mesh shaders; "
          "(6) Temporal anti-aliasing (TAA) and FidelityFX Super Resolution 2 (FSR2); "
          "(7) Skeletal animation with dual quaternion skinning; "
          "(8) Asset streaming with progressive LOD and virtual texturing. "
          "Describe each subsystem in detail, including data structures, shader pipelines, "
          "and memory management strategies. Target platforms: iOS, Android, Windows, macOS.",
          512, 5, 2, 0.7f, true};
}

InferenceBenchmarkScene InferenceBenchmarkScene::MultiTurn() {
  return {"MultiTurn", "多轮对话基准",
          "Let's have a conversation about renewable energy. "
          "First, what are the main types of renewable energy sources?",
          128, 20, 4, 0.8f, true};
}

InferenceBenchmarkScene InferenceBenchmarkScene::StreamingStress() {
  return {"StreamingStress", "流式生成压力测试",
          "Write a story about a robot learning to paint. Be creative and descriptive.",
          1024, 3, 1, 0.9f, true};
}

// ============================================================================
// InferenceBenchmark 实现
// ============================================================================

InferenceBenchmark::InferenceBenchmark(InferenceDeviceTier tier)
    : tier_(tier) {}

InferenceBenchmark::~InferenceBenchmark() = default;

void InferenceBenchmark::BeginSession(const std::string& session_name) {
  stats_ = InferenceBenchmarkStats{};
  history_.clear();
  history_.reserve(500);
  session_start_ = std::chrono::steady_clock::now();
}

void InferenceBenchmark::EndSession() {
  auto now = std::chrono::steady_clock::now();
  stats_.total_duration_seconds =
      std::chrono::duration<double>(now - session_start_).count();
  ComputeStats();
  stats_.meets_slo = CheckSLO();
}

void InferenceBenchmark::RecordInference(const InferenceMetrics& metrics) {
  history_.push_back(metrics);
  stats_.total_requests++;
}

InferenceBenchmarkStats InferenceBenchmark::RunScene(
    const InferenceBenchmarkScene& scene,
    std::function<void(const std::string&)> stream_cb,
    std::function<void(float)> progress_cb) {

  BeginSession(scene.name);

  uint32_t total = scene.warmup_requests + scene.num_requests;
  for (uint32_t i = 0; i < total; ++i) {
    InferenceMetrics m;
    m.request_id = i;

    auto start = std::chrono::high_resolution_clock::now();
    auto ttft_start = start;

    // 模拟推理过程（实际使用时由 LlamaEngine::GenerateStream 驱动）
    // 这里为离线基准框架提供标准化的指标收集接口
    // 生产环境通过 stream_cb 回调驱动真实的 llama.cpp GenerateStream
    m.prompt_tokens = static_cast<uint32_t>(scene.prompt.size() / 4); // rough estimate
    m.generated_tokens = 0;

    // 模拟 TTFT（prompt processing）
    double simulated_ttft = 100.0;  // 默认 100ms
    m.ttft_ms = simulated_ttft;

    // 模拟逐 token 生成
    uint32_t generated = 0;
    std::vector<double> token_intervals;
    for (uint32_t t = 0; t < scene.max_tokens && generated < scene.max_tokens; ++t) {
      // 模拟每 token 耗时（含卡顿）
      double interval = 20.0; // 默认 20ms/token = 50 tok/s
      if (t % 100 == 97) interval *= 3.5; // 模拟偶发卡顿
      token_intervals.push_back(interval);

      m.generated_tokens = t + 1;

      if (stream_cb) {
        stream_cb("tok" + std::to_string(t) + " ");
      }

      if (t >= scene.max_tokens - 1) {
        m.stopped_by_limit = 1;
      }
    }

    auto end = std::chrono::high_resolution_clock::now();
    m.total_duration_ms =
        std::chrono::duration<double, std::milli>(end - start).count();

    if (m.generated_tokens > 0) {
      m.tpot_ms = (m.total_duration_ms - m.ttft_ms) / m.generated_tokens;
      m.tokens_per_second = 1000.0 / m.tpot_ms;
    }

    // 卡顿检测
    if (!token_intervals.empty()) {
      double avg_tpot = m.tpot_ms;
      m.janky_token_intervals = 0;
      for (double iv : token_intervals) {
        if (IsJankyInterval(iv, avg_tpot)) {
          m.janky_token_intervals++;
        }
      }
      m.jank_rate_pct = (m.janky_token_intervals * 100.0) / token_intervals.size();
    }

    // 内存模拟
    m.memory_peak_bytes = static_cast<size_t>(scene.prompt.size() * 1024 + generated * 512);
    m.kv_cache_bytes = static_cast<size_t>((m.prompt_tokens + m.generated_tokens) * 128);
    m.context_utilization = static_cast<double>(m.prompt_tokens + m.generated_tokens) / 4096.0;

    if (i >= scene.warmup_requests) {
      RecordInference(m);
    }

    if (progress_cb && (i % 3 == 0 || i == total - 1)) {
      progress_cb(static_cast<float>(i) / total);
    }
  }

  EndSession();
  return stats_;
}

InferenceBenchmark::FullBenchmarkResult InferenceBenchmark::RunFullSuite(
    std::function<void(const std::string&)> stream_cb) {

  FullBenchmarkResult result;
  result.device_tier = tier_;
  result.backend_name = GetBackendName();

  result.short_prompt    = RunScene(InferenceBenchmarkScene::ShortPrompt(), stream_cb);
  result.medium_prompt   = RunScene(InferenceBenchmarkScene::MediumPrompt(), stream_cb);
  result.long_prompt     = RunScene(InferenceBenchmarkScene::LongPrompt(), stream_cb);
  result.multi_turn      = RunScene(InferenceBenchmarkScene::MultiTurn(), stream_cb);
  result.streaming_stress = RunScene(InferenceBenchmarkScene::StreamingStress(), stream_cb);

  result.all_slo_pass = result.short_prompt.meets_slo &&
                         result.medium_prompt.meets_slo &&
                         result.long_prompt.meets_slo;

  // 综合评分
  result.overall_score = 0.0;
  auto weight = [](const InferenceBenchmarkStats& s, double w) {
    return s.meets_slo ? w * 20.0 : 0.0;
  };
  result.overall_score += weight(result.short_prompt, 1.5);
  result.overall_score += weight(result.medium_prompt, 2.0);
  result.overall_score += weight(result.long_prompt, 1.5);
  result.overall_score += weight(result.multi_turn, 1.0);
  result.overall_score += weight(result.streaming_stress, 0.5);

  return result;
}

void InferenceBenchmark::ComputeStats() {
  if (history_.empty()) return;

  const size_t n = history_.size();

  // TTFT 统计
  std::vector<double> ttft_vals;
  ttft_vals.reserve(n);
  double sum_ttft = 0.0;

  // TPOT 统计
  std::vector<double> tpot_vals;
  tpot_vals.reserve(n);
  double sum_tpot = 0.0;

  double sum_tps = 0.0, sum_mem = 0.0, max_mem = 0.0;
  double sum_jank = 0.0;
  double total_tokens = 0.0;

  for (const auto& m : history_) {
    ttft_vals.push_back(m.ttft_ms);
    sum_ttft += m.ttft_ms;

    tpot_vals.push_back(m.tpot_ms);
    sum_tpot += m.tpot_ms;

    sum_tps += m.tokens_per_second;
    sum_mem += static_cast<double>(m.memory_peak_bytes);
    max_mem = std::max(max_mem, static_cast<double>(m.memory_peak_bytes));
    sum_jank += m.jank_rate_pct;
    total_tokens += m.generated_tokens;

    if (m.stopped_by_eos) stats_.eos_completions++;
    if (m.stopped_by_limit) stats_.limit_completions++;
    if (m.stopped_by_oom) stats_.oom_completions++;
    if (m.jank_rate_pct > 5.0) stats_.total_janky_requests++;
  }

  stats_.avg_ttft_ms = sum_ttft / n;
  stats_.avg_tpot_ms = sum_tpot / n;
  stats_.avg_tokens_per_second = sum_tps / n;
  stats_.avg_peak_memory_mb = (sum_mem / n) / (1024.0 * 1024.0);
  stats_.max_peak_memory_mb = max_mem / (1024.0 * 1024.0);
  stats_.avg_jank_rate_pct = sum_jank / n;
  stats_.total_tokens_generated = total_tokens;

  // 百分位
  std::sort(ttft_vals.begin(), ttft_vals.end());
  stats_.p50_ttft_ms = ttft_vals[n * 50 / 100];
  stats_.p95_ttft_ms = ttft_vals[n * 95 / 100];
  stats_.p99_ttft_ms = ttft_vals[n * 99 / 100];

  std::sort(tpot_vals.begin(), tpot_vals.end());
  stats_.p50_tpot_ms = tpot_vals[n * 50 / 100];
  stats_.p95_tpot_ms = tpot_vals[n * 95 / 100];
  stats_.p99_tpot_ms = tpot_vals[n * 99 / 100];
}

bool InferenceBenchmark::CheckSLO() {
  std::vector<std::string> violations;

  double max_ttft, max_tpot, min_tps, max_mem;
  switch (tier_) {
    case InferenceDeviceTier::kLowEnd:
      max_ttft = slo_.max_ttft_ms_low_end;
      max_tpot = slo_.max_tpot_ms_low_end;
      min_tps  = slo_.min_tokens_per_sec_low;
      max_mem  = slo_.max_memory_mb_low_end;
      break;
    case InferenceDeviceTier::kMidRange:
      max_ttft = slo_.max_ttft_ms_mid_range;
      max_tpot = slo_.max_tpot_ms_mid_range;
      min_tps  = slo_.min_tokens_per_sec_mid;
      max_mem  = slo_.max_memory_mb_mid_range;
      break;
    case InferenceDeviceTier::kHighEnd:
      max_ttft = slo_.max_ttft_ms_high_end;
      max_tpot = slo_.max_tpot_ms_high_end;
      min_tps  = slo_.min_tokens_per_sec_high;
      max_mem  = slo_.max_memory_mb_high_end;
      break;
    case InferenceDeviceTier::kDesktop:
      max_ttft = slo_.max_ttft_ms_desktop;
      max_tpot = slo_.max_tpot_ms_desktop;
      min_tps  = slo_.min_tokens_per_sec_desk;
      max_mem  = slo_.max_memory_mb_desktop;
      break;
  }

  if (stats_.avg_ttft_ms > max_ttft)
    violations.push_back("avg_ttft(" + std::to_string(stats_.avg_ttft_ms) +
                         "ms) > limit(" + std::to_string(max_ttft) + "ms)");
  if (stats_.avg_tpot_ms > max_tpot)
    violations.push_back("avg_tpot(" + std::to_string(stats_.avg_tpot_ms) +
                         "ms) > limit(" + std::to_string(max_tpot) + "ms)");
  if (stats_.avg_tokens_per_second < min_tps)
    violations.push_back("tok/s(" + std::to_string(stats_.avg_tokens_per_second) +
                         ") < min(" + std::to_string(min_tps) + ")");
  if (stats_.max_peak_memory_mb > max_mem)
    violations.push_back("peak_mem(" + std::to_string(stats_.max_peak_memory_mb) +
                         "MB) > limit(" + std::to_string(max_mem) + "MB)");

  if (!violations.empty()) {
    std::ostringstream oss;
    for (size_t i = 0; i < violations.size(); ++i) {
      if (i > 0) oss << "; ";
      oss << violations[i];
    }
    stats_.slo_violations = oss.str();
  }

  return violations.empty();
}

// ============================================================================
// 环境检测
// ============================================================================

InferenceDeviceTier InferenceBenchmark::DetectDeviceTier() {
  size_t mem = GetTotalMemoryMB();
  if (mem >= 16384) return InferenceDeviceTier::kDesktop;
  if (mem >= 8192)  return InferenceDeviceTier::kHighEnd;
  if (mem >= 4096)  return InferenceDeviceTier::kMidRange;
  return InferenceDeviceTier::kLowEnd;
}

std::string InferenceBenchmark::GetBackendName() {
#if defined(SOLRA_HAS_LLAMA)
  return LlamaEngine::GetBackendName();
#else
  return "CPU (stub)";
#endif
}

size_t InferenceBenchmark::GetTotalMemoryMB() {
#if defined(__APPLE__)
  int64_t mem = 0;
  size_t len = sizeof(mem);
  sysctlbyname("hw.memsize", &mem, &len, nullptr, 0);
  return static_cast<size_t>(mem / (1024 * 1024));
#elif defined(_WIN32)
  MEMORYSTATUSEX mex = {sizeof(mex)};
  GlobalMemoryStatusEx(&mex);
  return static_cast<size_t>(mex.ullTotalPhys / (1024 * 1024));
#elif defined(__ANDROID__)
  std::ifstream meminfo("/proc/meminfo");
  std::string line;
  size_t total = 0;
  while (std::getline(meminfo, line)) {
    if (line.find("MemTotal:") == 0) {
      sscanf(line.c_str(), "MemTotal: %zu kB", &total);
      return total / 1024;
    }
  }
  return 2048;
#else
  return 4096;
#endif
}

// ============================================================================
// 运行时卡顿检测工具
// ============================================================================

bool InferenceBenchmark::IsJankyInterval(double interval_ms, double avg_tpot_ms) {
  if (avg_tpot_ms <= 0.0) return false;
  // 超过 2x 平均 TPOT 或超过 100ms 视为卡顿
  return interval_ms > std::max(avg_tpot_ms * 2.0, 100.0);
}

double InferenceBenchmark::ComputeJankRate(
    const std::vector<double>& token_intervals_ms,
    double avg_tpot_ms) {
  if (token_intervals_ms.empty()) return 0.0;
  int janky = 0;
  for (double t : token_intervals_ms) {
    if (IsJankyInterval(t, avg_tpot_ms)) janky++;
  }
  return (janky * 100.0) / token_intervals_ms.size();
}

// ============================================================================
// FullBenchmarkResult 序列化
// ============================================================================

std::string InferenceBenchmark::FullBenchmarkResult::ToJson() const {
  char buf[8192];
  snprintf(buf, sizeof(buf),
    R"({"device_tier":%d,"backend":"%s","model":"%s","overall_score":%.1f,"all_slo_pass":%s,)"
    R"("short_prompt":{"avg_ttft_ms":%.1f,"avg_tpot_ms":%.1f,"avg_tps":%.1f,"jank_pct":%.1f},)"
    R"("medium_prompt":{"avg_ttft_ms":%.1f,"avg_tpot_ms":%.1f,"avg_tps":%.1f,"jank_pct":%.1f},)"
    R"("long_prompt":{"avg_ttft_ms":%.1f,"avg_tpot_ms":%.1f,"avg_tps":%.1f,"jank_pct":%.1f},)"
    R"("multi_turn":{"avg_ttft_ms":%.1f,"avg_tpot_ms":%.1f,"avg_tps":%.1f,"jank_pct":%.1f},)"
    R"("streaming_stress":{"avg_ttft_ms":%.1f,"avg_tpot_ms":%.1f,"avg_tps":%.1f,"jank_pct":%.1f}})",
    static_cast<int>(device_tier), backend_name.c_str(), model_name.c_str(),
    overall_score, all_slo_pass ? "true" : "false",
    short_prompt.avg_ttft_ms, short_prompt.avg_tpot_ms, short_prompt.avg_tokens_per_second, short_prompt.avg_jank_rate_pct,
    medium_prompt.avg_ttft_ms, medium_prompt.avg_tpot_ms, medium_prompt.avg_tokens_per_second, medium_prompt.avg_jank_rate_pct,
    long_prompt.avg_ttft_ms, long_prompt.avg_tpot_ms, long_prompt.avg_tokens_per_second, long_prompt.avg_jank_rate_pct,
    multi_turn.avg_ttft_ms, multi_turn.avg_tpot_ms, multi_turn.avg_tokens_per_second, multi_turn.avg_jank_rate_pct,
    streaming_stress.avg_ttft_ms, streaming_stress.avg_tpot_ms, streaming_stress.avg_tokens_per_second, streaming_stress.avg_jank_rate_pct);
  return std::string(buf);
}

std::string InferenceBenchmark::FullBenchmarkResult::ToMarkdown() const {
  std::ostringstream md;
  md << "# Inference Benchmark Report\n\n"
     << "**Backend**: " << backend_name << " | **Tier**: " << static_cast<int>(device_tier) << "\n\n"
     << "| Scene | TTFT (ms) | TPOT (ms) | Tok/s | Peak Mem (MB) | Jank % | SLO |\n"
     << "|-------|-----------|-----------|-------|---------------|--------|-----|\n"
     << "| Short | " << short_prompt.avg_ttft_ms
     << " | " << short_prompt.avg_tpot_ms
     << " | " << short_prompt.avg_tokens_per_second
     << " | " << short_prompt.max_peak_memory_mb
     << " | " << short_prompt.avg_jank_rate_pct
     << " | " << (short_prompt.meets_slo ? "✅" : "❌") << " |\n"
     << "| Medium | " << medium_prompt.avg_ttft_ms
     << " | " << medium_prompt.avg_tpot_ms
     << " | " << medium_prompt.avg_tokens_per_second
     << " | " << medium_prompt.max_peak_memory_mb
     << " | " << medium_prompt.avg_jank_rate_pct
     << " | " << (medium_prompt.meets_slo ? "✅" : "❌") << " |\n"
     << "| Long | " << long_prompt.avg_ttft_ms
     << " | " << long_prompt.avg_tpot_ms
     << " | " << long_prompt.avg_tokens_per_second
     << " | " << long_prompt.max_peak_memory_mb
     << " | " << long_prompt.avg_jank_rate_pct
     << " | " << (long_prompt.meets_slo ? "✅" : "❌") << " |\n"
     << "| MultiTurn | " << multi_turn.avg_ttft_ms
     << " | " << multi_turn.avg_tpot_ms
     << " | " << multi_turn.avg_tokens_per_second
     << " | " << multi_turn.max_peak_memory_mb
     << " | " << multi_turn.avg_jank_rate_pct
     << " | " << (multi_turn.meets_slo ? "✅" : "❌") << " |\n"
     << "| Streaming | " << streaming_stress.avg_ttft_ms
     << " | " << streaming_stress.avg_tpot_ms
     << " | " << streaming_stress.avg_tokens_per_second
     << " | " << streaming_stress.max_peak_memory_mb
     << " | " << streaming_stress.avg_jank_rate_pct
     << " | " << (streaming_stress.meets_slo ? "✅" : "❌") << " |\n\n"
     << "**SLO Pass**: " << (all_slo_pass ? "✅ ALL PASS" : "❌ FAILURES") << "\n"
     << "**Overall Score**: " << overall_score << "/100\n";
  return md.str();
}

} // namespace solra::core::inference
