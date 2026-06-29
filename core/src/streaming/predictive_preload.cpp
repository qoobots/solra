#include "predictive_preload.hpp"
#include <algorithm>
#include <cmath>
#include <stdexcept>

namespace solra::streaming {

PredictiveLoader::PredictiveLoader(const PreloadPolicy& policy)
    : policy_(policy) {}

void PredictiveLoader::updateRecommendations(const std::vector<AssetDescriptor>& candidates) {
    candidates_ = candidates;
    // Re-score and re-enqueue based on updated recommendations
    for (auto& c : candidates_) {
        if (c.probabilityOfUse >= policy_.minProbability) {
            enqueue(c, -1);
        }
    }
}

void PredictiveLoader::enqueue(const AssetDescriptor& asset, int priorityOverride) {
    // Skip already preloaded
    if (preloadedAssets_.count(asset.assetId)) return;
    // Skip already downloading
    for (auto& d : activeDownloads_)
        if (d.assetId == asset.assetId) return;

    PreloadItem item;
    item.asset = asset;
    item.score = computeScore(asset, priorityOverride >= 0 ? priorityOverride : asset.priority);
    item.enqueuedAt = std::chrono::steady_clock::now();
    queue_.push(std::move(item));
}

void PredictiveLoader::cancel(const std::string& assetId) {
    // Remove from active downloads (stub: actual impl cancels HTTP request)
    activeDownloads_.erase(
        std::remove_if(activeDownloads_.begin(), activeDownloads_.end(),
                       [&](const DownloadState& d) { return d.assetId == assetId; }),
        activeDownloads_.end());
}

void PredictiveLoader::update(float deltaTime) {
    if (shouldPause()) return;
    startNextDownloads();

    // Simulate download progress for active items
    for (auto& d : activeDownloads_) {
        if (!d.complete) {
            d.progress = std::min(d.progress + deltaTime * 0.5f, 1.0f); // stub: 2s download
            if (d.progress >= 1.0f) {
                d.complete = true;
                preloadedAssets_[d.assetId] = "/cache/" + d.assetId;
                if (onAssetReady_) onAssetReady_(d.assetId, preloadedAssets_[d.assetId]);
            }
        }
    }

    // Clean completed downloads
    activeDownloads_.erase(
        std::remove_if(activeDownloads_.begin(), activeDownloads_.end(),
                       [](const DownloadState& d) { return d.complete; }),
        activeDownloads_.end());

    // Evict expired entries
    // (stub: full impl uses last-used timestamp + TTL)
}

bool PredictiveLoader::isPreloaded(const std::string& assetId) const {
    return preloadedAssets_.count(assetId) > 0;
}

float PredictiveLoader::preloadProgress(const std::string& assetId) const {
    for (auto& d : activeDownloads_)
        if (d.assetId == assetId) return d.progress;
    return preloadedAssets_.count(assetId) ? 1.0f : 0.0f;
}

size_t PredictiveLoader::pendingCount() const {
    return queue_.size();
}

uint64_t PredictiveLoader::usedBudgetBytes() const {
    uint64_t total = 0;
    for (auto& [id, path] : preloadedAssets_) total += 0; // stub
    return total;
}

void PredictiveLoader::setPolicy(const PreloadPolicy& policy) {
    policy_ = policy;
}

bool PredictiveLoader::shouldPause() const {
    if (policy_.wifiOnly) {
        // Stub: check network type
    }
    if (policy_.batteryAware) {
        // Stub: check battery level
    }
    return false;
}

float PredictiveLoader::computeScore(const AssetDescriptor& asset, int priority) const {
    float pScore = asset.probabilityOfUse;
    float priScore = static_cast<float>(priority) / 100.0f;
    float sizePenalty = asset.sizeBytes > 1024*1024
        ? 1.0f / std::log2(static_cast<float>(asset.sizeBytes) / (1024*1024))
        : 1.0f;
    return pScore * 0.5f + priScore * 0.3f + sizePenalty * 0.2f;
}

void PredictiveLoader::startNextDownloads() {
    while (activeDownloads_.size() < policy_.maxConcurrentDownloads && !queue_.empty()) {
        auto item = queue_.top(); queue_.pop();
        DownloadState ds;
        ds.assetId = item.asset.assetId;
        ds.progress = 0.0f;
        activeDownloads_.push_back(ds);
    }
}

void PredictiveLoader::evictLru(uint64_t neededBytes) {
    // Stub: LRU eviction from preloadedAssets_
    (void)neededBytes;
}

} // namespace solra::streaming
