#pragma once
// Resumable Download: HTTP Range-based resume + checkpoint persistence
#include <cstdint>
#include <string>
#include <vector>
#include <unordered_map>
#include <functional>
#include <filesystem>

namespace solra::streaming {

// ---- Download checkpoint ----
struct DownloadCheckpoint {
    std::string uri;
    std::string localPath;
    uint64_t totalSize = 0;        // Content-Length from server
    uint64_t downloadedBytes = 0;  // bytes written to disk
    std::string etag;              // for conditional requests
    std::string lastModified;
    uint64_t lastCheckpointTime = 0; // unix timestamp
};

// ---- ResumableDownloader ----
class ResumableDownloader {
public:
    explicit ResumableDownloader(const std::string& checkpointDir);

    // Download with automatic resume
    using ProgressCallback = std::function<void(uint64_t downloaded, uint64_t total, float speedBps)>;
    using CompleteCallback = std::function<void(bool success, const std::string& localPath)>;

    bool download(const std::string& uri,
                  const std::string& localPath,
                  ProgressCallback onProgress = nullptr,
                  CompleteCallback onComplete = nullptr);

    // Check if a download can be resumed
    bool canResume(const std::string& uri) const;

    // Get download progress (0→1)
    float progress(const std::string& uri) const;

    // Cancel and clean up
    void cancel(const std::string& uri);
    void cancelAll();

    // Checkpoint persistence
    void saveCheckpoints();
    void loadCheckpoints();
    void clearCheckpoint(const std::string& uri);

    // Stats
    size_t activeDownloads() const;
    size_t checkpointCount() const;

    // Configuration
    void setChunkSize(uint64_t bytes) { chunkSize_ = bytes; }
    void setMaxRetries(uint32_t n) { maxRetries_ = n; }
    void setRetryDelayMs(uint32_t ms) { retryDelayMs_ = ms; }

private:
    std::string checkpointDir_;
    std::unordered_map<std::string, DownloadCheckpoint> checkpoints_;
    uint64_t chunkSize_ = 256 * 1024; // 256 KB
    uint32_t maxRetries_ = 5;
    uint32_t retryDelayMs_ = 2'000;   // exponential backoff

    std::string checkpointFilePath(const std::string& uri) const;
    std::string makeEtagSafe(const std::string& etag) const;

    struct ActiveDownload {
        std::string uri;
        std::string localPath;
        uint64_t offset = 0;
        uint64_t total = 0;
        ProgressCallback onProgress;
        CompleteCallback onComplete;
        uint32_t retryCount = 0;
        bool cancelled = false;
    };
    std::unordered_map<std::string, ActiveDownload> active_;
};

} // namespace solra::streaming
