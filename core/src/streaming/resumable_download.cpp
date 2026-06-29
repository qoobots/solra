#include "resumable_download.hpp"
#include <fstream>
#include <algorithm>
#include <chrono>
#include <thread>

namespace solra::streaming {

ResumableDownloader::ResumableDownloader(const std::string& checkpointDir)
    : checkpointDir_(checkpointDir) {
    std::filesystem::create_directories(checkpointDir_);
    loadCheckpoints();
}

bool ResumableDownloader::download(const std::string& uri,
                                    const std::string& localPath,
                                    ProgressCallback onProgress,
                                    CompleteCallback onComplete) {
    // Check if resumable
    uint64_t startOffset = 0;
    if (canResume(uri)) {
        auto& cp = checkpoints_[uri];
        startOffset = cp.downloadedBytes;
        // Verify local file size matches checkpoint
        std::error_code ec;
        auto fileSize = std::filesystem::file_size(localPath, ec);
        if (!ec && fileSize == cp.downloadedBytes) {
            // Resume from checkpoint
        } else {
            startOffset = 0; // File mismatch, restart
        }
    }

    ActiveDownload ad;
    ad.uri = uri;
    ad.localPath = localPath;
    ad.offset = startOffset;
    ad.total = 0;
    ad.onProgress = std::move(onProgress);
    ad.onComplete = std::move(onComplete);
    active_[uri] = std::move(ad);

    // Stub: Production code sends HTTP Range request:
    // GET <uri> HTTP/1.1
    // Range: bytes=<startOffset>-
    // If-None-Match: <etag>
    //
    // On each chunk received:
    // 1. Append to file
    // 2. Update checkpoint
    // 3. Call onProgress
    //
    // On completion: call onComplete(true, localPath)
    // On error: retry with exponential backoff

    return true;
}

bool ResumableDownloader::canResume(const std::string& uri) const {
    return checkpoints_.count(uri) > 0;
}

float ResumableDownloader::progress(const std::string& uri) const {
    auto it = checkpoints_.find(uri);
    if (it == checkpoints_.end()) return 0.0f;
    if (it->second.totalSize == 0) return 0.0f;
    return static_cast<float>(it->second.downloadedBytes) / it->second.totalSize;
}

void ResumableDownloader::cancel(const std::string& uri) {
    auto it = active_.find(uri);
    if (it != active_.end()) {
        it->second.cancelled = true;
        active_.erase(it);
    }
}

void ResumableDownloader::cancelAll() {
    for (auto& [uri, ad] : active_) ad.cancelled = true;
    active_.clear();
}

void ResumableDownloader::saveCheckpoints() {
    // Stub: serialize checkpoints_ to checkpointDir_/checkpoints.json
    // Format: { uri, localPath, totalSize, downloadedBytes, etag, lastModified, timestamp }
}

void ResumableDownloader::loadCheckpoints() {
    // Stub: deserialize from checkpointDir_/checkpoints.json
}

void ResumableDownloader::clearCheckpoint(const std::string& uri) {
    checkpoints_.erase(uri);
    // Remove checkpoint file on disk
}

size_t ResumableDownloader::activeDownloads() const {
    return active_.size();
}

size_t ResumableDownloader::checkpointCount() const {
    return checkpoints_.size();
}

std::string ResumableDownloader::checkpointFilePath(const std::string& uri) const {
    return checkpointDir_ + "/" + makeEtagSafe(uri) + ".ckpt";
}

std::string ResumableDownloader::makeEtagSafe(const std::string& etag) const {
    std::string safe;
    for (char c : etag)
        safe += (std::isalnum(static_cast<unsigned char>(c)) || c == '-' || c == '_') ? c : '_';
    return safe;
}

} // namespace solra::streaming
