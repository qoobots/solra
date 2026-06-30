#include "lod_streaming.hpp"

#include <algorithm>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <unordered_set>

namespace fs = std::filesystem;

namespace solra::core::streaming {

// ============================================================================
// AssetManifest 序列化
// ============================================================================

AssetManifest AssetManifest::FromJson(const std::string& json) {
  // TODO(kkfu): 使用 nlohmann/json 或 rapidjson 解析
  AssetManifest m;
  // 占位: 实际需要 JSON 解析库
  return m;
}

AssetManifest AssetManifest::FromBuffer(const uint8_t* data, size_t len) {
  // Binary manifest format (MessagePack or FlatBuffers)
  AssetManifest m;
  return m;
}

std::string AssetManifest::ToJson() const {
  std::ostringstream oss;
  oss << R"({"asset_id":")" << asset_id << R"(","name":")" << asset_name
      << R"(","version":")" << version << R"(","chunks":)"
      << (lod0_chunks.size() + lod1_chunks.size() + lod2_chunks.size() + lod3_chunks.size())
      << "}";
  return oss.str();
}

// ============================================================================
// BandwidthEstimator
// ============================================================================

void BandwidthEstimator::RecordTransfer(uint64_t bytes_transferred,
                                        std::chrono::milliseconds duration) {
  if (duration.count() == 0) return;

  std::lock_guard lock(mutex_);
  double bps = (static_cast<double>(bytes_transferred) * 1000.0) /
               static_cast<double>(duration.count());

  window_.push_back({bps, std::chrono::steady_clock::now()});
  if (window_.size() > kWindowSize) {
    window_.pop_front();
  }
}

double BandwidthEstimator::EstimateBps() const {
  std::lock_guard lock(mutex_);
  if (window_.empty()) return kDefaultBps;

  // 指数加权移动平均 (EWMA)：最近数据权重更高
  double sum = 0.0, weight_sum = 0.0;
  double alpha = 0.8; // EWMA 平滑因子
  double w = 1.0;

  for (auto it = window_.rbegin(); it != window_.rend(); ++it) {
    sum += it->bps * w;
    weight_sum += w;
    w *= alpha;
  }

  return sum / weight_sum;
}

double BandwidthEstimator::HealthScore() const {
  double bps = EstimateBps();

  // WiFi 基准: 10 MB/s = 健康
  // Cellular 基准: 2 MB/s = 健康
  double baseline = (network_type_ == NetworkType::kWiFi) ? 10'000'000.0 : 2'000'000.0;

  double score = bps / baseline;
  return std::clamp(score, 0.0, 1.0);
}

void BandwidthEstimator::SetNetworkType(NetworkType type) {
  std::lock_guard lock(mutex_);
  network_type_ = type;
}

auto BandwidthEstimator::GetNetworkType() const -> NetworkType {
  std::lock_guard lock(mutex_);
  return network_type_;
}

uint32_t BandwidthEstimator::SuggestedConcurrency() const {
  double health = HealthScore();
  if (health > 0.8) return 4;   // 良好: 4并发
  if (health > 0.5) return 2;   // 一般: 2并发
  return 1;                     // 差: 1并发
}

// ============================================================================
// LodPriorityScheduler
// ============================================================================

float LodPriorityScheduler::CalculatePriority(
    const AssetChunk& chunk,
    float camera_distance,
    float view_angle_cosine,
    bool is_user_focus) const {

  float score = 0.0f;

  // 1. 距离因素 (越近优先级越高)
  float max_distance = 100.0f;
  float distance_factor = 1.0f - std::clamp(camera_distance / max_distance, 0.0f, 1.0f);
  score += distance_weight_ * distance_factor;

  // 2. 视角因素 (正面优先级更高)
  float angle_factor = std::clamp(view_angle_cosine, 0.0f, 1.0f);
  score += angle_weight_ * angle_factor;

  // 3. 必须分块 (LOD0 base mesh)
  if (chunk.is_required) {
    score += required_weight_;
  }

  // 4. 依赖因素
  if (!chunk.dependencies.empty()) {
    score += dependency_weight_ * 0.5f; // 有待加载依赖, 提升优先级
  }

  // 5. 用户焦点加成
  if (is_user_focus) {
    score *= 1.5f;
  }

  return std::clamp(score, 0.0f, 1.0f);
}

