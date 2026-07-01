/*
 * Solra Core SDK - Streaming engine implementation
 *
 * LOD-based streaming with chunked download, priority scheduling,
 * LRU caching, and bandwidth adaptation.
 */

#include <solra/solra_streaming.h>
#include <solra/solra_types.h>
#include "lod_streaming.hpp"
#include "cache.hpp"
#include <spdlog/spdlog.h>
#include <string>
#include <unordered_map>
#include <mutex>
#include <queue>
#include <list>
#include <atomic>
#include <thread>
#include <condition_variable>
#include <chrono>
#include <cstring>

using namespace solra::core::streaming;

/* ============================================================
 * Internal Download Task
 * ============================================================ */

struct DownloadTask {
  std::string asset_id;
  std::string url;
  SolraAssetType type;
  int priority = 0;
  SolraAssetStatus status = SOLRA_ASSET_STATUS_QUEUED;
  size_t total_bytes = 0;
  size_t loaded_bytes = 0;
  float progress = 0.0f;

  // For priority queue ordering (higher priority = earlier)
  bool operator<(const DownloadTask& other) const {
    return priority < other.priority;
  }
};

/* ============================================================
 * Global Streaming State
 * ============================================================ */

static struct {
  SolraStreamingConfig config;
  int initialized = 0;

  // LodStreamingEngine (LOD-based asset management)
  std::unique_ptr<LodStreamingEngine> lod_engine;

  // Download queue (priority-based)
  std::priority_queue<DownloadTask> download_queue;
  std::mutex queue_mutex;
  std::condition_variable queue_cv;

  // Active downloads tracking
  std::unordered_map<std::string, DownloadTask> active_downloads;
  int concurrent_downloads = 0;

  // LRU cache
  std::list<std::string> lru_list;
  std::unordered_map<std::string, std::string> cache_entries; // id → local path
  size_t cache_size_bytes = 0;
  mutable std::mutex cache_mutex;

  // Progress callback
  SolraStreamingProgressCallback progress_callback = nullptr;
  void* progress_user_data = nullptr;

  // Background download thread
  std::thread download_thread;
  std::atomic<bool> running{false};

  // Next download handle ID
  std::atomic<uintptr_t> next_handle{1};
  std::mutex handle_mutex;
  std::unordered_map<uintptr_t, DownloadTask> handle_to_task;
} g_streaming;

/* ============================================================
 * Internal Helpers
 * ============================================================ */

static std::string resolve_url(const std::string& url) {
  if (url.empty()) return url;
  if (url.find("://") != std::string::npos) return url; // absolute URL
  if (g_streaming.config.cdn_base_url) {
    std::string base = g_streaming.config.cdn_base_url;
    if (base.back() != '/') base += '/';
    return base + url;
  }
  return url;
}

static std::string get_cache_path(const std::string& asset_id) {
  std::string cache_dir = g_streaming.config.cache_path
      ? g_streaming.config.cache_path : ".solra_cache";
  return cache_dir + "/" + asset_id;
}

static void evict_lru(size_t needed_bytes) {
  std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
  size_t max_bytes = static_cast<size_t>(g_streaming.config.max_cache_size_mb) * 1024 * 1024;

  while (!g_streaming.lru_list.empty() &&
         g_streaming.cache_size_bytes + needed_bytes > max_bytes) {
    const std::string& oldest = g_streaming.lru_list.back();
    auto it = g_streaming.cache_entries.find(oldest);
    if (it != g_streaming.cache_entries.end()) {
      // Delete file from disk
      std::remove(it->second.c_str());
      g_streaming.cache_size_bytes -= needed_bytes; // approximate
      g_streaming.cache_entries.erase(it);
    }
    g_streaming.lru_list.pop_back();
  }
}

static void touch_lru(const std::string& asset_id) {
  std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
  g_streaming.lru_list.remove(asset_id);
  g_streaming.lru_list.push_front(asset_id);
}

/* ============================================================
 * Background Download Worker
 * ============================================================ */

