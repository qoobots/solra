/*
 * Solra Core SDK - Main entry header
 *
 * This is the primary public header for the Solra Core SDK.
 * Include this to access all core functionality.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_CORE_H
#define SOLRA_CORE_H

#include <solra/solra_types.h>
#include <solra/solra_render.h>
#include <solra/solra_inference.h>
#include <solra/solra_streaming.h>
#include <solra/solra_webrtc.h>
#include <solra/solra_animation.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * SolraCoreConfig - Initialization configuration for the Core SDK.
 *
 * Set up the initial state including platform-specific settings
 * before starting the engine loop.
 */
typedef struct SolraCoreConfig {
  /** Application bundle/data path for asset loading */
  const char *data_path;
  /** Desired display width in pixels */
  int display_width;
  /** Desired display height in pixels */
  int display_height;
  /** Target frame rate (0 = unlimited) */
  int target_fps;
  /** Enable GPU rendering (set false for headless mode) */
  int enable_gpu;
  /** Max logging level: 0=trace, 1=debug, 2=info, 3=warn, 4=error */
  int log_level;
  /** User data pointer passed to callbacks */
  void *user_data;
} SolraCoreConfig;

/**
 * SolraCoreState - Runtime state of the Core SDK.
 */
typedef struct SolraCoreState {
  /** Whether the engine is initialized */
  int initialized;
  /** Whether the engine is currently running */
  int running;
  /** Whether the engine is paused */
  int paused;
  /** Current frame number since start */
  uint64_t frame_count;
  /** Elapsed time since start in milliseconds */
  double elapsed_time_ms;
} SolraCoreState;

/**
 * Initialize the Solra Core SDK.
 *
 * Must be called once before any other SDK function.
 *
 * @param config Non-null pointer to initialization config.
 * @return 0 on success, negative error code on failure.
 */
int solra_core_init(const SolraCoreConfig *config);

/**
 * Start the main engine loop.
 *
 * This is a blocking call that runs until solra_core_stop() is called
 * from another thread or the application terminates.
 *
 * @return 0 on normal shutdown, negative error code on error.
 */
int solra_core_run(void);

/**
 * Request the engine loop to stop.
 *
 * Thread-safe. Can be called from any thread.
 */
void solra_core_stop(void);

/**
 * Pause the engine (rendering and simulation halted).
 */
void solra_core_pause(void);

/**
 * Resume a paused engine.
 */
void solra_core_resume(void);

/**
 * Get the current engine state.
 *
 * @param state Non-null pointer to receive current state.
 * @return 0 on success, negative error code on failure.
 */
int solra_core_get_state(SolraCoreState *state);

/**
 * Shutdown and cleanup the Solra Core SDK.
 *
 * Must be called when done using the SDK. Frees all resources.
 */
void solra_core_shutdown(void);

/**
 * Get the SDK version string.
 *
 * @return Null-terminated version string (e.g. "0.1.0").
 */
const char *solra_core_get_version(void);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_CORE_H */
