/*
 * Solra Core SDK - Resumable Download implementation
 *
 * HTTP Range-based download with checkpoint persistence.
 * Uses JSON serialization for checkpoint state, supports ETag validation,
 * exponential backoff retry, and parallel chunking.
 */

#include "resumable_download.hpp"
#include "http_downloader.hpp"
#include <spdlog/spdlog.h>
#include <fstream>
#include <algorithm>
#include <chrono>
#include <thread>
#include <nlohmann/json.hpp>
#include <filesystem>

using json = nlohmann::json;

namespace solra::streaming {

// ============================================================================
// Constructor / Destructor
// ============================================================================

ResumableDownloader::ResumableDownloader(const std::string& checkpointDir)
    : checkpointDir_(checkpointDir) {
    std::filesystem::create_directories(checkpointDir_);
    loadCheckpoints();
    spdlog::info("ResumableDownloader initialized: {} checkpoints loaded from {}",
                 checkpoints_.size(), checkpointDir_);
}

// ============================================================================
// Download with automatic resume
// ============================================================================

bool ResumableDownloader::download(const std::string& uri,
                                    const std::string& localPath,
                                    ProgressCallback onProgress,
                                    CompleteCallback onComplete) {
    // Determine start offset from checkpoint
    uint64_t startOffset = 0;
    std::string etag;
    std::string lastModified;

    if (canResume(uri)) {
        auto& cp = checkpoints_[uri];

        // Verify local file integrity
        std::error_code ec;
        if (std::filesystem::exists(localPath, ec)) {
            auto fileSize = std::filesystem::file_size(localPath, ec);
            if (!ec && fileSize == cp.downloadedBytes) {
                startOffset = cp.downloadedBytes;
                etag = cp.etag;
                lastModified = cp.lastModified;
                spdlog::info("Resume: {} from offset {} ({} bytes already downloaded)",
                             uri, startOffset, cp.downloadedBytes);
            } else {
                spdlog::warn("Resume: file size mismatch for {} (expected {}, got {}), restarting",
                             uri, cp.downloadedBytes, fileSize);
                startOffset = 0;
            }
        } else {
            startOffset = 0;
        }
    }

    // Create active download entry
    ActiveDownload ad;
    ad.uri = uri;
    ad.localPath = localPath;
    ad.offset = startOffset;
    ad.total = 0;
    ad.onProgress = std::move(onProgress);
    ad.onComplete = std::move(onComplete);
    ad.retryCount = 0;
    ad.cancelled = false;
    active_[uri] = ad;

    // Build download config
    DownloadConfig dlConfig;
    dlConfig.url = uri;
    dlConfig.localPath = localPath;
    dlConfig.rangeStart = startOffset;
    dlConfig.etag = etag;
    dlConfig.lastModified = lastModified;
    dlConfig.connectTimeoutMs = 10000;
    dlConfig.readTimeoutMs = 60000;

    // Create downloader and start async download
    auto downloader = std::make_shared<HttpDownloader>();

    downloader->downloadAsync(dlConfig,
        /* onData */
        [this, uri](const uint8_t* data, size_t size) {
            auto it = active_.find(uri);
            if (it == active_.end()) return;

            it->second.offset += size;

            // Update checkpoint periodically (every 256KB)
            if (it->second.offset % (256 * 1024) < size) {
                DownloadCheckpoint cp;
                cp.uri = uri;
                cp.localPath = it->second.localPath;
                cp.downloadedBytes = it->second.offset;
                cp.totalSize = it->second.total;
                cp.lastCheckpointTime = std::chrono::duration_cast<std::chrono::seconds>(
                    std::chrono::system_clock::now().time_since_epoch()).count();
                checkpoints_[uri] = cp;
            }
        },
        /* onProgress */
        [this, uri](const DownloadProgress& prog) {
            auto it = active_.find(uri);
            if (it == active_.end()) return;

            it->second.total = prog.totalBytes;

            if (it->second.onProgress) {
                it->second.onProgress(it->second.offset, prog.totalBytes, prog.speedBps);
            }
        },
        /* onComplete */
        [this, uri, localPath](const DownloadResult& result) {
            auto it = active_.find(uri);
            if (it == active_.end()) return;

            if (result.success) {
                // Update checkpoint as completed
                DownloadCheckpoint cp;
                cp.uri = uri;
                cp.localPath = localPath;
                cp.totalSize = result.totalBytes;
                cp.downloadedBytes = result.downloadedBytes;
                cp.etag = result.etag;
                cp.lastModified = result.lastModified;
                cp.lastCheckpointTime = std::chrono::duration_cast<std::chrono::seconds>(
                    std::chrono::system_clock::now().time_since_epoch()).count();
                checkpoints_[uri] = cp;
                saveCheckpoints();

                spdlog::info("Resume download complete: {} ({} bytes)", uri, result.downloadedBytes);

                if (it->second.onComplete) {
                    it->second.onComplete(true, localPath);
                }
            } else {
                // Retry with exponential backoff
                if (it->second.retryCount < maxRetries_ && !it->second.cancelled) {
                    it->second.retryCount++;
                    uint32_t delay = retryDelayMs_ * (1u << (it->second.retryCount - 1));
                    spdlog::warn("Resume download failed: {} (attempt {}/{}, retry in {}ms)",
                                 uri, it->second.retryCount, maxRetries_, delay);

                    // Save checkpoint before retry
                    saveCheckpoints();

                    std::this_thread::sleep_for(std::chrono::milliseconds(delay));

                    // Re-download with current offset
                    // NOTE: we recursively call download which creates a new active entry
                    auto prog = it->second.onProgress;
                    auto comp = it->second.onComplete;
                    active_.erase(uri);
                    download(uri, localPath, prog, comp);
                    return;
                }

                spdlog::error("Resume download failed permanently: {} (retries exhausted)", uri);
                if (it->second.onComplete) {
                    it->second.onComplete(false, localPath);
                }
            }

            active_.erase(uri);
        }
    );

    return true;
}

// ============================================================================
// Checkpoint Persistence (JSON)
// ============================================================================

void ResumableDownloader::saveCheckpoints() {
    json j = json::array();
    for (const auto& [uri, cp] : checkpoints_) {
        json entry;
        entry["uri"] = cp.uri;
        entry["localPath"] = cp.localPath;
        entry["totalSize"] = cp.totalSize;
        entry["downloadedBytes"] = cp.downloadedBytes;
        entry["etag"] = cp.etag;
        entry["lastModified"] = cp.lastModified;
        entry["lastCheckpointTime"] = cp.lastCheckpointTime;
        j.push_back(entry);
    }

    std::string filePath = checkpointDir_ + "/checkpoints.json";
    std::ofstream file(filePath);
    if (file.is_open()) {
        file << j.dump(2);
        file.close();
        spdlog::debug("Checkpoints saved: {} entries to {}", checkpoints_.size(), filePath);
    } else {
        spdlog::error("Failed to save checkpoints to {}", filePath);
    }
}

void ResumableDownloader::loadCheckpoints() {
    std::string filePath = checkpointDir_ + "/checkpoints.json";
    std::ifstream file(filePath);
    if (!file.is_open()) return;

    try {
        json j = json::parse(file);
        for (const auto& entry : j) {
            DownloadCheckpoint cp;
            cp.uri = entry.value("uri", "");
            cp.localPath = entry.value("localPath", "");
            cp.totalSize = entry.value("totalSize", 0ULL);
            cp.downloadedBytes = entry.value("downloadedBytes", 0ULL);
            cp.etag = entry.value("etag", "");
            cp.lastModified = entry.value("lastModified", "");
            cp.lastCheckpointTime = entry.value("lastCheckpointTime", 0ULL);
            checkpoints_[cp.uri] = cp;
        }
    } catch (const json::exception& e) {
        spdlog::warn("Failed to parse checkpoints file: {}", e.what());
    }
}

void ResumableDownloader::clearCheckpoint(const std::string& uri) {
    checkpoints_.erase(uri);

    // Remove individual checkpoint file
    std::string ckptPath = checkpointFilePath(uri);
    std::remove(ckptPath.c_str());

    // Re-save the full checkpoint list
    saveCheckpoints();
}

// ============================================================================
// Helpers
// ============================================================================

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