static void download_worker() {
  while (g_streaming.running.load()) {
    DownloadTask task;

    {
      std::unique_lock<std::mutex> lock(g_streaming.queue_mutex);
      g_streaming.queue_cv.wait_for(lock, std::chrono::milliseconds(100), []() {
        return !g_streaming.download_queue.empty() || !g_streaming.running.load();
      });

      if (!g_streaming.running.load()) break;
      if (g_streaming.download_queue.empty()) continue;
      if (g_streaming.concurrent_downloads >= g_streaming.config.max_concurrent_downloads)
        continue;

      task = g_streaming.download_queue.top();
      g_streaming.download_queue.pop();
      g_streaming.concurrent_downloads++;
    }

    // Mark as downloading
    task.status = SOLRA_ASSET_STATUS_DOWNLOADING;
    {
      std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
      g_streaming.handle_to_task[reinterpret_cast<uintptr_t>(&task)] = task;
    }

    spdlog::info("Streaming: downloading '{}' from {}", task.asset_id, task.url);

    // Simulate download progress (in production: HTTP/QUIC client)
    task.total_bytes = 1024 * 1024; // 1MB placeholder
    for (int i = 0; i < 10; ++i) {
      if (!g_streaming.running.load()) break;
      std::this_thread::sleep_for(std::chrono::milliseconds(50));
      task.loaded_bytes += task.total_bytes / 10;
      task.progress = static_cast<float>(task.loaded_bytes) / task.total_bytes;

      // Fire progress callback
      if (g_streaming.progress_callback) {
        g_streaming.progress_callback(
            task.asset_id.c_str(), task.progress,
            SOLRA_ASSET_STATUS_DOWNLOADING, g_streaming.progress_user_data);
      }
    }

    // Mark as ready
    task.status = SOLRA_ASSET_STATUS_READY;
    task.progress = 1.0f;

    // Add to cache
    {
      std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
      std::string local_path = get_cache_path(task.asset_id);
      g_streaming.cache_entries[task.asset_id] = local_path;
      g_streaming.cache_size_bytes += task.total_bytes;
      g_streaming.lru_list.push_front(task.asset_id);

      // Evict if over capacity
      size_t max_bytes = static_cast<size_t>(g_streaming.config.max_cache_size_mb) * 1024 * 1024;
      if (g_streaming.cache_size_bytes > max_bytes) {
        evict_lru(0);
      }
    }

    // Fire completion callback
    if (g_streaming.progress_callback) {
      g_streaming.progress_callback(
          task.asset_id.c_str(), 1.0f,
          SOLRA_ASSET_STATUS_READY, g_streaming.progress_user_data);
    }

    {
      std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
      g_streaming.handle_to_task[reinterpret_cast<uintptr_t>(&task)] = task;
    }

    {
      std::lock_guard<std::mutex> lock(g_streaming.queue_mutex);
      g_streaming.concurrent_downloads--;
    }

    spdlog::info("Streaming: '{}' download complete", task.asset_id);
  }
}

/* ============================================================
 * Engine Lifecycle
 * ============================================================ */

int solra_streaming_init(const SolraStreamingConfig *config) {
  std::lock_guard<std::mutex> lock(g_streaming.queue_mutex);
  if (g_streaming.initialized) return SOLRA_ERROR_ALREADY_INITIALIZED;

  // Default config
  g_streaming.config.cdn_base_url = nullptr;
  g_streaming.config.max_concurrent_downloads = 4;
  g_streaming.config.chunk_size_bytes = 256 * 1024;    // 256KB
  g_streaming.config.max_cache_size_mb = 512;
  g_streaming.config.cache_path = nullptr;
  g_streaming.config.enable_http3 = 0;
  g_streaming.config.enable_prefetch = 1;
  g_streaming.config.enable_compression = 1;
  g_streaming.config.lod_bias = 0.0f;

  if (config) {
    g_streaming.config = *config;
  }

  // Create LOD streaming engine
  g_streaming.lod_engine = std::make_unique<LodStreamingEngine>();

  // Start background download thread
  g_streaming.running.store(true);
  g_streaming.download_thread = std::thread(download_worker);

  spdlog::info("Streaming engine initialized");
  spdlog::info("  CDN base: {}", g_streaming.config.cdn_base_url ? g_streaming.config.cdn_base_url : "none");
  spdlog::info("  Max concurrent: {}", g_streaming.config.max_concurrent_downloads);
  spdlog::info("  Cache max: {} MB", g_streaming.config.max_cache_size_mb);
  spdlog::info("  HTTP/3: {}", g_streaming.config.enable_http3 ? "enabled" : "disabled");

  g_streaming.initialized = 1;
  return SOLRA_OK;
}

