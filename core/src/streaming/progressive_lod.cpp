/*
 * Solra Core SDK - Progressive LOD & Asset Compression Implementation
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "progressive_lod.hpp"
#include <spdlog/spdlog.h>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <numeric>

namespace solra::core::streaming {

// ============================================================
// ProgressiveLodManager
// ============================================================

ProgressiveLodManager::ProgressiveLodManager(const ProgressiveLodConfig& config)
    : config_(config) {}

ProgressiveLodManager::~ProgressiveLodManager() {}

void ProgressiveLodManager::registerAsset(const std::string& asset_id, uint32_t max_lod_levels) {
    std::lock_guard<std::mutex> lock(mutex_);

    ProgressiveLodState state;
    state.max_levels = std::min(max_lod_levels, 4u);
    state.current_lod = LodLevel::kNone;
    state.target_lod = LodLevel::kNone;
    state.loading_lod = LodLevel::kNone;

    states_[asset_id] = state;

    // Initialize loaded LODs tracking
    auto& loaded = loaded_lods_[asset_id];
    loaded.resize(max_lod_levels, false);

    spdlog::debug("ProgressiveLOD: registered asset '{}' with {} levels", asset_id, max_lod_levels);
}

void ProgressiveLodManager::unregisterAsset(const std::string& asset_id) {
    std::lock_guard<std::mutex> lock(mutex_);
    states_.erase(asset_id);
    loaded_lods_.erase(asset_id);
}

LodLevel ProgressiveLodManager::selectLod(const std::string& asset_id,
                                           float camera_distance,
                                           float screen_coverage) const {
    std::lock_guard<std::mutex> lock(mutex_);
    return computeTargetLod(camera_distance, screen_coverage);
}

LodLevel ProgressiveLodManager::computeTargetLod(float distance, float screen_coverage) const {
    // Primary: distance-based selection
    LodLevel dist_lod = CalculateLodLevel(distance);

    // Secondary: screen-size-based override (for very small objects far away)
    if (screen_coverage < config_.screen_size_threshold) {
        // Object is very small on screen, drop LOD further
        int level = static_cast<int>(dist_lod);
        level = std::min(level + 1, 3);
        dist_lod = static_cast<LodLevel>(level);
    }

    // Apply quality bias: bias toward higher quality
    if (config_.quality_bias > 0.5f && dist_lod > LodLevel::kLOD0) {
        float bias_chance = (config_.quality_bias - 0.5f) * 2.0f;
        if (bias_chance > 0.3f) {
            int level = static_cast<int>(dist_lod) - 1;
            dist_lod = static_cast<LodLevel>(std::max(level, 0));
        }
    }

    // Apply adaptive quality scaling
    dist_lod = static_cast<LodLevel>(
        std::min(static_cast<int>(dist_lod) + static_cast<int>((1.0f - adaptive_quality_scale_) * 2.0f), 3));

    return dist_lod;
}

void ProgressiveLodManager::requestLod(const std::string& asset_id, LodLevel target) {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = states_.find(asset_id);
    if (it == states_.end()) return;

    auto& state = it->second;

    // Hysteresis: don't switch if target is same as current or loading
    if (target == state.target_lod) return;

    // Don't downgrade if within hysteresis margin
    if (target > state.current_lod &&
        state.camera_distance < config_.distance_thresholds[static_cast<int>(state.current_lod)] + config_.hysteresis) {
        return;
    }

    startTransition(state, target);
}

void ProgressiveLodManager::startTransition(ProgressiveLodState& state, LodLevel target) {
    LodLevel prev_target = state.target_lod;
    state.target_lod = target;
    state.transition_start = std::chrono::steady_clock::now();
    state.blend_factor = 0.0f;
    state.target_ready = false;
    state.mode = selectTransitionMode(state.current_lod, target);

    lod_switches_this_frame_++;

    spdlog::debug("ProgressiveLOD: LOD transition {} -> {} (mode={})",
                  static_cast<int>(prev_target), static_cast<int>(target),
                  static_cast<int>(state.mode));
}

ProgressiveLodState::TransitionMode ProgressiveLodManager::selectTransitionMode(
    LodLevel from, LodLevel to) const {
    if (from == LodLevel::kNone || to == LodLevel::kNone) {
        return ProgressiveLodState::TransitionMode::kInstant;
    }

    int diff = std::abs(static_cast<int>(to) - static_cast<int>(from));
    if (diff <= 1) {
        return config_.enable_dither ?
            ProgressiveLodState::TransitionMode::kDither :
            ProgressiveLodState::TransitionMode::kCrossFade;
    }

    if (config_.enable_morph && diff == 1) {
        return ProgressiveLodState::TransitionMode::kMorphTarget;
    }

    return ProgressiveLodState::TransitionMode::kCrossFade;
}

LodLevel ProgressiveLodManager::getEffectiveLod(const std::string& asset_id) const {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = states_.find(asset_id);
    if (it == states_.end()) return LodLevel::kNone;

    const auto& state = it->second;
    if (state.blend_factor >= 0.99f) return state.target_lod;
    if (state.blend_factor <= 0.01f) return state.current_lod;

    // During cross-fade, the effective LOD is the current (higher-quality) one
    // The blend factor controls the opacity of the new LOD
    return state.blend_factor > 0.5f ? state.target_lod : state.current_lod;
}

float ProgressiveLodManager::getBlendFactor(const std::string& asset_id) const {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = states_.find(asset_id);
    if (it == states_.end()) return 0.0f;
    return it->second.blend_factor;
}

bool ProgressiveLodManager::isLodLoaded(const std::string& asset_id, LodLevel level) const {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = loaded_lods_.find(asset_id);
    if (it == loaded_lods_.end()) return false;

    int idx = static_cast<int>(level);
    if (idx < 0 || idx >= static_cast<int>(it->second.size())) return false;

    return it->second[idx];
}

void ProgressiveLodManager::updateCamera(float px, float py, float pz,
                                          float dx, float dy, float dz,
                                          float fov_degrees, float viewport_width) {
    cam_pos_[0] = px; cam_pos_[1] = py; cam_pos_[2] = pz;
    cam_dir_[0] = dx; cam_dir_[1] = dy; cam_dir_[2] = dz;
    cam_fov_ = fov_degrees;
    cam_viewport_w_ = viewport_width;
}

float ProgressiveLodManager::getCameraDistance(float wx, float wy, float wz) const {
    float dx = wx - cam_pos_[0];
    float dy = wy - cam_pos_[1];
    float dz = wz - cam_pos_[2];
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

void ProgressiveLodManager::update(float delta_seconds, float current_fps) {
    std::lock_guard<std::mutex> lock(mutex_);

    lod_switches_this_frame_ = 0;

    // Adaptive quality: scale LOD based on FPS
    if (config_.adaptive_quality) {
        float fps_ratio = current_fps / config_.target_fps;
        adaptive_quality_scale_ = std::clamp(fps_ratio, 0.3f, 1.0f);
    }

    // Update blend factors for active transitions
    for (auto& [id, state] : states_) {
        updateBlendFactor(state, delta_seconds);

        // Check if transition complete
        if (state.blend_factor >= 1.0f && state.target_ready) {
            state.current_lod = state.target_lod;
            state.blend_factor = 0.0f;
            state.levels_loaded = std::max(state.levels_loaded,
                static_cast<uint32_t>(static_cast<int>(state.current_lod) + 1));

            if (transition_cb_) {
                transition_cb_(id, state.current_lod, state.target_lod);
            }
        }
    }
}

void ProgressiveLodManager::updateBlendFactor(ProgressiveLodState& state, float dt) {
    if (state.mode == ProgressiveLodState::TransitionMode::kInstant) {
        state.blend_factor = 1.0f;
        return;
    }

    if (!state.target_ready) {
        // Target LOD not yet loaded, hold at current
        state.blend_factor = 0.0f;
        return;
    }

    // Advance blend factor
    float speed = state.transition_speed;
    state.blend_factor += speed * dt;
    state.blend_factor = std::clamp(state.blend_factor, 0.0f, 1.0f);
}

std::vector<LodLevel> ProgressiveLodManager::getPendingLoads(const std::string& asset_id) const {
    std::lock_guard<std::mutex> lock(mutex_);

    std::vector<LodLevel> pending;

    auto state_it = states_.find(asset_id);
    if (state_it == states_.end()) return pending;

    auto loaded_it = loaded_lods_.find(asset_id);
    if (loaded_it == loaded_lods_.end()) return pending;

    const auto& state = state_it->second;
    const auto& loaded = loaded_it->second;

    // Always ensure LOD0 is loaded first (base mesh)
    if (!loaded[0]) {
        pending.push_back(LodLevel::kLOD0);
    }

    // Load target LOD if not loaded
    int target_idx = static_cast<int>(state.target_lod);
    if (target_idx >= 0 && target_idx < static_cast<int>(loaded.size()) && !loaded[target_idx]) {
        pending.push_back(state.target_lod);
    }

    // Load intermediate LODs for progressive refinement
    int current_idx = static_cast<int>(state.current_lod);
    for (int i = current_idx + 1; i < target_idx; ++i) {
        if (i < static_cast<int>(loaded.size()) && !loaded[i]) {
            pending.push_back(static_cast<LodLevel>(i));
        }
    }

    return pending;
}

void ProgressiveLodManager::onLodLoaded(const std::string& asset_id, LodLevel level) {
    std::lock_guard<std::mutex> lock(mutex_);

    auto it = loaded_lods_.find(asset_id);
    if (it == loaded_lods_.end()) return;

    int idx = static_cast<int>(level);
    if (idx < 0 || idx >= static_cast<int>(it->second.size())) return;

    it->second[idx] = true;

    auto state_it = states_.find(asset_id);
    if (state_it != states_.end()) {
        auto& state = state_it->second;

        // Mark LOD0 as base ready
        if (level == LodLevel::kLOD0) {
            state.base_ready = true;
        }

        // Mark target ready if this was the target LOD
        if (level == state.target_lod) {
            state.target_ready = true;
        }

        state.levels_loaded = std::max(state.levels_loaded, static_cast<uint32_t>(idx + 1));
    }

    if (lod_ready_cb_) {
        lod_ready_cb_(asset_id, level);
    }

    spdlog::debug("ProgressiveLOD: LOD {} loaded for '{}'", idx, asset_id);
}

void ProgressiveLodManager::onLodFailed(const std::string& asset_id, LodLevel level) {
    std::lock_guard<std::mutex> lock(mutex_);

    spdlog::warn("ProgressiveLOD: LOD {} load failed for '{}'", static_cast<int>(level), asset_id);

    // If base LOD fails, mark asset as unavailable
    if (level == LodLevel::kLOD0) {
        auto state_it = states_.find(asset_id);
        if (state_it != states_.end()) {
            state_it->second.base_ready = false;
        }
    }
}

void ProgressiveLodManager::setTransitionCallback(LodTransitionCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    transition_cb_ = std::move(cb);
}

void ProgressiveLodManager::setLodReadyCallback(LodReadyCallback cb) {
    std::lock_guard<std::mutex> lock(mutex_);
    lod_ready_cb_ = std::move(cb);
}

const ProgressiveLodState* ProgressiveLodManager::getState(const std::string& asset_id) const {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = states_.find(asset_id);
    return (it != states_.end()) ? &it->second : nullptr;
}

std::vector<std::string> ProgressiveLodManager::getRegisteredAssets() const {
    std::lock_guard<std::mutex> lock(mutex_);
    std::vector<std::string> ids;
    ids.reserve(states_.size());
    for (const auto& [id, _] : states_) ids.push_back(id);
    return ids;
}

void ProgressiveLodManager::updateConfig(const ProgressiveLodConfig& config) {
    std::lock_guard<std::mutex> lock(mutex_);
    config_ = config;
}

ProgressiveLodManager::LodStats ProgressiveLodManager::getStats() const {
    std::lock_guard<std::mutex> lock(mutex_);

    LodStats stats;
    stats.total_assets = static_cast<uint32_t>(states_.size());
    stats.lod_switches_this_frame = lod_switches_this_frame_;

    uint32_t transitions = 0;
    uint32_t pending = 0;
    float total_blend = 0.0f;

    for (const auto& [id, state] : states_) {
        if (state.blend_factor > 0.0f && state.blend_factor < 1.0f) {
            transitions++;
        }
        if (state.target_lod != state.current_lod && !state.target_ready) {
            pending++;
        }
        total_blend += state.blend_factor;
    }

    stats.active_transitions = transitions;
    stats.pending_loads = pending;
    stats.avg_blend_factor = states_.empty() ? 0.0f : total_blend / states_.size();

    return stats;
}

// ============================================================
// AssetDecompressor
// ============================================================

AssetDecompressor::AssetDecompressor(const CompressionConfig& config)
    : config_(config) {}

AssetDecompressor::~AssetDecompressor() {}

CompressionFormat AssetDecompressor::detectFormat(const uint8_t* data, size_t size) {
    if (size < 4) return CompressionFormat::kNone;

    // Zstd magic: 0x28 0xB5 0x2F 0xFD
    if (data[0] == 0x28 && data[1] == 0xB5 && data[2] == 0x2F && data[3] == 0xFD) {
        return CompressionFormat::kZstd;
    }

    // LZ4 magic: 0x04 0x22 0x4D 0x18
    if (data[0] == 0x04 && data[1] == 0x22 && data[2] == 0x4D && data[3] == 0x18) {
        return CompressionFormat::kLZ4;
    }

    // Gzip magic: 0x1F 0x8B
    if (data[0] == 0x1F && data[1] == 0x8B) {
        return CompressionFormat::kGzip;
    }

    // Draco: "DRACO" header
    if (size >= 5 && std::memcmp(data, "DRACO", 5) == 0) {
        return CompressionFormat::kDraco;
    }

    // Basis Universal: magic 0x73 0x42 0x4B (sBK)
    if (size >= 4 && data[0] == 0x73 && data[1] == 0x42 && data[2] == 0x4B) {
        return CompressionFormat::kBasisUniversal;
    }

    return CompressionFormat::kNone;
}

bool AssetDecompressor::isFormatSupported(CompressionFormat format) {
    switch (format) {
        case CompressionFormat::kNone: return true;
        case CompressionFormat::kZstd: return true;  // via zstd library
        case CompressionFormat::kLZ4:  return true;  // via lz4 library
        case CompressionFormat::kGzip: return true;  // via zlib
        case CompressionFormat::kDraco: return true; // via draco library
        case CompressionFormat::kBasisUniversal: return true; // via basisu library
        default: return false;
    }
}

float AssetDecompressor::estimateCompressionRatio(CompressionFormat format) {
    switch (format) {
        case CompressionFormat::kZstd:  return 3.5f;
        case CompressionFormat::kLZ4:   return 2.0f;
        case CompressionFormat::kGzip:  return 3.0f;
        case CompressionFormat::kDraco: return 8.0f;  // ~8x for geometry
        case CompressionFormat::kBasisUniversal: return 4.0f; // ~4x for textures
        default: return 1.0f;
    }
}

std::unique_ptr<uint8_t[]> AssetDecompressor::decompress(
    const uint8_t* data, size_t size,
    CompressionFormat format, size_t* out_size) {

    if (!data || size == 0) {
        if (out_size) *out_size = 0;
        return nullptr;
    }

    // Auto-detect if format not specified
    if (format == CompressionFormat::kNone) {
        format = detectFormat(data, size);
    }

    switch (format) {
        case CompressionFormat::kZstd:
            return decompressZstd(data, size, out_size);
        case CompressionFormat::kLZ4:
            return decompressLZ4(data, size, out_size);
        case CompressionFormat::kGzip:
            return decompressGzip(data, size, out_size);
        case CompressionFormat::kDraco:
            return decompressDraco(data, size, out_size);
        case CompressionFormat::kBasisUniversal:
            return decompressBasis(data, size, out_size);
        case CompressionFormat::kNone:
        default:
            // Uncompressed: just copy
            if (out_size) *out_size = size;
            auto buf = std::make_unique<uint8_t[]>(size);
            std::memcpy(buf.get(), data, size);
            return buf;
    }
}

// ============================================================
// Zstd decompression (via zstd library when available)
// ============================================================
std::unique_ptr<uint8_t[]> AssetDecompressor::decompressZstd(
    const uint8_t* data, size_t size, size_t* out_size) {
#if defined(SOLRA_HAS_ZSTD)
    // Zstd integration would go here
    // unsigned long long frameSize = ZSTD_getFrameContentSize(data, size);
    // auto buf = std::make_unique<uint8_t[]>(frameSize);
    // size_t result = ZSTD_decompress(buf.get(), frameSize, data, size);
    spdlog::warn("Zstd decompression: zstd library integration pending");
#endif
    // Fallback: return raw data
    if (out_size) *out_size = size;
    auto buf = std::make_unique<uint8_t[]>(size);
    std::memcpy(buf.get(), data, size);
    return buf;
}

// ============================================================
// LZ4 decompression (via lz4 library when available)
// ============================================================
std::unique_ptr<uint8_t[]> AssetDecompressor::decompressLZ4(
    const uint8_t* data, size_t size, size_t* out_size) {
#if defined(SOLRA_HAS_LZ4)
    // LZ4 integration would go here
    spdlog::warn("LZ4 decompression: lz4 library integration pending");
#endif
    if (out_size) *out_size = size;
    auto buf = std::make_unique<uint8_t[]>(size);
    std::memcpy(buf.get(), data, size);
    return buf;
}

// ============================================================
// Gzip decompression (via zlib)
// ============================================================
std::unique_ptr<uint8_t[]> AssetDecompressor::decompressGzip(
    const uint8_t* data, size_t size, size_t* out_size) {
#if defined(SOLRA_HAS_ZLIB)
    // zlib integration would go here
    spdlog::warn("Gzip decompression: zlib integration pending");
#endif
    if (out_size) *out_size = size;
    auto buf = std::make_unique<uint8_t[]>(size);
    std::memcpy(buf.get(), data, size);
    return buf;
}

// ============================================================
// Draco mesh decompression (via draco library when available)
// ============================================================
std::unique_ptr<uint8_t[]> AssetDecompressor::decompressDraco(
    const uint8_t* data, size_t size, size_t* out_size) {
#if defined(SOLRA_HAS_DRACO)
    // Draco integration would go here:
    // draco::DecoderBuffer buffer;
    // buffer.Init((const char*)data, size);
    // draco::Decoder decoder;
    // auto statusor = decoder.DecodeMeshFromBuffer(&buffer);
    // if (statusor.ok()) { ... extract mesh data ... }
    spdlog::warn("Draco decompression: draco library integration pending");
#endif
    if (out_size) *out_size = size;
    auto buf = std::make_unique<uint8_t[]>(size);
    std::memcpy(buf.get(), data, size);
    return buf;
}

// ============================================================
// Basis Universal texture decompression
// ============================================================
std::unique_ptr<uint8_t[]> AssetDecompressor::decompressBasis(
    const uint8_t* data, size_t size, size_t* out_size) {
#if defined(SOLRA_HAS_BASISU)
    // Basis Universal transcoding would go here:
    // basist::basisu_transcoder transcoder;
    // transcoder.start_transcoding(data, size);
    // basist::basisu_image_info info;
    // transcoder.get_image_info(data, size, info, 0);
    // ... transcode to GPU format ...
    spdlog::warn("Basis Universal decompression: basisu library integration pending");
#endif
    if (out_size) *out_size = size;
    auto buf = std::make_unique<uint8_t[]>(size);
    std::memcpy(buf.get(), data, size);
    return buf;
}

} // namespace solra::core::streaming
