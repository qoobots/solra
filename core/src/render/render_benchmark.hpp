#pragma once
/// @file render_benchmark.hpp
/// @brief GPU渲染性能基准测试框架 —— 多场景基准收集与SLO验证
/// @ingroup core/render
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <string>
#include <vector>
#include <chrono>
#include <memory>

namespace solra::core::render {

// ============================================================================
// 性能等级定义
// ============================================================================

enum class DeviceTier : uint8_t {
  kLowEnd,      // <3GB RAM, 低端GPU (Adreno 6xx / Mali-G52)
  kMidRange,    // 3-6GB RAM, 中端GPU (Adreno 7xx / Mali-G76 + / Apple A13-15)
  kHighEnd,     // >6GB RAM, 旗舰GPU (Adreno 8xx / Mali-G710+ / Apple A16+)
  kDesktop,     // 桌面级GPU (NVidia RTX / AMD RDNA / Apple M2 Max+)
};

// ============================================================================
// SLO 基准 (Service Level Objective)
// ============================================================================

struct FrameTimingSLO {
  double min_fps_low_end    = 30.0;   // 低端机 ≥30fps
  double min_fps_mid_range  = 45.0;   // 中端机 ≥45fps
  double min_fps_high_end   = 60.0;   // 旗舰机 ≥60fps
  double min_fps_desktop    = 90.0;   // 桌面 ≥90fps

  double max_frame_time_ms  = 33.3;   // 首帧 ≤33ms (30fps)
  double max_gpu_util_pct   = 85.0;   // GPU占用 ≤85%
  double max_memory_mb      = 512.0;  // 内存 ≤512MB

  double target_99p_latency_ms = 50.0; // P99延迟 ≤50ms
  double target_draw_calls     = 500.0; // 每帧 ≤500 draw calls
  double target_triangle_count = 200000.0; // 每帧 ≤200K三角形
};

// ============================================================================
// 单帧性能指标
// ============================================================================

struct FrameMetrics {
  uint64_t frame_number = 0;

  double cpu_time_ms     = 0.0;  // CPU耗时（含驱动开销）
  double gpu_time_ms     = 0.0;  // GPU耗时（GPU timestamp query）
  double total_time_ms   = 0.0;  // 总帧时间

  uint32_t draw_calls    = 0;    // Draw call 数量
  uint32_t triangles     = 0;    // 三角形数量
  uint32_t vertices      = 0;    // 顶点数量

  uint32_t texture_binds = 0;    // 纹理绑定数
  uint32_t shader_switches = 0;  // 着色器切换数
  uint32_t state_changes = 0;    // 渲染状态变更数

  size_t gpu_memory_bytes  = 0;  // GPU显存使用
  size_t cpu_memory_bytes  = 0;  // CPU内存使用
  double gpu_utilization   = 0.0; // GPU利用率 (0-100)
};

// ============================================================================
// 基准会话统计
// ============================================================================

struct BenchmarkSessionStats {
  uint64_t total_frames      = 0;
  double duration_seconds     = 0.0;

  double avg_fps              = 0.0;
  double min_fps              = 0.0;
  double max_fps              = 0.0;

  double avg_frame_time_ms    = 0.0;
  double p50_frame_time_ms    = 0.0;
  double p95_frame_time_ms    = 0.0;
  double p99_frame_time_ms    = 0.0;

  uint32_t avg_draw_calls     = 0;
  uint32_t avg_triangles      = 0;

  double avg_gpu_util_pct     = 0.0;
  double peak_memory_mb       = 0.0;

  // 掉帧统计
  uint64_t dropped_frames     = 0;
  uint64_t janky_frames       = 0;     // > 2x average frame time
  double jank_rate_pct        = 0.0;

  bool meets_slo              = false;
  std::string slo_violations;
};

// ============================================================================
// 基准场景定义
// ============================================================================

struct BenchmarkScene {
  std::string name;
  std::string description;

  // 场景负载参数
  uint32_t draw_calls_target   = 200;    // 目标 draw call 数
  uint32_t triangle_target     = 100000; // 目标三角形数
  uint32_t material_count      = 50;     // 材质种类
  uint32_t light_count         = 4;      // 动态光源数
  uint32_t shadow_casters      = 2;      // 投射阴影光源数

  uint32_t duration_frames     = 300;    // 采集帧数 (~5秒@60fps)
  uint32_t warmup_frames       = 60;     // 预热帧数

  bool use_transparency        = false;  // 是否使用半透明
  bool use_post_processing     = false;  // 是否使用后处理
  bool use_skinning            = false;  // 是否使用骨骼蒙皮
  bool use_morph_targets       = false;  // 是否使用表情混合形

  // 预定义场景
  static BenchmarkScene Empty();
  static BenchmarkScene SimpleSponza();       // ~200 draw calls, 100K tris
  static BenchmarkScene MediumCity();         // ~400 draw calls, 200K tris
  static BenchmarkScene HeavyInterior();      // ~800 draw calls, 500K tris
  static BenchmarkScene StressTest();         // ~1500 draw calls, 1M tris
};

// ============================================================================
// GPU 性能查询接口（平台实现）
// ============================================================================

class IGpuTimer {
 public:
  virtual ~IGpuTimer() = default;

  virtual void BeginFrame() = 0;
  virtual void EndFrame() = 0;
  virtual double GetGpuTimeMs() const = 0;
  virtual double GetCpuTimeMs() const = 0;

  // 平台工厂
  static std::unique_ptr<IGpuTimer> CreateForPlatform();
};

// ============================================================================
// 性能基准测试器
// ============================================================================

class RenderBenchmark {
 public:
  explicit RenderBenchmark(DeviceTier tier);
  ~RenderBenchmark();

  // 配置
  void SetDeviceTier(DeviceTier tier) { tier_ = tier; }
  void SetFrameTimingSLO(const FrameTimingSLO& slo) { slo_ = slo; }

  // 会话管理
  void BeginSession(const std::string& session_name);
  void EndSession();

  // 逐帧指标收集（每帧调用）
  void RecordFrame(const FrameMetrics& metrics);
  void RecordFrameAuto();  // 自动从GPU timer查询

  // 场景基准测试
  BenchmarkSessionStats RunScene(const BenchmarkScene& scene,
                                  std::function<void(float)> progress_cb = {});

  // 完整基准套件
  struct FullBenchmarkResult {
    DeviceTier device_tier;
    std::string gpu_name;
    std::string driver_version;

    BenchmarkSessionStats empty_scene;
    BenchmarkSessionStats sponza_scene;
    BenchmarkSessionStats city_scene;
    BenchmarkSessionStats interior_scene;
    BenchmarkSessionStats stress_scene;

    bool all_slo_pass;
    double overall_score;  // 0-100 综合性能评分

    std::string ToJson() const;
    std::string ToMarkdown() const;
  };

  FullBenchmarkResult RunFullSuite();

  // 结果查询
  const BenchmarkSessionStats& GetCurrentStats() const { return stats_; }
  const std::vector<FrameMetrics>& GetFrameHistory() const { return frame_history_; }

  // 环境检测
  static DeviceTier DetectDeviceTier();
  static std::string GetGpuName();
  static size_t GetTotalGpuMemoryMB();

 private:
  DeviceTier tier_;
  FrameTimingSLO slo_;

  std::unique_ptr<IGpuTimer> gpu_timer_;

  BenchmarkSessionStats stats_;
  std::vector<FrameMetrics> frame_history_;

  std::chrono::steady_clock::time_point session_start_;

  void ComputeStats();
  bool CheckSLO();
};

} // namespace solra::core::render
