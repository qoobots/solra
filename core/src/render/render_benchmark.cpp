#include "render_benchmark.hpp"

#include <algorithm>
#include <cmath>
#include <numeric>
#include <sstream>
#include <cstdio>
#include <map>

#if defined(__APPLE__)
  #include <sys/sysctl.h>
  #include <mach/mach.h>
#elif defined(_WIN32)
  #include <windows.h>
  #include <dxgi1_6.h>
#elif defined(__ANDROID__)
  #include <android/log.h>
  #include <fstream>
#endif

namespace solra::core::render {

// ============================================================================
// BenchmarkScene 预定义场景
// ============================================================================

BenchmarkScene BenchmarkScene::Empty() {
  return {"Empty", "空场景基线测试",
          10, 1000, 1, 0, 0, 300, 60, false, false, false, false};
}

BenchmarkScene BenchmarkScene::SimpleSponza() {
  return {"Sponza", "简化Sponza场景（200 DC / 100K Tris）",
          200, 100000, 50, 4, 2, 300, 60, false, true, false, false};
}

BenchmarkScene BenchmarkScene::MediumCity() {
  return {"City", "中等城市场景（400 DC / 200K Tris）",
          400, 200000, 100, 8, 4, 300, 60, false, true, false, false};
}

BenchmarkScene BenchmarkScene::HeavyInterior() {
  return {"Interior", "重度室内场景（800 DC / 500K Tris）",
          800, 500000, 200, 12, 6, 300, 60, true, true, true, true};
}

BenchmarkScene BenchmarkScene::StressTest() {
  return {"Stress", "超大压力测试（1500 DC / 1M Tris）",
          1500, 1000000, 500, 16, 8, 300, 60, true, true, true, true};
}

// ============================================================================
// IGpuTimer 平台工厂
// ============================================================================

// TODO(kkfu): 平台特定实现
// - iOS: 使用 MTLCommandBuffer addScheduledHandler + GPUStartTime/GPUEndTime
// - Android: 使用 VK_EXT_calibrated_timestamps / GL_EXT_disjoint_timer_query
// - Windows: 使用 ID3D12QueryHeap D3D12_QUERY_TYPE_TIMESTAMP
// - macOS: 同 iOS Metal

class DefaultGpuTimer : public IGpuTimer {
 public:
  void BeginFrame() override {
    cpu_start_ = std::chrono::high_resolution_clock::now();
  }
  void EndFrame() override {
    auto now = std::chrono::high_resolution_clock::now();
    cpu_time_ms_ = std::chrono::duration<double, std::milli>(now - cpu_start_).count();
    gpu_time_ms_ = cpu_time_ms_ * 0.8; // fallback估算: GPU≈80% of CPU
  }
  double GetGpuTimeMs() const override { return gpu_time_ms_; }
  double GetCpuTimeMs() const override { return cpu_time_ms_; }

