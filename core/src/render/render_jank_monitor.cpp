#include "render_jank_monitor.hpp"

#include <algorithm>
#include <cmath>
#include <spdlog/spdlog.h>

namespace solra::core::render {

// ============================================================================
// JankMonitor 构造/析构
// ============================================================================

JankMonitor::JankMonitor()
    : window_size_(600), summary_interval_seconds_(5.0) {
  last_frame_time_ = std::chrono::steady_clock::now();
  last_summary_time_ = last_frame_time_;
}

JankMonitor::~JankMonitor() = default;

// ============================================================================
// 帧记录
// ============================================================================

JankSeverity JankMonitor::RecordFrame(double frame_time_ms) {
  std::lock_guard<std::mutex> lock(mutex_);

  // 加入滑动窗口
  frame_times_.push_back(frame_time_ms);
  sum_frame_times_ += frame_time_ms;
  frame_count_++;

  // 窗口溢出处理
  while (frame_times_.size() > window_size_) {
    sum_frame_times_ -= frame_times_.front();
    frame_times_.pop_front();
  }

  // 计算当前平均帧时间
  double avg_time = ComputeAvgFrameTime();
  if (avg_time > 0.0) {
    current_fps_.store(1000.0 / avg_time, std::memory_order_relaxed);
  }

  // 分类卡顿级别
  JankSeverity severity = ClassifyFrame(frame_time_ms, avg_time);

  // 统计
  switch (severity) {
    case JankSeverity::kMicro:    micro_cnt_++;    total_janky_frames_++; break;
    case JankSeverity::kModerate: moderate_cnt_++;  total_janky_frames_++; break;
    case JankSeverity::kSevere:   severe_cnt_++;    total_janky_frames_++; break;
    case JankSeverity::kCritical: critical_cnt_++;  total_janky_frames_++; break;
    case JankSeverity::kNone: break;
  }

  // 触发回调
  if (severity >= JankSeverity::kModerate && jank_callback_) {
    JankFrameInfo info;
    info.frame_number = frame_count_;
    info.frame_time_ms = frame_time_ms;
    info.avg_frame_time_ms = avg_time;
    info.deviation_ratio = avg_time > 0.0 ? frame_time_ms / avg_time : 1.0;
    info.severity = severity;
    info.timestamp = std::chrono::steady_clock::now();
    jank_callback_(info);
  }

  // 周期性摘要
  MaybeTriggerSummary();

  return severity;
}

JankSeverity JankMonitor::RecordFrameAuto() {
  auto now = std::chrono::steady_clock::now();
  double elapsed = std::chrono::duration<double, std::milli>(
      now - last_frame_time_).count();
  last_frame_time_ = now;
  return RecordFrame(elapsed);
}

// ============================================================================
// 查询
// ============================================================================

JankWindowStats JankMonitor::GetStats() const {
  std::lock_guard<std::mutex> lock(mutex_);

  JankWindowStats stats;
  stats.total_frames = frame_times_.size();
  stats.micro_janks = micro_cnt_;
  stats.moderate_janks = moderate_cnt_;
  stats.severe_janks = severe_cnt_;
  stats.critical_janks = critical_cnt_;

  double avg = ComputeAvgFrameTime();
  stats.avg_frame_time_ms = avg;
  stats.current_fps = avg > 0.0 ? 1000.0 / avg : 0.0;

  uint64_t total_jank = micro_cnt_ + moderate_cnt_ + severe_cnt_ + critical_cnt_;
  if (stats.total_frames > 0) {
    stats.jank_rate_pct = (static_cast<double>(total_jank) / stats.total_frames) * 100.0;
  }

  // 流畅度评分 (0-100)
  // 基础分 100，每次卡顿扣分
  double penalty = 0.0;
  penalty += micro_cnt_ * 0.1;      // 微卡顿扣 0.1
  penalty += moderate_cnt_ * 0.5;   // 中等扣 0.5
  penalty += severe_cnt_ * 2.0;     // 严重扣 2.0
  penalty += critical_cnt_ * 5.0;   // 致命扣 5.0
  stats.smoothness_score = std::max(0.0, 100.0 - penalty);

  stats.window_start = std::chrono::steady_clock::now() -
      std::chrono::duration_cast<std::chrono::steady_clock::duration>(
          std::chrono::duration<double>(avg * stats.total_frames / 1000.0));
  stats.window_duration_seconds = avg * stats.total_frames / 1000.0;

  return stats;
}

double JankMonitor::GetSmoothnessScore() const {
  return GetStats().smoothness_score;
}

void JankMonitor::Reset() {
  std::lock_guard<std::mutex> lock(mutex_);
  frame_times_.clear();
  sum_frame_times_ = 0.0;
  frame_count_ = 0;
  micro_cnt_ = moderate_cnt_ = severe_cnt_ = critical_cnt_ = 0;
  total_janky_frames_.store(0);
  current_fps_.store(0.0);
  last_frame_time_ = std::chrono::steady_clock::now();
  last_summary_time_ = last_frame_time_;
}

// ============================================================================
// 全局单例
// ============================================================================

JankMonitor& JankMonitor::Instance() {
  static JankMonitor instance;
  return instance;
}

// ============================================================================
// 内部辅助
// ============================================================================

double JankMonitor::ComputeAvgFrameTime() const {
  if (frame_times_.empty()) return 0.0;
  return sum_frame_times_ / frame_times_.size();
}

JankSeverity JankMonitor::ClassifyFrame(double frame_time_ms, double avg_time_ms) {
  if (avg_time_ms <= 0.0) return JankSeverity::kNone;

  double ratio = frame_time_ms / avg_time_ms;

  // 绝对阈值：超过 100ms 必定卡顿
  if (frame_time_ms > 100.0) {
    if (ratio > 5.0) return JankSeverity::kCritical;
    if (ratio > 3.0) return JankSeverity::kSevere;
    return JankSeverity::kModerate;
  }

  // 相对阈值
  if (ratio > 5.0)  return JankSeverity::kCritical;
  if (ratio > 3.0)  return JankSeverity::kSevere;
  if (ratio > 2.0)  return JankSeverity::kModerate;
  if (ratio > 1.5)  return JankSeverity::kMicro;

  return JankSeverity::kNone;
}

void JankMonitor::MaybeTriggerSummary() {
  if (!summary_callback_) return;

  auto now = std::chrono::steady_clock::now();
  double elapsed = std::chrono::duration<double>(now - last_summary_time_).count();

  if (elapsed >= summary_interval_seconds_) {
    last_summary_time_ = now;
    summary_callback_(GetStats());
  }
}

} // namespace solra::core::render