void LodPriorityScheduler::SortChunks(
    std::vector<AssetChunk*>& chunks,
    const std::function<float(const AssetChunk&)>& camera_distance_fn,
    const std::function<float(const AssetChunk&)>& view_angle_fn) {

  std::sort(chunks.begin(), chunks.end(),
      [&](AssetChunk* a, AssetChunk* b) {
        float pa = CalculatePriority(*a, camera_distance_fn(*a),
                                     view_angle_fn(*a), a->is_required);
        float pb = CalculatePriority(*b, camera_distance_fn(*b),
                                     view_angle_fn(*b), b->is_required);
        a->priority = pa;
        b->priority = pb;
        return pa > pb; // 高优先级在前
      });
}

// ============================================================================
// LodStreamingEngine 内部实现
// ============================================================================

struct LodStreamingEngine::Impl {
  // 清单仓库
  std::unordered_map<std::string, AssetManifest> manifests;
  mutable std::mutex manifest_mutex;

  // 资产状态
  struct AssetState {
    std::string asset_id;
    LodLevel current_lod  = LodLevel::kNone;
    LodLevel target_lod   = LodLevel::kLOD0;
    float camera_distance = 0.0f;
    float view_angle      = 0.0f;

    // 每个分块的状态
    std::unordered_map<uint32_t, AssetChunk*> chunks_by_index;
  };
  std::unordered_map<std::string, AssetState> asset_states;
  mutable std::mutex state_mutex;

  // 下载队列 (按优先级排序)
  struct QueueEntry {
    AssetChunk* chunk = nullptr;
    float priority    = 0.0f;

    bool operator<(const QueueEntry& other) const {
      return priority < other.priority; // 大顶堆
    }
  };
  std::priority_queue<QueueEntry> download_queue;
  mutable std::mutex queue_mutex;

  // 活跃下载 (并发限制)
  std::unordered_set<std::string> active_downloads;
  static constexpr uint32_t kMaxConcurrentDownloads = 4;

  // 相机
  float cam_x = 0, cam_y = 0, cam_z = 0;
  float cam_dx = 0, cam_dy = 0, cam_dz = 1;

  // 回调
  ChunkReadyCallback on_chunk_ready;
  AssetReadyCallback  on_asset_ready;
  ErrorCallback       on_error;

  // 统计
  BandwidthEstimator bandwidth;
  std::string cache_path;
  uint64_t session_bytes   = 0;
  uint32_t session_chunks  = 0;
  double   total_load_ms   = 0.0;
};

// ============================================================================
// LodStreamingEngine
// ============================================================================

LodStreamingEngine::LodStreamingEngine()
    : impl_(std::make_unique<Impl>()) {}

LodStreamingEngine::~LodStreamingEngine() = default;

void LodStreamingEngine::RegisterAssetManifest(const AssetManifest& manifest) {
  std::lock_guard lock(impl_->manifest_mutex);
  impl_->manifests[manifest.asset_id] = manifest;

  // 初始化资产状态
  std::lock_guard slock(impl_->state_mutex);
  auto& state = impl_->asset_states[manifest.asset_id];
  state.asset_id = manifest.asset_id;
  state.target_lod = LodLevel::kLOD0;

  // 索引所有分块
  auto index_chunks = [&](std::vector<AssetChunk>& chunks) {
    for (auto& c : chunks) {
      state.chunks_by_index[c.chunk_index] = &c;
    }
  };
  // Note: 需要从 manifests 中获取 non-const 引用
}

void LodStreamingEngine::UnregisterAsset(const std::string& asset_id) {
  std::lock_guard lock(impl_->manifest_mutex);
  impl_->manifests.erase(asset_id);

  std::lock_guard slock(impl_->state_mutex);
  impl_->asset_states.erase(asset_id);
}

