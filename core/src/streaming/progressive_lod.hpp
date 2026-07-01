/*
 * Solra Core SDK - Progressive LOD System
 *
 * Progressive Level-of-Detail: seamlessly transitions between LOD levels
 * with cross-fade blending, dithering, and geometry morph targets.
 * Supports both discrete LOD switching and continuous geometry streaming.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include "lod_streaming.hpp"
#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <unordered_map>
#include <array>
#include <chrono>

namespace solra::core::streaming {

// ============================================================
// Progressive LOD State
// ============================================================

/// Per-asset progressive LOD state machine
struct ProgressiveLodState {
    /// Current active LOD level (fully loaded)
    LodLevel current_lod = LodLevel::kNone;

    /// Target LOD level (being transitioned to)
    LodLevel target_lod = LodLevel::kNone;

    /// Next LOD loading (one step finer than current)
    LodLevel loading_lod = LodLevel::kNone;

    /// Transition blend factor: 0 = current LOD, 1 = target LOD
    float blend_factor = 0.0f;

    /// Transition speed (units per second)
    float transition_speed = 2.0f; // default 0.5s cross-fade

    /// Distance to camera (meters), updated each frame
    float camera_distance = 0.0f;

    /// Whether the asset is fully loaded at target LOD
    bool target_ready = false;

    /// Whether the base LOD0 is loaded (minimum viable)
    bool base_ready = false;

    /// Time when transition started
    std::chrono::steady_clock::time_point transition_start;

    /// Number of LOD levels loaded
    uint32_t levels_loaded = 0;

    /// Maximum LOD levels for this asset
    uint32_t max_levels = 4;

    /// Transition mode
    enum class TransitionMode : uint8_t {
        kInstant,       // No blending (pop)
        kCrossFade,     // Alpha cross-fade between two LODs
        kDither,        // Screen-door dither for transparency
        kMorphTarget,   // Geometry morph between LOD meshes
    };
    TransitionMode mode = TransitionMode::kCrossFade;
};

// ============================================================
// Progressive LOD Config
// ============================================================

struct ProgressiveLodConfig {
    /// Distance thresholds for each LOD level (meters)
    std::array<float, 4> distance_thresholds = {5.0f, 20.0f, 50.0f, 100.0f};

    /// Hysteresis margin to prevent LOD flickering (meters)
    float hysteresis = 2.0f;

    /// Default cross-fade duration (seconds)
    float fade_duration = 0.5f;

    /// Maximum concurrent progressive loads
    uint32_t max_concurrent_loads = 4;

    /// Bias toward higher quality (0=performance, 1=quality)
    float quality_bias = 0.7f;

    /// Screen-size threshold for LOD selection (% of viewport)
    float screen_size_threshold = 0.02f;

    /// Enable dither-based transparency for LOD transitions
    bool enable_dither = true;

    /// Enable geometry morphing (requires pre-computed morph targets)
    bool enable_morph = false;

    /// Enable automatic LOD adjustment based on FPS
    bool adaptive_quality = true;

    /// Target FPS for adaptive quality
    float target_fps = 30.0f;
};

// ============================================================
// Progressive LOD Manager
// ============================================================

class ProgressiveLodManager {
public:
    explicit ProgressiveLodManager(const ProgressiveLodConfig& config = {});
    ~ProgressiveLodManager();

    // === Registration ===
    /// Register an asset for progressive LOD management
    void registerAsset(const std::string& asset_id, uint32_t max_lod_levels = 4);

    /// Unregister an asset
    void unregisterAsset(const std::string& asset_id);

    // === LOD Selection ===
    /// Select the best LOD level based on distance and screen coverage
    LodLevel selectLod(const std::string& asset_id,
                       float camera_distance,
                       float screen_coverage = 1.0f) const;

    /// Request a LOD level transition
    void requestLod(const std::string& asset_id, LodLevel target);

    /// Get current effective LOD (accounting for blend state)
    LodLevel getEffectiveLod(const std::string& asset_id) const;

    /// Get blend factor for cross-fading (0=current LOD, 1=target LOD)
    float getBlendFactor(const std::string& asset_id) const;

    /// Check if a specific LOD level is loaded
    bool isLodLoaded(const std::string& asset_id, LodLevel level) const;

    // === Camera ===
    /// Update camera parameters (called each frame)
    void updateCamera(float pos_x, float pos_y, float pos_z,
                      float dir_x, float dir_y, float dir_z,
                      float fov_degrees, float viewport_width);

    /// Get distance from camera to a world-space point
    float getCameraDistance(float wx, float wy, float wz) const;

    // === Per-frame Update ===
    /// Update all LOD states, trigger transitions, update blend factors
    /// @param delta_seconds Frame delta time
    /// @param current_fps Current FPS for adaptive quality
    void update(float delta_seconds, float current_fps = 30.0f);

    // === Progressive Loading ===
    /// Get the list of LOD levels that need to be loaded for an asset
    /// (ordered by priority: LOD0 first, then progressively finer)
    std::vector<LodLevel> getPendingLoads(const std::string& asset_id) const;

    /// Notify that a LOD level has finished loading
    void onLodLoaded(const std::string& asset_id, LodLevel level);

    /// Notify that a LOD level load failed
    void onLodFailed(const std::string& asset_id, LodLevel level);

    // === Callbacks ===
    using LodTransitionCallback = std::function<void(const std::string& asset_id,
                                                      LodLevel from, LodLevel to)>;
    using LodReadyCallback = std::function<void(const std::string& asset_id, LodLevel level)>;

    void setTransitionCallback(LodTransitionCallback cb);
    void setLodReadyCallback(LodReadyCallback cb);

    // === Query ===
    /// Get the state for an asset
    const ProgressiveLodState* getState(const std::string& asset_id) const;

    /// Get all registered asset IDs
    std::vector<std::string> getRegisteredAssets() const;

    /// Get current config
    const ProgressiveLodConfig& config() const { return config_; }

    /// Update config at runtime
    void updateConfig(const ProgressiveLodConfig& config);

    // === Stats ===
    struct LodStats {
        uint32_t active_transitions = 0;
        uint32_t pending_loads = 0;
        uint32_t total_assets = 0;
        float avg_blend_factor = 0.0f;
        uint32_t lod_switches_this_frame = 0;
    };
    LodStats getStats() const;

private:
    ProgressiveLodConfig config_;
    std::unordered_map<std::string, ProgressiveLodState> states_;
    std::unordered_map<std::string, std::vector<bool>> loaded_lods_; // asset_id -> [lod_level] = loaded

    // Camera state
    float cam_pos_[3] = {0, 0, 0};
    float cam_dir_[3] = {0, 0, -1};
    float cam_fov_ = 60.0f;
    float cam_viewport_w_ = 1920.0f;

    // Callbacks
    LodTransitionCallback transition_cb_;
    LodReadyCallback lod_ready_cb_;

    // Adaptive quality
    float adaptive_quality_scale_ = 1.0f;

    // Stats
    uint32_t lod_switches_this_frame_ = 0;

    mutable std::mutex mutex_;

    // Helpers
    LodLevel computeTargetLod(float distance, float screen_coverage) const;
    void startTransition(ProgressiveLodState& state, LodLevel target);
    void updateBlendFactor(ProgressiveLodState& state, float dt);
    ProgressiveLodState::TransitionMode selectTransitionMode(LodLevel from, LodLevel to) const;
};

// ============================================================
// Asset Compression Support
// ============================================================

/// Supported compression formats for asset streaming
enum class CompressionFormat : uint8_t {
    kNone,          // Uncompressed
    kZstd,          // Zstandard (general purpose, fast decode)
    kLZ4,           // LZ4 (fastest, lower ratio)
    kDraco,         // Google Draco (mesh geometry)
    kBasisUniversal,// Binomial Basis Universal (texture)
    kGzip,          // Gzip (compatibility)
};

/// Compression configuration per asset type
struct CompressionConfig {
    CompressionFormat mesh_format = CompressionFormat::kDraco;
    CompressionFormat texture_format = CompressionFormat::kBasisUniversal;
    CompressionFormat general_format = CompressionFormat::kZstd;

    /// Compression level (1=fastest, 22=best ratio for zstd)
    int compression_level = 3;

    /// Draco-specific: quantization bits for position
    int draco_position_quantization = 11;

    /// Draco-specific: quantization bits for normal
    int draco_normal_quantization = 8;

    /// Draco-specific: quantization bits for UV
    int draco_texcoord_quantization = 10;

    /// Basis Universal: enable ETC1S mode (better for GPU transcoding)
    bool basis_etc1s = true;

    /// Basis Universal: quality level (0-255)
    int basis_quality = 128;

    /// Minimum size in bytes to apply compression (skip tiny assets)
    uint64_t min_compress_size = 1024;
};

/// Asset decompressor for streaming pipeline
class AssetDecompressor {
public:
    explicit AssetDecompressor(const CompressionConfig& config = {});
    ~AssetDecompressor();

    /// Decompress a buffer using the detected format
    /// @param data Input compressed data
    /// @param size Input size
    /// @param format Compression format (auto-detect if kNone)
    /// @param out_size Output: decompressed size
    /// @return Decompressed data (caller owns), or nullptr on failure
    std::unique_ptr<uint8_t[]> decompress(const uint8_t* data, size_t size,
                                           CompressionFormat format,
                                           size_t* out_size);

    /// Auto-detect compression format from magic bytes
    static CompressionFormat detectFormat(const uint8_t* data, size_t size);

    /// Check if a format is supported at runtime
    static bool isFormatSupported(CompressionFormat format);

    /// Get the typical compression ratio for a format (for bandwidth estimation)
    static float estimateCompressionRatio(CompressionFormat format);

private:
    CompressionConfig config_;

    std::unique_ptr<uint8_t[]> decompressZstd(const uint8_t* data, size_t size, size_t* out_size);
    std::unique_ptr<uint8_t[]> decompressLZ4(const uint8_t* data, size_t size, size_t* out_size);
    std::unique_ptr<uint8_t[]> decompressGzip(const uint8_t* data, size_t size, size_t* out_size);
    std::unique_ptr<uint8_t[]> decompressDraco(const uint8_t* data, size_t size, size_t* out_size);
    std::unique_ptr<uint8_t[]> decompressBasis(const uint8_t* data, size_t size, size_t* out_size);
};

} // namespace solra::core::streaming
