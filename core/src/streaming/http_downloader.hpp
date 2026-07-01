#pragma once
// HTTP Download Client: cross-platform HTTP/HTTPS download with Range support
// Uses libcurl on all platforms for reliable HTTP/2 + HTTP/3 support.
// Provides chunked download callback interface for streaming integration.

#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <mutex>
#include <atomic>
#include <thread>
#include <unordered_map>
#include <chrono>

namespace solra::streaming {

// ============================================================================
// Download Configuration
// ============================================================================
struct DownloadConfig {
    std::string url;
    std::string localPath;
    uint64_t rangeStart = 0;      // HTTP Range: bytes=start-
    uint64_t rangeEnd = 0;        // 0 = until EOF
    uint32_t connectTimeoutMs = 10000;
    uint32_t readTimeoutMs = 30000;
    uint32_t lowSpeedLimitBps = 1024;  // 1 KB/s minimum
    uint32_t lowSpeedTimeSeconds = 30;
    bool followRedirects = true;
    uint32_t maxRedirects = 5;
    bool verifySsl = true;
    std::string caBundlePath;     // custom CA bundle
    std::string proxyUrl;         // HTTP proxy (e.g. http://proxy:8080)
    std::string userAgent = "SolraCore/1.0";
    bool enableHttp2 = true;
    bool enableHttp3 = false;     // requires libcurl built with HTTP/3
    // Resume support
    std::string etag;             // If-None-Match / If-Match
    std::string lastModified;     // If-Modified-Since
};

// ============================================================================
// Download Progress
// ============================================================================
struct DownloadProgress {
    uint64_t downloadedBytes = 0;
    uint64_t totalBytes = 0;       // 0 = unknown (chunked encoding)
    uint64_t speedBps = 0;         // current transfer speed
    uint32_t elapsedMs = 0;
    int httpStatusCode = 0;
    bool isResumed = false;
    float progress() const {
        if (totalBytes == 0) return 0.0f;
        return static_cast<float>(downloadedBytes) / static_cast<float>(totalBytes);
    }
};

// ============================================================================
// Download Result
// ============================================================================
struct DownloadResult {
    bool success = false;
    int httpStatusCode = 0;
    std::string errorMessage;
    uint64_t totalBytes = 0;
    uint64_t downloadedBytes = 0;
    uint32_t durationMs = 0;
    std::string etag;
    std::string lastModified;
    std::string contentType;
    std::string effectiveUrl;     // final URL after redirects
};

// ============================================================================
// Callbacks
// ============================================================================
using DataCallback = std::function<void(const uint8_t* data, size_t size)>;
using ProgressCallback = std::function<void(const DownloadProgress& progress)>;
using CompleteCallback = std::function<void(const DownloadResult& result)>;

// ============================================================================
// HttpDownloader — single-file HTTP download with Range + resume
// ============================================================================
class HttpDownloader {
public:
    HttpDownloader();
    ~HttpDownloader();

    // Synchronous download (blocking)
    DownloadResult download(const DownloadConfig& config);

    // Asynchronous download (non-blocking, callback on background thread)
    bool downloadAsync(const DownloadConfig& config,
                       DataCallback onData = nullptr,
                       ProgressCallback onProgress = nullptr,
                       CompleteCallback onComplete = nullptr);

    // Cancel an ongoing download
    void cancel();

    // Check if download is in progress
    bool isActive() const;

    // Get current progress
    DownloadProgress currentProgress() const;

    // Global init/shutdown (call once per process)
    static bool globalInit();
    static void globalShutdown();

    // Check if a URL supports Range requests (HEAD request)
    static bool supportsRange(const std::string& url, uint32_t timeoutMs = 5000);

    // Get file size via HEAD request
    static uint64_t getContentLength(const std::string& url, uint32_t timeoutMs = 5000);

private:
    void* curlHandle_;     // CURL*
    std::atomic<bool> active_{false};
    std::atomic<bool> cancelled_{false};
    std::thread workerThread_;
    mutable std::mutex mutex_;
    DownloadProgress progress_;

    // Callbacks
    DataCallback dataCallback_;
    ProgressCallback progressCallback_;
    CompleteCallback completeCallback_;

    void downloadWorker(const DownloadConfig& config);
    static size_t writeCallback(void* ptr, size_t size, size_t nmemb, void* userdata);
    static int progressCallbackFunc(void* clientp, curl_off_t dltotal,
                                     curl_off_t dlnow, curl_off_t ultotal,
                                     curl_off_t ulnow);
};

// ============================================================================
// HttpConnectionPool — connection reuse for multiple downloads to same host
// ============================================================================
class HttpConnectionPool {
public:
    static HttpConnectionPool& instance();

    // Get a pre-configured CURL handle for the given host
    void* acquire(const std::string& host);
    void release(const std::string& host, void* handle);

    void setMaxConnectionsPerHost(uint32_t n) { maxPerHost_ = n; }
    void clear();

private:
    HttpConnectionPool() = default;
    ~HttpConnectionPool();
    std::mutex mutex_;
    uint32_t maxPerHost_ = 4;
    std::unordered_map<std::string, std::vector<void*>> pool_;
};

} // namespace solra::streaming