const AssetManifest* LodStreamingEngine::GetManifest(
    const std::string& asset_id) const {
  std::lock_guard lock(impl_->manifest_mutex);
  auto it = impl_->manifests.find(asset_id);
  return (it != impl_->manifests.end()) ? &it->second : nullptr;
}

void LodStreamingEngine::SetLodLevel(const std::string& asset_id,
                                      LodLevel level) {
  std::lock_guard lock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  if (it != impl_->asset_states.end()) {
    it->second.target_lod = level;
  }
}

void LodStreamingEngine::SetCameraPosition(float x, float y, float z) {
  impl_->cam_x = x; impl_->cam_y = y; impl_->cam_z = z;
}

void LodStreamingEngine::SetCameraDirection(float dx, float dy, float dz) {
  float len = std::sqrt(dx*dx + dy*dy + dz*dz);
  if (len > 0.0001f) {
    impl_->cam_dx = dx / len;
    impl_->cam_dy = dy / len;
    impl_->cam_dz = dz / len;
  }
}

void LodStreamingEngine::Update(float delta_seconds) {
  UpdateChunkPriorities();
  DispatchDownloads();
}

void LodStreamingEngine::UpdateChunkPriorities() {
  std::lock_guard slock(impl_->state_mutex);

  for (auto& [id, state] : impl_->asset_states) {
    // 计算相机距离
    float dx = 0 - impl_->cam_x; // 简化: 资产在原点
    float dy = 0 - impl_->cam_y;
    float dz = 0 - impl_->cam_z;
    state.camera_distance = std::sqrt(dx*dx + dy*dy + dz*dz);

    // 自动计算 LOD
    state.target_lod = CalculateLodLevel(state.camera_distance);

    // 视角 (简化)
    float view_dot = (dx * impl_->cam_dx + dy * impl_->cam_dy + dz * impl_->cam_dz);
    state.view_angle = (view_dot / (state.camera_distance + 0.001f));
  }
}

void LodStreamingEngine::DispatchDownloads() {
  uint32_t concurrency = impl_->bandwidth.SuggestedConcurrency();

  while (impl_->active_downloads.size() < concurrency) {
    std::unique_lock qlock(impl_->queue_mutex);
    if (impl_->download_queue.empty()) break;

    auto entry = impl_->download_queue.top();
    impl_->download_queue.pop();
    qlock.unlock();

    if (entry.chunk->state == AssetChunk::State::kQueued) {
      entry.chunk->state = AssetChunk::State::kDownloading;
      impl_->active_downloads.insert(
          entry.chunk->asset_id + ":" + std::to_string(entry.chunk->chunk_index));

      // TODO(kkfu): 实际发起 HTTP Range 请求下载
      // ResumableDownload::Download(
      //     entry.chunk->url, entry.chunk->local_path,
      //     [this, chunk = entry.chunk]() { OnChunkDownloaded(...); },
      //     [this, chunk = entry.chunk](auto err) { OnChunkFailed(...); });
    }
  }
}

void LodStreamingEngine::OnChunkDownloaded(const std::string& asset_id,
                                            uint32_t chunk_index) {
  impl_->active_downloads.erase(asset_id + ":" + std::to_string(chunk_index));
  impl_->session_chunks++;

  // 更新分块状态
  std::lock_guard slock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  if (it != impl_->asset_states.end()) {
    auto cit = it->second.chunks_by_index.find(chunk_index);
    if (cit != it->second.chunks_by_index.end()) {
      cit->second->state = AssetChunk::State::kDownloaded;
    }
  }

  if (impl_->on_chunk_ready) {
    impl_->on_chunk_ready(asset_id, chunk_index,
                           GetCurrentLodLevel(asset_id));
  }

  // 检查资产是否完全就绪
  if (IsAssetReady(asset_id) && impl_->on_asset_ready) {
    impl_->on_asset_ready(asset_id);
  }
}

