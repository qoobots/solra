#pragma once
/// @file lod_streaming.hpp
/// @brief 资产分块加载 (LOD按需流式) —— 优先级调度 + 带宽自适应
/// @ingroup core/streaming
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <unordered_map>
#include <queue>
#include <atomic>
#include <mutex>
#include <chrono>

namespace solra::core::streaming {

// ============================================================================
// LOD 级别
// ============================================================================

enum class LodLevel : uint8_t {
  kLOD0 = 0,  // 最高细节 (近景 <5m)
  kLOD1 = 1,  // 中等细节 (中景 5-20m)
  kLOD2 = 2,  // 低细节 (远景 20-50m)
  kLOD3 = 3,  // 最低细节 (超远景 >50m, billboard/impostor)
  kNone = 4,  // 不加载
};

// 距离阈值 (米)
constexpr float kLodDistanceLOD0 = 5.0f;
constexpr float kLodDistanceLOD1 = 20.0f;
constexpr float kLodDistanceLOD2 = 50.0f;

inline LodLevel CalculateLodLevel(float distance_meters) {
  if (distance_meters <= kLodDistanceLOD0) return LodLevel::kLOD0;
  if (distance_meters <= kLodDistanceLOD1) return LodLevel::kLOD1;
  if (distance_meters <= kLodDistanceLOD2) return LodLevel::kLOD2;
  return LodLevel::kLOD3;
}

// ============================================================================
// 资产分块定义
// ============================================================================

struct AssetChunk {
  std::string asset_id;        // 所属资产
  uint32_t    chunk_index;     // 分块序号 (0-based)
  LodLevel    lod_level;       // LOD级别

  std::string url;             // 下载URL
  std::string local_path;      // 本地缓存路径
  std::string content_hash;    // SHA256

  uint64_t    size_bytes;      // 文件大小
  bool        is_required;     // 是否为必须分块 (LOD0 base mesh)

  // 依赖 (必须先于本分块加载)
  std::vector<std::string> dependencies;

  // 加载状态
  enum class State : uint8_t {
    kNotLoaded,       // 未加载
    kQueued,          // 队列中
    kDownloading,     // 下载中
    kDownloaded,      // 已下载
    kParsed,          // 已解析 (纹理解码/几何解压)
    kReady,           // 就绪可用
    kFailed,          // 失败
  };
  State state = State::kNotLoaded;

  float priority = 0.0f;       // 动态优先级
  std::chrono::steady_clock::time_point queue_time;
};

// ============================================================================
// 资产清单
// ============================================================================

struct AssetManifest {
  std::string asset_id;
  std::string asset_name;
  std::string version;

  // 各LOD级别的分块列表
  std::vector<AssetChunk> lod0_chunks;   // 必须分块: base mesh + main texture
  std::vector<AssetChunk> lod1_chunks;   // 中景: simplified mesh + mip2-4
  std::vector<AssetChunk> lod2_chunks;   // 远景: impostor + mip4-6
  std::vector<AssetChunk> lod3_chunks;   // 超远景: billboard

  // 总大小
  uint64_t total_size_bytes = 0;

  // 元数据
  uint32_t material_count = 0;

  // 序列化
  static AssetManifest FromJson(const std::string& json);
  static AssetManifest FromBuffer(const uint8_t* data, size_t len);
  std::string ToJson() const;
};

// ============================================================================
// 带宽自适应控制器
// ============================================================================

class BandwidthEstimator {
 public:
  // 记录下载完成事件
  void RecordTransfer(uint64_t bytes_transferred,
                      std::chrono::milliseconds duration);

  // 估计当前带宽 (bytes/sec)
  double EstimateBps() const;

  // 估计健康度: 0=极差, 1=良好
  double HealthScore() const;

  // 网络类型
  enum class NetworkType { kWiFi, kCellular, kEthernet, kUnknown };
  void SetNetworkType(NetworkType type);
  NetworkType GetNetworkType() const;

  // 建议并发下载数
  uint32_t SuggestedConcurrency() const;

