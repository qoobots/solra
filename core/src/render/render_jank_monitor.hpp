#pragma once
/// @file render_jank_monitor.hpp
/// @brief 运行时帧抖动 (Jank) 监控器 —— 每帧自动检测与上报
/// @ingroup core/render
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <string>
#include <vector>
#include <chrono>
#include <deque>
#include <mutex>
#include <atomic>

namespace solra::core::render {

// ============================================================================
// Jank 严重级别
// ============================================================================

enum class JankSeverity : uint8_t {
  kNone     = 0,  // 正常帧
  kMicro    = 1,  // 微卡顿 (1.5x-2x 平均帧时间)
  kModerate = 2,  // 中等卡顿 (2x-3x 平均帧时间)
  kSevere   = 3,  // 严重卡顿 (3x-5x 平均帧时间)
  kCritical = 4,  // 致命卡顿 (>5x 或 >100ms)
};

// ============================================================================
// 单帧 Jank 信息
// ============================================================================

struct JankFrameInfo {
  uint64_t frame_number;
  double frame_time_ms;        // 当前帧时间
  double avg_frame_time_ms;    // 滑动窗口平均
  double deviation_ratio;      // 偏离倍数 (frame_time / avg)
  JankSeverity severity;
  std::chrono::steady_clock::time_point timestamp;
};

// ============================================================================
// 滑动窗口 Jank 统计
// ============================================================================

struct JankWindowStats {
  uint64_t total_frames       = 0;
  uint64_t micro_janks        = 0;
  uint64_t moderate_janks     = 0;
  uint64_t severe_janks       = 0;
  uint64_t critical_janks     = 0;

  double avg_frame_time_ms    = 0.0;
  double current_fps          = 0.0;
  double jank_rate_pct        = 0.0;   // (janky_frames / total) * 100
  double smoothness_score     = 100.0; // 0-100 流畅度评分

  std::chrono::steady_clock::time_point window_start;
  double window_duration_seconds = 0.0;
};

// ============================================================================
// Jank 事件回调
// ============================================================================

using JankCallback = std::function<void(const JankFrameInfo& info)>;
using JankSummaryCallback = std::function<void(const JankWindowStats& stats)>;

// ============================================================================
// 运行时 Jank 监控器
// ============================================================================

class JankMonitor {
 public:
  JankMonitor();
  ~JankMonitor();

  // 配置
  /// 设置滑动窗口大小（帧数，默认 600 帧 = 10秒@60fps）
  void SetWindowSize(uint32_t frames) { window_size_ = frames; }

  /// 设置摘要报告间隔（秒，默认 5.0 秒）
  void SetSummaryInterval(double seconds) { summary_interval_seconds_ = seconds; }

  /// 注册卡顿事件回调
  void SetJankCallback(JankCallback cb) { jank_callback_ = std::move(cb); }

  /// 注册周期性摘要回调
  void SetSummaryCallback(JankSummaryCallback cb) { summary_callback_ = std::move(cb); }

  // 运行时接口（每帧调用）
  /// 记录一帧的时间，返回该帧的 Jank 严重级别
  JankSeverity RecordFrame(double frame_time_ms);

  /// 记录一帧（自动从高精度时钟计算帧时间）
  JankSeverity RecordFrameAuto();

  // 查询
  JankWindowStats GetStats() const;
  double GetCurrentFPS() const { return current_fps_.load(); }
  double GetSmoothnessScore() const;
  uint64_t GetTotalJankyFrames() const { return total_janky_frames_.load(); }

  // 重置
  void Reset();

  // 全局单例（可选）
  static JankMonitor& Instance();

 private:
  uint32_t window_size_;
  double summary_interval_seconds_;

  // 滑动窗口
  std::deque<double> frame_times_;
  mutable std::mutex mutex_;

  // 状态
  std::atomic<double> current_fps_{0.0};
  std::atomic<uint64_t> total_janky_frames_{0};
  std::chrono::steady_clock::time_point last_frame_time_;
  std::chrono::steady_clock::time_point last_summary_time_;
  uint64_t frame_count_ = 0;
  double sum_frame_times_ = 0.0;
  uint32_t window_count_ = 0;

  // 卡顿计数
  uint64_t micro_cnt_ = 0, moderate_cnt_ = 0, severe_cnt_ = 0, critical_cnt_ = 0;

  // 回调
  JankCallback jank_callback_;
  JankSummaryCallback summary_callback_;

  // 辅助
  double ComputeAvgFrameTime() const;
  JankSeverity ClassifyFrame(double frame_time_ms, double avg_time_ms);
  void MaybeTriggerSummary();
};

} // namespace solra::core::render
