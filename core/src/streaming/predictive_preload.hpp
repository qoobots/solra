#pragma once
// Predictive Preloading: background asset fetching driven by recommendation signals
#include <cstdint>
#include <string>
#include <vector>
#include <queue>
#include <memory>
#include <functional>
#include <chrono>

namespace solra::streaming {

// ---- Asset metadata ----
struct AssetDescriptor {
    std::string uri;              // CDN / streaming URI
    std::string assetId;
    uint64_t sizeBytes = 0;
    int priority = 0;             // higher = more urgent (0 = background, 100 = critical)
    float probabilityOfUse = 0.0f;// 0-1, from recommendation engine
    std::string contentType;      // "model/gltf", "texture/ktx2", "audio/opus"
};

// ---- Preload policy ----
struct PreloadPolicy {
    uint64_t maxBudgetBytes = 256 * 1024 * 1024; // 256 MB
    uint32_t maxConcurrentDownloads = 3;
    float minProbability = 0.3f;                 // skip assets below this threshold
    uint32_t ttlSeconds = 600;                   // evict after 10 min unused
    bool wifiOnly = true;                        // cellular-aware
    bool batteryAware = true;                    // pause below 20% battery
};

// ---- Preload queue item ----
struct PreloadItem {
    AssetDescriptor asset;
    float score = 0.0f;          // composite: probability × size-urgency × recency
    std::chrono::steady_clock::time_point enqueuedAt;
    uint32_t retryCount = 0;
    static constexpr uint32_t kMaxRetries = 3;
};

// ---- PredictiveLoader ----
class PredictiveLoader {
public:
    explicit PredictiveLoader(const PreloadPolicy& policy = {});

    // Feed recommendation signals (called by recommendation pipeline)
    void updateRecommendations(const std::vector<AssetDescriptor>& candidates);

    // Manually enqueue an asset for preloading
    void enqueue(const AssetDescriptor& asset, int priorityOverride = -1);

    // Cancel preloading for a specific asset
    void cancel(const std::string& assetId);

    // Per-frame update (drives the download queue)
    void update(float deltaTime);

    // Query
    bool isPreloaded(const std::string& assetId) const;
    float preloadProgress(const std::string& assetId) const; // 0→1
    size_t pendingCount() const;
    uint64_t usedBudgetBytes() const;

    // Callbacks
    using AssetReadyCallback = std::function<void(const std::string& assetId, const std::string& localPath)>;
    void setAssetReadyCallback(AssetReadyCallback cb) { onAssetReady_ = std::move(cb); }

    // Policy
    void setPolicy(const PreloadPolicy& policy);
    const PreloadPolicy& policy() const { return policy_; }

private:
    PreloadPolicy policy_;
    std::vector<AssetDescriptor> candidates_;
    std::priority_queue<PreloadItem> queue_; // sorted by score descending

    struct DownloadState {
        std::string assetId;
        float progress = 0.0f;
        bool complete = false;
    };
    std::vector<DownloadState> activeDownloads_;
    std::unordered_map<std::string, std::string> preloadedAssets_; // id→path

    AssetReadyCallback onAssetReady_;

    bool shouldPause() const;
    float computeScore(const AssetDescriptor& asset, int priority) const;
    void startNextDownloads();
    void evictLru(uint64_t neededBytes);
};

} // namespace solra::streaming