 private:
  struct WindowEntry {
    double bps;
    std::chrono::steady_clock::time_point timestamp;
  };
  std::deque<WindowEntry> window_;  // 滑动窗口
  static constexpr size_t kWindowSize = 16;
  static constexpr double kDefaultBps = 1'000'000.0; // 1 MB/s

  NetworkType network_type_ = NetworkType::kUnknown;
  mutable std::mutex mutex_;
};

// ============================================================================
// 优先级调度器
// ============================================================================

class LodPriorityScheduler {
 public:
  // 计算分块优先级 (0=最低, 1=最高)
  float CalculatePriority(const AssetChunk& chunk,
                          float camera_distance,
                          float view_angle_cosine,  // 视角余弦 (1=正面, 0=背面)
                          bool is_user_focus) const;

  // 排序分块 (高优先级在前)
  void SortChunks(std::vector<AssetChunk*>& chunks,
                  const std::function<float(const AssetChunk&)>& camera_distance_fn,
                  const std::function<float(const AssetChunk&)>& view_angle_fn);

 private:
  // 权重配置
  float distance_weight_    = 0.40f;  // 距离权重
  float angle_weight_       = 0.25f;  // 视角权重
  float required_weight_    = 0.20f;  // 必须分块权重
  float dependency_weight_  = 0.15f;  // 依赖权重
};

// ============================================================================
// 流式加载引擎
// ============================================================================

class LodStreamingEngine {
 public:
  LodStreamingEngine();
  ~LodStreamingEngine();

  // === 清单管理 ===
  void RegisterAssetManifest(const AssetManifest& manifest);
  void UnregisterAsset(const std::string& asset_id);
  const AssetManifest* GetManifest(const std::string& asset_id) const;

  // === LOD 管理 ===
  // 设置资产的LOD等级 (驱动分块加载)
  void SetLodLevel(const std::string& asset_id, LodLevel level);

  // 设置相机状态 (驱动全局LOD计算)
  void SetCameraPosition(float x, float y, float z);
  void SetCameraDirection(float dx, float dy, float dz);

  // === 下载控制 ===
  // 触发所需分块的下载 (每帧调用)
  void Update(float delta_seconds);

  // 通知分块下载完成
  void OnChunkDownloaded(const std::string& asset_id, uint32_t chunk_index);

  // 通知分块下载失败
  void OnChunkFailed(const std::string& asset_id, uint32_t chunk_index);

  // === 查询 ===
  bool IsAssetReady(const std::string& asset_id) const;
  float GetAssetProgress(const std::string& asset_id) const; // 0-1
  LodLevel GetCurrentLodLevel(const std::string& asset_id) const;

  // === 回调和事件 ===
  using ChunkReadyCallback = std::function<void(const std::string& asset_id,
                                                 uint32_t chunk_index,
                                                 LodLevel level)>;
  using AssetReadyCallback = std::function<void(const std::string& asset_id)>;
  using ErrorCallback = std::function<void(const std::string& asset_id,
                                           const std::string& error)>;

  void SetChunkReadyCallback(ChunkReadyCallback cb);
  void SetAssetReadyCallback(AssetReadyCallback cb);
  void SetErrorCallback(ErrorCallback cb);

  // === 统计 ===
  struct StreamingStats {
    uint32_t active_downloads    = 0;
    uint32_t queued_chunks       = 0;
    uint64_t total_bytes_queued  = 0;
    double   estimated_bps       = 0.0;
    uint64_t bytes_downloaded_session = 0;
    uint32_t chunks_loaded_session    = 0;
    double   avg_load_time_ms    = 0.0;
  };
  StreamingStats GetStats() const;

  // === 缓存 ===
  void SetCachePath(const std::string& path);
  uint64_t GetCacheSize() const;
  void ClearCache();

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;

  void UpdateChunkPriorities();
  void DispatchDownloads();
  void ProcessDependencies(const AssetChunk& chunk);
};

} // namespace solra::core::streaming
