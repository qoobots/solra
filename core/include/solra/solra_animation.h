/*
 * Solra Core SDK - Animation API
 *
 * Face blend shapes, lip-sync, and inverse kinematics for virtual avatars.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_ANIMATION_H
#define SOLRA_ANIMATION_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * Blend Shape Configuration
 * ============================================================ */

/** Maximum number of blend shapes per face mesh */
#define SOLRA_MAX_BLEND_SHAPES 64

typedef struct SolraBlendShapeConfig {
  /** Number of active blend shapes */
  int count;
  /** Blend shape names (e.g. "jawOpen", "mouthSmile") */
  const char *names[SOLRA_MAX_BLEND_SHAPES];
  /** Target weights per blend shape (0.0 to 1.0) */
  float weights[SOLRA_MAX_BLEND_SHAPES];
} SolraBlendShapeConfig;

/**
 * Reset all blend shape weights to zero.
 */
SOLRA_API void solra_blendshape_reset(SolraBlendShapeConfig *config);

/**
 * Set a single blend shape weight by name.
 *
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_blendshape_set_weight(
    SolraBlendShapeConfig *config, const char *name, float weight);

/**
 * Get a single blend shape weight by name.
 */
SOLRA_API float solra_blendshape_get_weight(
    const SolraBlendShapeConfig *config, const char *name);

/* ============================================================
 * Lip Sync
 * ============================================================ */

/** Opaque handle to a lip-sync engine */
typedef struct SolraLipSync *SolraLipSyncHandle;

/**
 * Create a lip-sync engine for a specific avatar.
 *
 * @param viseme_count Number of viseme targets in the avatar mesh.
 * @return Lip-sync handle, or NULL on failure.
 */
SOLRA_API SolraLipSyncHandle solra_lipsync_create(int viseme_count);

/**
 * Process an audio phoneme and update viseme weights.
 *
 * @param lipsync Lip-sync engine.
 * @param phoneme IPA phoneme string (e.g. "AA", "M", "S").
 * @param duration_ms Duration of this phoneme in ms.
 * @param weights Output array of viseme weights (size = viseme_count).
 * @return 0 on success.
 */
SOLRA_API int solra_lipsync_process_phoneme(
  SolraLipSyncHandle lipsync,
  const char *phoneme,
  int duration_ms,
  float *weights
);

/**
 * Process raw audio data to extract phonemes and update visemes.
 *
 * @param lipsync Lip-sync engine.
 * @param audio_samples PCM audio data (16-bit signed, 16kHz mono).
 * @param sample_count Number of samples.
 * @param weights Output array of viseme weights.
 * @return 0 on success.
 */
SOLRA_API int solra_lipsync_process_audio(
  SolraLipSyncHandle lipsync,
  const int16_t *audio_samples,
  int sample_count,
  float *weights
);

/**
 * Destroy a lip-sync engine.
 */
SOLRA_API void solra_lipsync_destroy(SolraLipSyncHandle lipsync);

/* ============================================================
 * Inverse Kinematics
 * ============================================================ */

/** Opaque handle to an IK solver */
typedef struct SolraIKSolver *SolraIKSolverHandle;

/**
 * Create an IK solver for a skeletal chain.
 *
 * @param bone_count Number of bones in the chain.
 * @return IK solver handle, or NULL on failure.
 */
SOLRA_API SolraIKSolverHandle solra_ik_solver_create(int bone_count);

/**
 * Solve IK for a target position.
 *
 * @param solver IK solver.
 * @param bone_lengths Array of bone segment lengths (size = bone_count).
 * @param bone_local_positions Input/output: current positions, updated on return.
 * @param target_world Target position in world space.
 * @param root_world Root joint position in world space.
 * @return 0 if converged, positive = iterations used, negative = failed.
 */
SOLRA_API int solra_ik_solve_fabrik(
  SolraIKSolverHandle solver,
  const float *bone_lengths,
  SolraVec3 *bone_local_positions,
  const SolraVec3 *target_world,
  const SolraVec3 *root_world
);

/**
 * Destroy an IK solver.
 */
SOLRA_API void solra_ik_solver_destroy(SolraIKSolverHandle solver);

/* ============================================================
 * Animation Clip
 * ============================================================ */

/** Opaque handle to an animation clip */
typedef struct SolraAnimationClip *SolraAnimationClipHandle;

/**
 * Load an animation clip from file.
 *
 * @param path File path (GLTF animation or custom format).
 * @return Animation clip handle, or NULL on failure.
 */
SOLRA_API SolraAnimationClipHandle solra_animation_clip_load(const char *path);

/**
 * Evaluate an animation clip at a given time.
 *
 * @param clip Animation clip.
 * @param time_seconds Time to evaluate at.
 * @param out_transforms Output array of bone transforms (mat4, count = bone_count).
 * @param max_bones Maximum number of bones in output array.
 * @return Number of bones written, or negative on error.
 */
SOLRA_API int solra_animation_clip_evaluate(
  SolraAnimationClipHandle clip,
  float time_seconds,
  SolraMat4 *out_transforms,
  int max_bones
);

/**
 * Get the duration of an animation clip in seconds.
 */
SOLRA_API float solra_animation_clip_get_duration(SolraAnimationClipHandle clip);

/**
 * Destroy an animation clip.
 */
SOLRA_API void solra_animation_clip_destroy(SolraAnimationClipHandle clip);

/* ============================================================
 * Animation Controller (playback)
 * ============================================================ */

/**
 * Play an animation clip with optional crossfade.
 *
 * @param clip Animation clip handle.
 * @param clip_name Name of the sub-clip to play.
 * @param crossfade_duration Crossfade duration in seconds (0 = instant).
 * @param speed Playback speed multiplier (1.0 = normal).
 * @param loop Whether to loop the animation.
 * @return 0 on success.
 */
SOLRA_API int solra_animation_play(
    SolraAnimationClipHandle clip,
    const char *clip_name,
    float crossfade_duration,
    float speed,
    int loop);

/**
 * Advance the animation controller by delta time.
 *
 * @param clip Animation clip handle.
 * @param delta_time Elapsed time in seconds.
 * @return 0 on success.
 */
SOLRA_API int solra_animation_update(
    SolraAnimationClipHandle clip,
    float delta_time);

/**
 * Get the number of bones in the animation clip skeleton.
 */
SOLRA_API int solra_animation_get_bone_count(SolraAnimationClipHandle clip);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_ANIMATION_H */
