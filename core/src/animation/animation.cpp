/*
 * Solra Core SDK - Animation system (stub)
 */

#include <solra/solra_animation.h>
#include <spdlog/spdlog.h>

/* Blend shapes */
/* (uses types declared in public header) */

/* Lip Sync */
SolraLipSyncHandle solra_lipsync_create(int viseme_count) {
  spdlog::debug("LipSync created with {} visemes", viseme_count);
  return nullptr;
}

int solra_lipsync_process_phoneme(SolraLipSyncHandle, const char *phoneme, int duration_ms, float *weights) {
  return 0;
}

int solra_lipsync_process_audio(SolraLipSyncHandle, const int16_t *audio_samples, int sample_count, float *weights) {
  return 0;
}

void solra_lipsync_destroy(SolraLipSyncHandle) {}

/* IK Solver */
SolraIKSolverHandle solra_ik_solver_create(int bone_count) {
  spdlog::debug("IK solver created with {} bones", bone_count);
  return nullptr;
}

int solra_ik_solve_fabrik(SolraIKSolverHandle, const float *bone_lengths,
                           SolraVec3 *bone_local_positions,
                           const SolraVec3 *target_world,
                           const SolraVec3 *root_world) {
  return 0;
}

void solra_ik_solver_destroy(SolraIKSolverHandle) {}

/* Animation Clip */
SolraAnimationClipHandle solra_animation_clip_load(const char *path) {
  spdlog::debug("Animation clip loaded: {}", path);
  return nullptr;
}

int solra_animation_clip_evaluate(SolraAnimationClipHandle, float time_seconds,
                                   SolraMat4 *out_transforms, int max_bones) {
  return 0;
}

float solra_animation_clip_get_duration(SolraAnimationClipHandle) {
  return 0.0f;
}

void solra_animation_clip_destroy(SolraAnimationClipHandle) {}