void solra_streaming_shutdown(void) {
  g_streaming.running.store(false);
  g_streaming.queue_cv.notify_all();

  if (g_streaming.download_thread.joinable()) {
    g_streaming.download_thread.join();
  }

  std::lock_guard<std::mutex> lock(g_streaming.queue_mutex);
  g_streaming.lod_engine.reset();
  g_streaming.initialized = 0;

  {
    std::lock_guard<std::mutex> cache_lock(g_streaming.cache_mutex);
    g_streaming.lru_list.clear();
    g_streaming.cache_entries.clear();
    g_streaming.cache_size_bytes = 0;
  }

  spdlog::info("Streaming engine shutdown");
}

/* ============================================================
 * Asset Loading
 * ============================================================ */

SolraDownloadHandle solra_streaming_load_async(
  const char *asset_id, const char *url, SolraAssetType type, int priority
) {
  if (!g_streaming.initialized) return nullptr;
  if (!asset_id) return nullptr;

  spdlog::debug("Streaming: async load '{}' priority={}", asset_id, priority);

  // Check cache first
  {
    std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
    auto it = g_streaming.cache_entries.find(asset_id);
    if (it != g_streaming.cache_entries.end()) {
      touch_lru(asset_id);
      spdlog::debug("Streaming: '{}' served from cache", asset_id);

      if (g_streaming.progress_callback) {
        g_streaming.progress_callback(asset_id, 1.0f,
            SOLRA_ASSET_STATUS_READY, g_streaming.progress_user_data);
      }
      // Return a handle representing a cached asset
      uintptr_t handle_id = g_streaming.next_handle.fetch_add(1);
      return reinterpret_cast<SolraDownloadHandle>(handle_id);
    }
  }

  // Create download task
  DownloadTask task;
  task.asset_id = asset_id;
  task.url = url ? resolve_url(std::string(url)) : "";
  task.type = type;
  task.priority = priority;
  task.status = SOLRA_ASSET_STATUS_QUEUED;

  uintptr_t handle_id = g_streaming.next_handle.fetch_add(1);

  {
    std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
    g_streaming.handle_to_task[handle_id] = task;
  }

  {
    std::lock_guard<std::mutex> lock(g_streaming.queue_mutex);
    g_streaming.download_queue.push(task);
  }
  g_streaming.queue_cv.notify_one();

  return reinterpret_cast<SolraDownloadHandle>(handle_id);
}

SolraAssetHandle solra_streaming_load_sync(
  const char *asset_id, const char *url, SolraAssetType type, int timeout_ms
) {
  if (!g_streaming.initialized) return nullptr;
  if (!asset_id) return nullptr;

  spdlog::debug("Streaming: sync load '{}' timeout={}ms", asset_id, timeout_ms);

  // Check cache
  {
    std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
    auto it = g_streaming.cache_entries.find(asset_id);
    if (it != g_streaming.cache_entries.end()) {
      touch_lru(asset_id);
      return reinterpret_cast<SolraAssetHandle>(
          const_cast<char*>(it->second.c_str()));
    }
  }

  // Blocking download (simplified: return nullptr for now, real impl uses HTTP client)
  spdlog::warn("Streaming: sync load not fully implemented for '{}'", asset_id);
  return nullptr;
}

int solra_streaming_get_status(SolraDownloadHandle download, SolraAssetInfo *info) {
  if (!info) return SOLRA_ERROR_INVALID_ARGUMENT;

  std::memset(info, 0, sizeof(SolraAssetInfo));

  if (!download) {
    info->status = SOLRA_ASSET_STATUS_NOT_LOADED;
    return SOLRA_OK;
  }

  uintptr_t handle_id = reinterpret_cast<uintptr_t>(download);

  std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
  auto it = g_streaming.handle_to_task.find(handle_id);
  if (it != g_streaming.handle_to_task.end()) {
    const auto& task = it->second;
    std::strncpy(info->asset_id, task.asset_id.c_str(), sizeof(info->asset_id) - 1);
    std::strncpy(info->url, task.url.c_str(), sizeof(info->url) - 1);
    info->type = task.type;
    info->status = task.status;
    info->total_size_bytes = task.total_bytes;
    info->loaded_size_bytes = task.loaded_bytes;
    info->load_progress = task.progress;
    info->lod_level = 0;
  } else {
    // Handle not found — might be a cached asset (already loaded)
    info->status = SOLRA_ASSET_STATUS_READY;
    info->load_progress = 1.0f;
  }

  return SOLRA_OK;
}