void LodStreamingEngine::OnChunkFailed(const std::string& asset_id,
                                        uint32_t chunk_index) {
  impl_->active_downloads.erase(asset_id + ":" + std::to_string(chunk_index));

  std::lock_guard slock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  if (it != impl_->asset_states.end()) {
    auto cit = it->second.chunks_by_index.find(chunk_index);
    if (cit != it->second.chunks_by_index.end()) {
      cit->second->state = AssetChunk::State::kFailed;
    }
  }

  if (impl_->on_error) {
    impl_->on_error(asset_id, "Chunk " + std::to_string(chunk_index) +
                    " download failed");
  }
}

bool LodStreamingEngine::IsAssetReady(const std::string& asset_id) const {
  std::lock_guard lock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  if (it == impl_->asset_states.end()) return false;

  const auto& state = it->second;
  for (const auto& [idx, chunk] : state.chunks_by_index) {
    if (chunk->lod_level > state.target_lod) continue; // 不需要的LOD
    if (chunk->state != AssetChunk::State::kReady) return false;
  }
  return true;
}

float LodStreamingEngine::GetAssetProgress(const std::string& asset_id) const {
  std::lock_guard lock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  if (it == impl_->asset_states.end()) return 0.0f;

  const auto& state = it->second;
  uint32_t total = 0, ready = 0;
  for (const auto& [idx, chunk] : state.chunks_by_index) {
    if (chunk->lod_level > state.target_lod) continue;
    total++;
    if (chunk->state >= AssetChunk::State::kDownloaded) ready++;
  }
  return total > 0 ? static_cast<float>(ready) / total : 0.0f;
}

LodLevel LodStreamingEngine::GetCurrentLodLevel(
    const std::string& asset_id) const {
  std::lock_guard lock(impl_->state_mutex);
  auto it = impl_->asset_states.find(asset_id);
  return (it != impl_->asset_states.end()) ? it->second.target_lod : LodLevel::kNone;
}

void LodStreamingEngine::SetChunkReadyCallback(ChunkReadyCallback cb) {
  impl_->on_chunk_ready = std::move(cb);
}

void LodStreamingEngine::SetAssetReadyCallback(AssetReadyCallback cb) {
  impl_->on_asset_ready = std::move(cb);
}

void LodStreamingEngine::SetErrorCallback(ErrorCallback cb) {
  impl_->on_error = std::move(cb);
}

LodStreamingEngine::StreamingStats LodStreamingEngine::GetStats() const {
  StreamingStats s;
  s.active_downloads    = static_cast<uint32_t>(impl_->active_downloads.size());
  s.estimated_bps       = impl_->bandwidth.EstimateBps();
  s.bytes_downloaded_session = impl_->session_bytes;
  s.chunks_loaded_session    = impl_->session_chunks;

  std::lock_guard qlock(impl_->queue_mutex);
  s.queued_chunks = static_cast<uint32_t>(impl_->download_queue.size());
  s.total_bytes_queued = 0; // 粗略

  return s;
}

void LodStreamingEngine::SetCachePath(const std::string& path) {
  impl_->cache_path = path;
  fs::create_directories(path);
}

uint64_t LodStreamingEngine::GetCacheSize() const {
  if (impl_->cache_path.empty()) return 0;
  uint64_t total = 0;
  for (const auto& entry : fs::recursive_directory_iterator(impl_->cache_path)) {
    if (entry.is_regular_file()) {
      total += entry.file_size();
    }
  }
  return total;
}

void LodStreamingEngine::ClearCache() {
  if (!impl_->cache_path.empty()) {
    fs::remove_all(impl_->cache_path);
    fs::create_directories(impl_->cache_path);
  }
}

void LodStreamingEngine::ProcessDependencies(const AssetChunk& chunk) {
  // 依赖分块必须先于当前分块加载
  for (const auto& dep_id : chunk.dependencies) {
    // 提升依赖分块优先级
    std::lock_guard slock(impl_->state_mutex);
    // 遍历所有资产状态找到依赖分块并提升优先级
  }
}

} // namespace solra::core::streaming
