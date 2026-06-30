/*
 * Solra Core SDK - Streaming / Asset Loading API
 *
 * Efficient LOD-based streaming, chunked download, predictive preloading,
 * and local caching for 3D assets.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_STREAMING_H
#define SOLRA_STREAMING_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * Streaming Config
 * ============================================================ */

typedef struct SolraStreamingConfig {
  /** Base URL for asset CDN */
  const char *cdn_base_url;
  /** Max concurrent downloads */
  int max_concurrent_downloads;
  /** Chunk size in bytes for chunked downloads */
  int chunk_size_bytes;
  /** Max local cache size in MB */
  int max_cache_size_mb;
  /** Cache directory path (NULL = default) */
  const char *cache_path;
  /** Enable HTTP/3 + QUIC if available */
  int enable_http3;
  /** Enable predictive preloading */
  int enable_prefetch;
  /** Enable compression */
  int enable_compression;
  /** LOD bias (0.0 = full detail, -1.0 = always low, +1.0 = always high) */
  float lod_bias;
} SolraStreamingConfig;

/* ============================================================
 * Asset Types
 * ============================================================ */

typedef enum SolraAssetType {
  SOLRA_ASSET_TYPE_UNKNOWN = 0,
  SOLRA_ASSET_TYPE_SCENE = 1,      /* 3D scene (GLTF/GLB) */
  SOLRA_ASSET_TYPE_MESH = 2,       /* Mesh data */
  SOLRA_ASSET_TYPE_TEXTURE = 3,    /* Texture (PNG/KTX2) */
  SOLRA_ASSET_TYPE_MATERIAL = 4,   /* Material definition */
  SOLRA_ASSET_TYPE_AUDIO = 5,      /* Audio clip */
  SOLRA_ASSET_TYPE_ANIMATION = 6,  /* Animation clip */
  SOLRA_ASSET_TYPE_SCRIPT = 7,     /* Lua/behavior script */
} SolraAssetType;

typedef enum SolraAssetStatus {
  SOLRA_ASSET_STATUS_NOT_LOADED = 0,
  SOLRA_ASSET_STATUS_QUEUED = 1,
  SOLRA_ASSET_STATUS_DOWNLOADING = 2,
  SOLRA_ASSET_STATUS_READY = 3,
  SOLRA_ASSET_STATUS_FAILED = 4,
} SolraAssetStatus;

typedef struct SolraAssetInfo {
  char asset_id[SOLRA_ASSET_ID_MAX_LEN];
  char url[SOLRA_ASSET_URL_MAX_LEN];
  SolraAssetType type;
  SolraAssetStatus status;
  size_t total_size_bytes;
  size_t loaded_size_bytes;
  int lod_level;
  float load_progress;  /* 0.0 to 1.0 */
} SolraAssetInfo;

/** Opaque handle to a streamed asset */
typedef struct SolraAsset *SolraAssetHandle;

/** Opaque handle to a download request */
typedef struct SolraDownload *SolraDownloadHandle;

/* ============================================================
 * Streaming Engine Lifecycle
 * ============================================================ */

/**
 * Initialize the streaming engine.
 *
 * @param config Streaming configuration (NULL = sensible defaults).
 * @return 0 on success, negative on failure.
 */
SOLRA_API int solra_streaming_init(const SolraStreamingConfig *config);

/**
 * Shutdown the streaming engine. Cancels all pending downloads.
 */
SOLRA_API void solra_streaming_shutdown(void);

/* ============================================================
 * Asset Loading
 * ============================================================ */

/**
 * Request an asset for download (non-blocking).
 *
 * The asset will be downloaded, decompressed, and cached automatically.
 *
 * @param asset_id Unique asset identifier.
 * @param url Download URL (may be relative to cdn_base_url).
 * @param type Asset type hint.
 * @param priority Higher values = load sooner (0 = lowest).
 * @return Download handle for tracking progress, or NULL on error.
 */
SOLRA_API SolraDownloadHandle solra_streaming_load_async(
  const char *asset_id,
  const char *url,
  SolraAssetType type,
  int priority
);

/**
 * Request an asset for download (blocking).
 *
 * Blocks until the asset is fully loaded or fails.
 *
 * @param asset_id Unique asset identifier.
 * @param url Download URL.
 * @param type Asset type hint.
 * @param timeout_ms Timeout in milliseconds (0 = no timeout).
 * @return Asset handle, or NULL on failure.
 */
SOLRA_API SolraAssetHandle solra_streaming_load_sync(
  const char *asset_id,
  const char *url,
  SolraAssetType type,
  int timeout_ms
);

/**
 * Get the status of a pending download.
 *
 * @param download Download handle from solra_streaming_load_async().
 * @param info Non-null pointer to receive current info.
 * @return 0 on success.
 */
SOLRA_API int solra_streaming_get_status(SolraDownloadHandle download, SolraAssetInfo *info);

/**
 * Set priority of a queued/downloading asset.
 */
SOLRA_API void solra_streaming_set_priority(SolraDownloadHandle download, int priority);

/**
 * Cancel a pending download.
 *
 * @param download Download handle to cancel.
 */
SOLRA_API void solra_streaming_cancel(SolraDownloadHandle download);

/* ============================================================
 * LOD Management
 * ============================================================ */

/**
 * Set the target LOD level for a specific asset.
 *
 * Higher LOD = more detail, more data. The streaming engine
 * will load the appropriate LOD chunk.
 *
 * @param asset_id Asset to adjust.
 * @param lod_level Target LOD level (0 = highest, higher = lower detail).
 */
SOLRA_API void solra_streaming_set_lod(const char *asset_id, int lod_level);

/**
 * Set global LOD bias.
 *
 * @param bias -1.0 (always lowest) to +1.0 (always highest).
 */
SOLRA_API void solra_streaming_set_lod_bias(float bias);

/* ============================================================
 * Cache Management
 * ============================================================ */

/**
 * Check if an asset exists in the local cache.
 *
 * @param asset_id Asset to check.
 * @return 1 if cached, 0 if not.
 */
SOLRA_API int solra_streaming_is_cached(const char *asset_id);

/**
 * Get the current cache size.
 *
 * @return Cache size in bytes.
 */
SOLRA_API size_t solra_streaming_get_cache_size(void);

/**
 * Clear the local asset cache.
 *
 * @param keep_recent If non-zero, keep assets used in the last N minutes.
 */
SOLRA_API void solra_streaming_clear_cache(int keep_recent);

/**
 * Prefetch a list of assets into cache (low priority, background).
 *
 * @param asset_ids Array of asset ID strings.
 * @param count Number of assets to prefetch.
 * @param urls Parallel array of URLs (same length as asset_ids).
 */
SOLRA_API void solra_streaming_prefetch(const char **asset_ids, const char **urls, int count);

/* ============================================================
 * Progress Callback
 * ============================================================ */

/**
 * Global streaming progress callback.
 *
 * @param asset_id Asset being loaded.
 * @param progress 0.0 to 1.0.
 * @param status Current asset status.
 * @param user_data Opaque user data.
 */
typedef void (*SolraStreamingProgressCallback)(
  const char *asset_id, float progress, SolraAssetStatus status, void *user_data
);

SOLRA_API void solra_streaming_set_progress_callback(SolraStreamingProgressCallback callback, void *user_data);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_STREAMING_H */
