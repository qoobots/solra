/*
 * Solra Core SDK - Core module implementation
 *
 * SDK initialization, lifecycle, and main loop management.
 */

#include <solra/solra_core.h>
#include <solra/solra_types.h>
#include <spdlog/spdlog.h>
#include <atomic>
#include <thread>
#include <chrono>

/* Internal state */
static struct {
  SolraCoreConfig config;
  SolraCoreState state;
  std::atomic<int> initialized{0};
  std::atomic<int> should_stop{0};
  std::atomic<int> should_pause{0};
  std::thread *main_thread{nullptr};
  double start_time{0.0};
} g_core;

/* Forward declaration */
static void main_loop(void);

int solra_core_init(const SolraCoreConfig *config) {
  if (g_core.initialized.load()) {
    return SOLRA_ERROR_ALREADY_INITIALIZED;
  }
  if (!config) {
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  /* Set up logging */
  spdlog::level::level_enum log_level;
  switch (config->log_level) {
    case 0: log_level = spdlog::level::trace; break;
    case 1: log_level = spdlog::level::debug; break;
    default:
    case 2: log_level = spdlog::level::info; break;
    case 3: log_level = spdlog::level::warn; break;
    case 4: log_level = spdlog::level::err; break;
  }
  spdlog::set_level(log_level);

  /* Store config */
  g_core.config = *config;

  /* Initialize state */
  g_core.state.initialized = 0;
  g_core.state.running = 0;
  g_core.state.paused = 0;
  g_core.state.frame_count = 0;
  g_core.state.elapsed_time_ms = 0.0;

  spdlog::info("Solra Core SDK v{} initializing", solra_core_get_version());
  spdlog::info("  Resolution: {}x{}", config->display_width, config->display_height);
  spdlog::info("  Target FPS: {}", config->target_fps);
  spdlog::info("  GPU enabled: {}", config->enable_gpu ? "yes" : "no (headless)");

  g_core.state.initialized = 1;
  g_core.initialized.store(1);

  return SOLRA_OK;
}

int solra_core_run(void) {
  if (!g_core.initialized.load()) {
    return SOLRA_ERROR_NOT_INITIALIZED;
  }

  g_core.should_stop.store(0);
  g_core.should_pause.store(0);
  g_core.state.running = 1;
  g_core.start_time = std::chrono::duration<double>(
    std::chrono::steady_clock::now().time_since_epoch()
  ).count();

  /* Run main loop synchronously (this function blocks) */
  main_loop();

  g_core.state.running = 0;
  return SOLRA_OK;
}

void solra_core_stop(void) {
  g_core.should_stop.store(1);
}

void solra_core_pause(void) {
  g_core.should_pause.store(1);
  g_core.state.paused = 1;
}

void solra_core_resume(void) {
  g_core.should_pause.store(0);
  g_core.state.paused = 0;
}

int solra_core_get_state(SolraCoreState *state) {
  if (!state) {
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }
  *state = g_core.state;
  return SOLRA_OK;
}

void solra_core_shutdown(void) {
  if (g_core.state.running) {
    solra_core_stop();
  }

  g_core.initialized.store(0);
  g_core.state.initialized = 0;

  spdlog::info("Solra Core SDK shutdown complete");
}

/* -------------------------------------------
 * Internal main loop
 * ------------------------------------------- */

static void main_loop(void) {
  using clock = std::chrono::steady_clock;
  auto frame_duration = std::chrono::microseconds(0);
  if (g_core.config.target_fps > 0) {
    frame_duration = std::chrono::microseconds(1000000 / g_core.config.target_fps);
  }

  auto last_frame_time = clock::now();
  uint64_t frame_count = 0;

  spdlog::info("Main loop started");

  while (!g_core.should_stop.load()) {
    auto frame_start = clock::now();

    if (!g_core.should_pause.load()) {
      /* ----- Frame Begin ----- */
      /* TODO: solra_render_begin_frame(); */

      /* ----- Update ----- */
      /* TODO: Update scene graph, physics, animation */

      /* ----- Frame End ----- */
      /* TODO: solra_render_end_frame(); */

      frame_count++;
    }

    /* Frame timing */
    auto frame_end = clock::now();
    auto frame_elapsed = frame_end - frame_start;

    /* Sleep if ahead of target frame rate */
    if (frame_duration.count() > 0 && frame_elapsed < frame_duration) {
      std::this_thread::sleep_for(frame_duration - frame_elapsed);
    }

    /* Update elapsed time */
    auto now = clock::now();
    g_core.state.frame_count = frame_count;
    g_core.state.elapsed_time_ms = std::chrono::duration<double, std::milli>(
      now.time_since_epoch()
    ).count() - g_core.start_time * 1000.0;

    last_frame_time = now;
  }

  spdlog::info("Main loop stopped after {} frames", frame_count);
}
