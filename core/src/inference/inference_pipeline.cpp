#include "inference_pipeline.hpp"

#include <algorithm>
#include <atomic>
#include <regex>
#include <queue>
#include <sstream>
#include <unordered_set>

namespace solra::core::inference {

// ============================================================================
// 任务队列 (优先级排序)
// ============================================================================

struct PriorityCompare {
  bool operator()(const InferenceRequest& a, const InferenceRequest& b) const {
    // 先按优先级倒序 (0=最高), 再按提交时间正序
    if (a.priority != b.priority)
      return static_cast<int>(a.priority) > static_cast<int>(b.priority);
    return a.submit_time > b.submit_time;
  }
};

// ============================================================================
// 内部实现
// ============================================================================

struct InferencePipeline::Impl {
  // 线程池
  size_t num_workers;
  std::vector<std::thread> workers;
  std::atomic<bool> running{false};
  std::atomic<bool> paused{false};

  // 任务队列
  std::priority_queue<InferenceRequest,
                      std::vector<InferenceRequest>,
                      PriorityCompare> task_queue;
  std::mutex queue_mutex;
  std::condition_variable queue_cv;

  // 正在处理的任务
  std::unordered_set<std::string> active_requests;
  std::mutex active_mutex;

  // 回调注册
  struct StreamContext {
    std::string request_id;
    StreamTokenCallback on_token;
    InferenceCallback on_complete;
    ErrorCallback on_error;
  };
  std::unordered_map<std::string, StreamContext> stream_callbacks;
  std::mutex callback_mutex;

  // 后处理链
  std::vector<std::unique_ptr<IPostProcessor>> post_processors;
  bool post_processing_enabled = true;

  // 取消集合
  std::unordered_set<std::string> cancelled;
  std::mutex cancel_mutex;

  // 统计
  struct Stats {
    std::atomic<uint64_t> total{0};
    std::atomic<uint64_t> completed{0};
    std::atomic<uint64_t> failed{0};
    std::atomic<uint64_t> rejected{0};
    std::atomic<double> total_latency_ms{0.0};
    std::atomic<double> total_ttft_ms{0.0};
    std::atomic<double> total_tpot_ms{0.0};
    std::atomic<double> total_tps{0.0};
  };
  Stats stats;

  std::string model_name = "default";