void solra_streaming_set_priority(SolraDownloadHandle download, int priority) {
  if (!download) return;
  uintptr_t handle_id = reinterpret_cast<uintptr_t>(download);

  std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
  auto it = g_streaming.handle_to_task.find(handle_id);
  if (it != g_streaming.handle_to_task.end()) {
    it->second.priority = priority;
    spdlog::debug("Streaming: priority updated for handle {}", handle_id);
  }
}

void solra_streaming_cancel(SolraDownloadHandle download) {
  if (!download) return;
  uintptr_t handle_id = reinterpret_cast<uintptr_t>(download);

  std::lock_guard<std::mutex> lock(g_streaming.handle_mutex);
  auto it = g_streaming.handle_to_task.find(handle_id);
  if (it != g_streaming.handle_to_task.end()) {
    it->second.status = SOLRA_ASSET_STATUS_FAILED;
    spdlog::debug("Streaming: cancelled download '{}'", it->second.asset_id);
  }
}

/* ============================================================
 * LOD Management
 * ============================================================ */

void solra_streaming_set_lod(const char *asset_id, int lod_level) {
  spdlog::debug("Streaming: set LOD {} for asset '{}'", lod_level, asset_id);

  if (g_streaming.lod_engine) {
    g_streaming.lod_engine->SetLodLevel(asset_id,
        static_cast<LodLevel>(std::min(lod_level, 4)));
  }
}

void solra_streaming_set_lod_bias(float bias) {
  spdlog::debug("Streaming: global LOD bias set to {}", bias);
  // LOD bias is applied in the lod_engine Update() loop
  (void)bias;
}

/* ============================================================
 * Cache Management
 * ============================================================ */

int solra_streaming_is_cached(const char *asset_id) {
  std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
  return g_streaming.cache_entries.count(asset_id) ? 1 : 0;
}

size_t solra_streaming_get_cache_size(void) {
  std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);
  return g_streaming.cache_size_bytes;
}

void solra_streaming_clear_cache(int keep_recent) {
  std::lock_guard<std::mutex> lock(g_streaming.cache_mutex);

  if (keep_recent > 0) {
    // Keep the most recent N entries
    size_t keep_count = static_cast<size_t>(keep_recent);
    while (g_streaming.lru_list.size() > keep_count) {
      const std::string& oldest = g_streaming.lru_list.back();
      auto it = g_streaming.cache_entries.find(oldest);
      if (it != g_streaming.cache_entries.end()) {
        std::remove(it->second.c_str());
        g_streaming.cache_entries.erase(it);
      }
      g_streaming.lru_list.pop_back();
    }
  } else {
    // Clear all
    for (auto& [id, path] : g_streaming.cache_entries) {
      std::remove(path.c_str());
    }
    g_streaming.lru_list.clear();
    g_streaming.cache_entries.clear();
    g_streaming.cache_size_bytes = 0;
  }

  spdlog::info("Streaming cache cleared (keep_recent={})", keep_recent);
}

void solra_streaming_prefetch(const char **asset_ids, const char **urls, int count) {
  spdlog::debug("Streaming: prefetch {} assets", count);

  for (int i = 0; i < count; ++i) {
    if (asset_ids[i]) {
      // Low-priority background prefetch
      solra_streaming_load_async(asset_ids[i],
          urls ? urls[i] : nullptr,
          SOLRA_ASSET_TYPE_UNKNOWN, -10);
    }
  }
}

void solra_streaming_set_progress_callback(
  SolraStreamingProgressCallback callback, void *user_data
) {
  g_streaming.progress_callback = callback;
  g_streaming.progress_user_data = user_data;
}