 private:
  std::chrono::high_resolution_clock::time_point cpu_start_;
  double cpu_time_ms_ = 0.0;
  double gpu_time_ms_ = 0.0;
};

std::unique_ptr<IGpuTimer> IGpuTimer::CreateForPlatform() {
  return std::make_unique<DefaultGpuTimer>();
}

// ============================================================================
// RenderBenchmark 实现
// ============================================================================

RenderBenchmark::RenderBenchmark(DeviceTier tier)
    : tier_(tier), gpu_timer_(IGpuTimer::CreateForPlatform()) {}

RenderBenchmark::~RenderBenchmark() = default;

void RenderBenchmark::BeginSession(const std::string& session_name) {
  stats_ = BenchmarkSessionStats{};
  frame_history_.clear();
  frame_history_.reserve(2000);
  session_start_ = std::chrono::steady_clock::now();
}

void RenderBenchmark::EndSession() {
  auto now = std::chrono::steady_clock::now();
  stats_.duration_seconds =
      std::chrono::duration<double>(now - session_start_).count();
  ComputeStats();
  stats_.meets_slo = CheckSLO();
}

void RenderBenchmark::RecordFrame(const FrameMetrics& metrics) {
  frame_history_.push_back(metrics);
  stats_.total_frames++;
}

void RenderBenchmark::RecordFrameAuto() {
  gpu_timer_->EndFrame();

  FrameMetrics m;
  m.frame_number = stats_.total_frames;
  m.cpu_time_ms = gpu_timer_->GetCpuTimeMs();
  m.gpu_time_ms = gpu_timer_->GetGpuTimeMs();
  m.total_time_ms = m.cpu_time_ms;

  RecordFrame(m);

  gpu_timer_->BeginFrame();
}

BenchmarkSessionStats RenderBenchmark::RunScene(
    const BenchmarkScene& scene,
    std::function<void(float)> progress_cb) {

  BeginSession(scene.name);

  uint32_t total_frames = scene.warmup_frames + scene.duration_frames;
  for (uint32_t i = 0; i < total_frames; ++i) {
    if (i >= scene.warmup_frames) {
      RecordFrameAuto();
    }

    if (progress_cb && (i % 30 == 0 || i == total_frames - 1)) {
      progress_cb(static_cast<float>(i) / total_frames);
    }
  }

  EndSession();
  return stats_;
}

RenderBenchmark::FullBenchmarkResult RenderBenchmark::RunFullSuite() {
  FullBenchmarkResult result;
  result.device_tier = tier_;
  result.gpu_name = GetGpuName();

  SetDeviceTier(tier_);

  result.empty_scene    = RunScene(BenchmarkScene::Empty());
  result.sponza_scene   = RunScene(BenchmarkScene::SimpleSponza());
  result.city_scene     = RunScene(BenchmarkScene::MediumCity());
  result.interior_scene = RunScene(BenchmarkScene::HeavyInterior());
  result.stress_scene   = RunScene(BenchmarkScene::StressTest());

  result.all_slo_pass = result.empty_scene.meets_slo &&
                         result.sponza_scene.meets_slo &&
                         result.city_scene.meets_slo;

  result.overall_score = 0.0;
  auto weight = [](const BenchmarkSessionStats& s, double w) {
    return s.meets_slo ? w * 20.0 : 0.0;
  };
  result.overall_score += weight(result.empty_scene, 1.0);
  result.overall_score += weight(result.sponza_scene, 2.0);
  result.overall_score += weight(result.city_scene, 1.5);
  result.overall_score += weight(result.interior_scene, 1.0);
  result.overall_score += weight(result.stress_scene, 0.5);

  return result;
}

void RenderBenchmark::ComputeStats() {
  if (frame_history_.empty()) return;

  const size_t n = frame_history_.size();
  stats_.total_frames = n;

  // 帧时间统计
  std::vector<double> frame_times;
  frame_times.reserve(n);

  double sum_time = 0.0, sum_dc = 0.0, sum_tri = 0.0;
  double max_time = 0.0, min_time = 1e9;
  double sum_gpu_util = 0.0;
  double peak_mem = 0.0;

  for (const auto& m : frame_history_) {
    frame_times.push_back(m.total_time_ms);
    sum_time += m.total_time_ms;
    max_time = std::max(max_time, m.total_time_ms);
    min_time = std::min(min_time, m.total_time_ms);

    sum_dc += m.draw_calls;
    sum_tri += m.triangles;
    sum_gpu_util += m.gpu_utilization;
    peak_mem = std::max(peak_mem, static_cast<double>(m.gpu_memory_bytes));
  }

  double avg_time = sum_time / n;
  stats_.avg_frame_time_ms = avg_time;
  stats_.avg_fps = 1000.0 / avg_time;
  stats_.min_fps = 1000.0 / max_time;
  stats_.max_fps = 1000.0 / min_time;

  // 百分位
  std::sort(frame_times.begin(), frame_times.end());
  stats_.p50_frame_time_ms = frame_times[n * 50 / 100];
  stats_.p95_frame_time_ms = frame_times[n * 95 / 100];
  stats_.p99_frame_time_ms = frame_times[n * 99 / 100];

  stats_.avg_draw_calls = static_cast<uint32_t>(sum_dc / n);
  stats_.avg_triangles = static_cast<uint32_t>(sum_tri / n);
  stats_.avg_gpu_util_pct = sum_gpu_util / n;
  stats_.peak_memory_mb = peak_mem / (1024.0 * 1024.0);

  // Jank 检测
  double jank_threshold = avg_time * 2.0;
  for (double t : frame_times) {
    if (t > 33.3) stats_.dropped_frames++;
    if (t > jank_threshold) stats_.janky_frames++;
  }
  stats_.jank_rate_pct = (stats_.janky_frames * 100.0) / n;
}

bool RenderBenchmark::CheckSLO() {
  std::vector<std::string> violations;

  double min_fps_required = slo_.min_fps_low_end;
  switch (tier_) {
    case DeviceTier::kLowEnd:   min_fps_required = slo_.min_fps_low_end; break;
    case DeviceTier::kMidRange:  min_fps_required = slo_.min_fps_mid_range; break;
    case DeviceTier::kHighEnd:   min_fps_required = slo_.min_fps_high_end; break;
    case DeviceTier::kDesktop:   min_fps_required = slo_.min_fps_desktop; break;
  }

  if (stats_.avg_fps < min_fps_required)
    violations.push_back("avg_fps(" + std::to_string(stats_.avg_fps) +
                         ") < required(" + std::to_string(min_fps_required) + ")");
  if (stats_.peak_memory_mb > slo_.max_memory_mb)
    violations.push_back("peak_memory(" + std::to_string(stats_.peak_memory_mb) +
                         "MB) > limit(" + std::to_string(slo_.max_memory_mb) + "MB)");
  if (stats_.p99_frame_time_ms > slo_.target_99p_latency_ms)
    violations.push_back("p99(" + std::to_string(stats_.p99_frame_time_ms) +
                         "ms) > target(" + std::to_string(slo_.target_99p_latency_ms) + "ms)");
  if (stats_.avg_gpu_util_pct > slo_.max_gpu_util_pct)
    violations.push_back("gpu_util(" + std::to_string(stats_.avg_gpu_util_pct) +
                         "%) > limit(" + std::to_string(slo_.max_gpu_util_pct) + "%)");

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

DeviceTier RenderBenchmark::DetectDeviceTier() {
  size_t gpu_mem = GetTotalGpuMemoryMB();

  if (gpu_mem >= 8192) return DeviceTier::kDesktop;
  if (gpu_mem >= 6144) return DeviceTier::kHighEnd;
  if (gpu_mem >= 3072) return DeviceTier::kMidRange;
  return DeviceTier::kLowEnd;
}

std::string RenderBenchmark::GetGpuName() {
#if defined(__APPLE__)
  // Metal:可通过 MTLDevice.name 获取，此处用 sysctl fallback
  char buf[256] = {};
  size_t len = sizeof(buf);
  sysctlbyname("machdep.cpu.brand_string", buf, &len, nullptr, 0);
  return std::string(buf);
#elif defined(_WIN32)
  return "DXGI_GPU"; // 需 IDXGIAdapter::GetDesc
#elif defined(__ANDROID__)
  return "Android_GPU"; // 需 GL_RENDERER / VkPhysicalDeviceProperties
#else
  return "Unknown GPU";
#endif
}

size_t RenderBenchmark::GetTotalGpuMemoryMB() {
#if defined(__APPLE__)
  // macOS/iOS: Metal device.recommendedMaxWorkingSetSize or IOKit
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
  return 2048; // fallback 2GB
#else
  return 4096; // fallback desktop
#endif
}

// ============================================================================
// FullBenchmarkResult 序列化
// ============================================================================

std::string RenderBenchmark::FullBenchmarkResult::ToJson() const {
  char buf[4096];
  snprintf(buf, sizeof(buf),
    R"({"device_tier":%d,"gpu_name":"%s","overall_score":%.1f,"all_slo_pass":%s})",
    static_cast<int>(device_tier), gpu_name.c_str(), overall_score,
    all_slo_pass ? "true" : "false");
  return std::string(buf);
}

std::string RenderBenchmark::FullBenchmarkResult::ToMarkdown() const {
  std::ostringstream md;
  md << "# GPU Render Benchmark Report\n\n"
     << "| Metric | Empty | Sponza | City | Interior | Stress |\n"
     << "|--------|-------|--------|------|----------|--------|\n"
     << "| Avg FPS | " << empty_scene.avg_fps << " | " << sponza_scene.avg_fps
     << " | " << city_scene.avg_fps << " | " << interior_scene.avg_fps
     << " | " << stress_scene.avg_fps << " |\n"
     << "| P99 (ms) | " << empty_scene.p99_frame_time_ms
     << " | " << sponza_scene.p99_frame_time_ms
     << " | " << city_scene.p99_frame_time_ms
     << " | " << interior_scene.p99_frame_time_ms
     << " | " << stress_scene.p99_frame_time_ms << " |\n"
     << "| Memory (MB) | " << empty_scene.peak_memory_mb
     << " | " << sponza_scene.peak_memory_mb
     << " | " << city_scene.peak_memory_mb
     << " | " << interior_scene.peak_memory_mb
     << " | " << stress_scene.peak_memory_mb << " |\n"
     << "| Jank % | " << empty_scene.jank_rate_pct
     << " | " << sponza_scene.jank_rate_pct
     << " | " << city_scene.jank_rate_pct
     << " | " << interior_scene.jank_rate_pct
     << " | " << stress_scene.jank_rate_pct << " |\n\n"
     << "**Device**: " << gpu_name << " (Tier " << static_cast<int>(device_tier) << ")\n"
     << "**SLO Pass**: " << (all_slo_pass ? "✅" : "❌") << "\n"
     << "**Overall Score**: " << overall_score << "/100\n";
  return md.str();
}

} // namespace solra::core::render