  // 执行推理
  InferenceResult ExecuteInference(const InferenceRequest& req);
};

// ============================================================================
// InferencePipeline
// ============================================================================

InferencePipeline::InferencePipeline(size_t worker_threads)
    : impl_(std::make_unique<Impl>()) {
  impl_->num_workers = worker_threads > 0 ? worker_threads
                                           : std::thread::hardware_concurrency();
  impl_->running = true;

  // 启动工作线程
  impl_->workers.reserve(impl_->num_workers);
  for (size_t i = 0; i < impl_->num_workers; ++i) {
    impl_->workers.emplace_back(&InferencePipeline::WorkerLoop, this);
  }
}

InferencePipeline::~InferencePipeline() {
  impl_->running = false;
  impl_->queue_cv.notify_all();
  for (auto& w : impl_->workers) {
    if (w.joinable()) w.join();
  }
}

InferenceResult InferencePipeline::Infer(const InferenceRequest& request) {
  auto future = InferAsync(request);
  return future.get();
}

std::future<InferenceResult> InferencePipeline::InferAsync(
    const InferenceRequest& request) {
  auto promise = std::make_shared<std::promise<InferenceResult>>();
  auto future = promise->get_future();

  auto req = request;
  req.submit_time = std::chrono::steady_clock::now();

  {
    std::lock_guard lock(impl_->queue_mutex);
    impl_->task_queue.push(req);
    impl_->stats.total++;
  }
  impl_->queue_cv.notify_one();

  // 启动一个监控线程等待结果
  std::thread([this, promise, req, future = std::move(future)]() mutable {
    try {
      auto result = future.get();
      promise->set_value(result);
    } catch (const std::exception& e) {
      InferenceResult err;
      err.request_id = req.request_id;
      err.success = false;
      err.error_message = e.what();
      promise->set_value(err);
    }
  }).detach();

  return promise->get_future();
}

void InferencePipeline::InferStream(
    const InferenceRequest& request,
    StreamTokenCallback on_token,
    InferenceCallback on_complete,
    ErrorCallback on_error) {

  // 注册流式回调
  {
    std::lock_guard lock(impl_->callback_mutex);
    impl_->stream_callbacks[request.request_id] = {
        request.request_id, on_token, on_complete, on_error};
  }

  auto req = request;
  req.submit_time = std::chrono::steady_clock::now();

  {
    std::lock_guard lock(impl_->queue_mutex);
    impl_->task_queue.push(req);
    impl_->stats.total++;
  }
  impl_->queue_cv.notify_one();
}

bool InferencePipeline::CancelRequest(const std::string& request_id) {
  std::lock_guard lock(impl_->cancel_mutex);
  impl_->cancelled.insert(request_id);
  return true;
}

void InferencePipeline::WaitAll(std::chrono::milliseconds timeout) {
  auto deadline = std::chrono::steady_clock::now() + timeout;

  while (std::chrono::steady_clock::now() < deadline) {
    std::unique_lock lock(impl_->queue_mutex);
    if (impl_->task_queue.empty()) {
      std::lock_guard alock(impl_->active_mutex);
      if (impl_->active_requests.empty()) break;
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
  }
}

void InferencePipeline::Pause() { impl_->paused = true; }

void InferencePipeline::Resume() {
  impl_->paused = false;
  impl_->queue_cv.notify_all();
}

void InferencePipeline::ClearQueue() {
  std::lock_guard lock(impl_->queue_mutex);
  impl_->task_queue = {};
  impl_->stats.rejected += impl_->task_queue.size();
}

void InferencePipeline::AddPostProcessor(
    std::unique_ptr<IPostProcessor> processor) {
  impl_->post_processors.push_back(std::move(processor));
}

void InferencePipeline::RemovePostProcessor(const std::string& name) {
  auto& pps = impl_->post_processors;
  pps.erase(std::remove_if(pps.begin(), pps.end(),
      [&](const auto& p) { return p->Name() == name; }), pps.end());
}

void InferencePipeline::SetPostProcessingEnabled(bool enabled) {
  impl_->post_processing_enabled = enabled;
}

InferencePipeline::PipelineStats InferencePipeline::GetStats() const {
  PipelineStats s;
  s.total_requests     = impl_->stats.total.load();
  s.completed_requests = impl_->stats.completed.load();
  s.failed_requests    = impl_->stats.failed.load();
  s.rejected_requests  = impl_->stats.rejected.load();
  s.active_workers     = 0; // approximate

  auto completed = s.completed_requests;
  if (completed > 0) {
    s.avg_latency_ms  = impl_->stats.total_latency_ms / completed;
    s.avg_ttft_ms     = impl_->stats.total_ttft_ms / completed;
    s.avg_tpot_ms     = impl_->stats.total_tpot_ms / completed;
    s.avg_tokens_per_sec = impl_->stats.total_tps / completed;
  }

  std::lock_guard lock(impl_->queue_mutex);
  s.queue_depth = impl_->task_queue.size();
  s.is_paused   = impl_->paused.load();

  return s;
}

void InferencePipeline::SetModelName(const std::string& name) {
  impl_->model_name = name;
}

std::string InferencePipeline::GetModelName() const {
  return impl_->model_name;
}

// ============================================================================
// 工作循环
// ============================================================================

void InferencePipeline::WorkerLoop() {
  while (impl_->running) {
    InferenceRequest req;

    {
      std::unique_lock lock(impl_->queue_mutex);
      impl_->queue_cv.wait(lock, [this] {
        return !impl_->task_queue.empty() || !impl_->running;
      });

      if (!impl_->running) break;
      if (impl_->paused) continue;

      req = impl_->task_queue.top();
      impl_->task_queue.pop();
    }

    // 检查是否被取消
    {
      std::lock_guard lock(impl_->cancel_mutex);
      if (impl_->cancelled.count(req.request_id)) {
        impl_->cancelled.erase(req.request_id);
        impl_->stats.rejected++;
        continue;
      }
    }

    // 标记活跃
    {
      std::lock_guard lock(impl_->active_mutex);
      impl_->active_requests.insert(req.request_id);
    }

    // 执行推理
    auto result = impl_->ExecuteInference(req);

    // 后处理
    if (impl_->post_processing_enabled) {
      InferenceResult::PostProcessResult combined;
      for (auto& pp : impl_->post_processors) {
        auto pp_result = pp->Process(result);
        if (!pp_result.safety_pass) combined.safety_pass = false;
        if (pp_result.sensitive_blocked) combined.sensitive_blocked = true;
        if (!pp_result.filtered_text.empty()) combined.filtered_text = pp_result.filtered_text;
        combined.safety_score = std::min(combined.safety_score, pp_result.safety_score);
      }
      if (!combined.filtered_text.empty()) {
        result.text = combined.filtered_text;
      }
      result.post_process = combined;
    }

    // 触发完成回调
    {
      std::lock_guard lock(impl_->callback_mutex);
      auto it = impl_->stream_callbacks.find(req.request_id);
      if (it != impl_->stream_callbacks.end()) {
        if (it->second.on_complete) {
          it->second.on_complete(result);
        }
        impl_->stream_callbacks.erase(it);
      }
    }

    // 更新统计
    if (result.success) {
      impl_->stats.completed++;
      impl_->stats.total_latency_ms.store(
          impl_->stats.total_latency_ms + result.total_duration_ms);
      impl_->stats.total_ttft_ms.store(
          impl_->stats.total_ttft_ms + result.time_to_first_token_ms);
      impl_->stats.total_tpot_ms.store(
          impl_->stats.total_tpot_ms + result.time_per_output_token_ms);
      impl_->stats.total_tps.store(
          impl_->stats.total_tps + result.tokens_per_second);
    } else {
      impl_->stats.failed++;
    }

    // 移除活跃标记
    {
      std::lock_guard lock(impl_->active_mutex);
      impl_->active_requests.erase(req.request_id);
    }
  }
}

InferenceResult InferencePipeline::Impl::ExecuteInference(
    const InferenceRequest& req) {
  InferenceResult result;
  result.request_id = req.request_id;

  auto t_start = std::chrono::high_resolution_clock::now();

  // TODO(kkfu): 调用实际的推理引擎 (LlamaEngine)
  // 这里是占位实现

  // 检查超时
  auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
      std::chrono::high_resolution_clock::now() - t_start);
  if (elapsed > req.timeout) {
    result.success = false;
    result.timeout_occurred = true;
    result.error_message = "Inference timeout";
    return result;
  }

  result.text = "InferencePipeline placeholder — "
                "connect to LlamaEngine for actual inference";
  result.tokens_generated = 10;
  result.total_duration_ms =
      std::chrono::duration<double, std::milli>(
          std::chrono::high_resolution_clock::now() - t_start).count();

  return result;
}

// ============================================================================
// SafetyPostProcessor
// ============================================================================

InferenceResult::PostProcessResult SafetyPostProcessor::Process(
    const InferenceResult& result) {
  InferenceResult::PostProcessResult r;
  r.safety_pass = true;
  r.safety_score = 1.0f;

  for (const auto& kw : blocked_keywords_) {
    if (result.text.find(kw) != std::string::npos) {
      r.safety_pass = false;
      r.sensitive_blocked = true;
      r.safety_score = 0.0f;
      r.filtered_text = "[内容已过滤]";
      break;
    }
  }

  return r;
}

void SafetyPostProcessor::SetBlockedKeywords(
    const std::vector<std::string>& keywords) {
  blocked_keywords_ = keywords;
}

void SafetyPostProcessor::SetBlockedRegex(const std::string& pattern) {
  // blocked_regex_ = std::regex(pattern, std::regex::ECMAScript);
}

// ============================================================================
// FormatPostProcessor
// ============================================================================

InferenceResult::PostProcessResult FormatPostProcessor::Process(
    const InferenceResult& result) {
  InferenceResult::PostProcessResult r;
  r.safety_pass = true;
  r.safety_score = 1.0f;

  // 清理常见格式问题
  std::string cleaned = result.text;

  // 移除多余空白
  auto trim = [](std::string& s) {
    s.erase(0, s.find_first_not_of(" \t\n\r"));
    s.erase(s.find_last_not_of(" \t\n\r") + 1);
  };
  trim(cleaned);

  // 合并连续换行
  std::string merged;
  bool prev_nl = false;
  for (char c : cleaned) {
    if (c == '\n') {
      if (!prev_nl) { merged += c; prev_nl = true; }
    } else {
      merged += c; prev_nl = false;
    }
  }
  r.filtered_text = merged;

  return r;
}

} // namespace solra::core::inference
