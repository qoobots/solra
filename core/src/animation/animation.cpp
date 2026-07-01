/*
 * Solra Core SDK - Animation system (C API implementation)
 *
 * Wires up the underlying C++ animation classes:
 *   - solra::render::Skeleton / AnimationClip / AnimationController
 *   - solra::animation::BlendShapeWeights / ExpressionLibrary
 *   - solra::animation::PhonemeVisemeMap / LipSyncEngine
 *   - solra::animation::LimbIKSolver / FullBodyIK
 */

#include <solra/solra_animation.h>
#include "skeletal_animation.hpp"
#include "blendshape.hpp"
#include "lipsync.hpp"
#include "limb_ik.hpp"

#include <spdlog/spdlog.h>
#include <unordered_map>
#include <mutex>
#include <memory>
#include <cstring>
#include <vector>

/* ============================================================
 * Internal state managers
 * ============================================================ */

static std::mutex g_anim_mutex;

/* ============================================================
 * Blend Shapes
 * ============================================================ */

void solra_blendshape_reset(SolraBlendShapeConfig* config) {
  if (!config) return;
  config->count = 0;
  for (int i = 0; i < SOLRA_MAX_BLEND_SHAPES; ++i) {
    config->names[i] = nullptr;
    config->weights[i] = 0.0f;
  }
}

int solra_blendshape_set_weight(SolraBlendShapeConfig* config, const char* name, float weight) {
  if (!config || !name) return -1;

  // Find existing slot or add new
  for (int i = 0; i < config->count; ++i) {
    if (config->names[i] && std::strcmp(config->names[i], name) == 0) {
      config->weights[i] = std::clamp(weight, 0.0f, 1.0f);
      return 0;
    }
  }

  if (config->count >= SOLRA_MAX_BLEND_SHAPES) {
    spdlog::warn("Blend shape limit reached ({}), ignoring: {}", SOLRA_MAX_BLEND_SHAPES, name);
    return -1;
  }

  int idx = config->count++;
  config->names[idx] = name; // caller must keep string alive
  config->weights[idx] = std::clamp(weight, 0.0f, 1.0f);
  return 0;
}

float solra_blendshape_get_weight(const SolraBlendShapeConfig* config, const char* name) {
  if (!config || !name) return 0.0f;
  for (int i = 0; i < config->count; ++i) {
    if (config->names[i] && std::strcmp(config->names[i], name) == 0)
      return config->weights[i];
  }
  return 0.0f;
}

/* ============================================================
 * Lip Sync
 * ============================================================ */

struct LipSyncInstance {
  std::unique_ptr<solra::animation::LipSyncEngine> engine;
  int visemeCount;
};

static std::unordered_map<SolraLipSyncHandle, std::shared_ptr<LipSyncInstance>> g_lipsyncs;
static uintptr_t g_next_lipsync_handle = 1;

SolraLipSyncHandle solra_lipsync_create(int viseme_count) {
  if (viseme_count <= 0 || viseme_count > 64) {
    spdlog::error("LipSync: invalid viseme count {}", viseme_count);
    return nullptr;
  }

  auto inst = std::make_shared<LipSyncInstance>();
  inst->engine = std::make_unique<solra::animation::LipSyncEngine>();
  inst->visemeCount = viseme_count;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  SolraLipSyncHandle handle = reinterpret_cast<SolraLipSyncHandle>(g_next_lipsync_handle++);
  g_lipsyncs[handle] = inst;

  spdlog::info("LipSync created: {} visemes", viseme_count);
  return handle;
}

int solra_lipsync_process_phoneme(
    SolraLipSyncHandle lipsync,
    const char* phoneme,
    int duration_ms,
    float* weights) {
  if (!lipsync || !phoneme || !weights) return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_lipsyncs.find(lipsync);
  if (it == g_lipsyncs.end()) return -1;

  auto& engine = *it->second->engine;

  // Map ARPABET phoneme to Viseme
  auto viseme = solra::animation::PhonemeVisemeMap::instance().phonemeToViseme(phoneme);

  // Build phoneme timeline event
  solra::animation::PhonemeEvent event;
  event.viseme = viseme;
  event.startTime = 0.0f;
  event.endTime = static_cast<float>(duration_ms) / 1000.0f;
  event.confidence = 1.0f;

  std::vector<solra::animation::PhonemeEvent> timeline = { event };
  engine.setPhonemeTimeline(timeline);
  engine.update(event.endTime);

  // Extract weights
  const auto& bw = engine.currentWeights();
  int count = it->second->visemeCount;
  for (int i = 0; i < count; ++i) {
    weights[i] = bw[static_cast<size_t>(i)];
  }

  return 0;
}

int solra_lipsync_process_audio(
    SolraLipSyncHandle lipsync,
    const int16_t* audio_samples,
    int sample_count,
    float* weights) {
  if (!lipsync || !audio_samples || !weights) return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_lipsyncs.find(lipsync);
  if (it == g_lipsyncs.end()) return -1;

  // Simplified audio processing: detect energy level to drive jaw open
  // Full phoneme extraction would need a speech recognition backend
  float energy = 0.0f;
  for (int i = 0; i < sample_count; ++i) {
    float s = static_cast<float>(audio_samples[i]) / 32768.0f;
    energy += s * s;
  }
  energy = std::sqrt(energy / static_cast<float>(sample_count));

  // Map energy to jaw opening
  auto& engine = *it->second->engine;
  engine.setIntensity(std::min(energy * 8.0f, 1.5f));

  solra::animation::PhonemeEvent event;
  event.viseme = (energy > 0.01f)
    ? solra::animation::Viseme::AE  // default open mouth for audio
    : solra::animation::Viseme::Silence;
  event.startTime = 0.0f;
  event.endTime = 0.016f; // ~60fps frame
  event.confidence = std::min(energy * 10.0f, 1.0f);

  std::vector<solra::animation::PhonemeEvent> timeline = { event };
  engine.setPhonemeTimeline(timeline);
  engine.update(0.016f);

  const auto& bw = engine.currentWeights();
  int count = it->second->visemeCount;
  for (int i = 0; i < count; ++i) {
    weights[i] = bw[static_cast<size_t>(i)];
  }

  return 0;
}

void solra_lipsync_destroy(SolraLipSyncHandle lipsync) {
  if (!lipsync) return;
  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_lipsyncs.find(lipsync);
  if (it != g_lipsyncs.end()) {
    spdlog::debug("LipSync destroyed: {}", reinterpret_cast<uintptr_t>(lipsync));
    g_lipsyncs.erase(it);
  }
}

/* ============================================================
 * IK Solver
 * ============================================================ */

struct IKSolverInstance {
  std::unique_ptr<solra::animation::LimbIKSolver> solver;
  int boneCount;
  std::string chainName;
};

static std::unordered_map<SolraIKSolverHandle, std::shared_ptr<IKSolverInstance>> g_iksolvers;
static uintptr_t g_next_ik_handle = 1;

SolraIKSolverHandle solra_ik_solver_create(int bone_count) {
  if (bone_count < 2 || bone_count > 32) {
    spdlog::error("IK solver: invalid bone count {}", bone_count);
    return nullptr;
  }

  auto inst = std::make_shared<IKSolverInstance>();
  solra::animation::IKSolverConfig config;
  config.solver = solra::animation::IKSolverType::FABRIK;
  config.enforce_joint_limits = true;
  inst->solver = std::make_unique<solra::animation::LimbIKSolver>(config);
  inst->boneCount = bone_count;

  // Create a simple chain
  solra::animation::IKChain chain;
  chain.name = "main";
  for (int i = 0; i < bone_count; ++i) {
    chain.joint_indices.push_back(i);
  }
  chain.root = 0;
  chain.effector = bone_count - 1;
  chain.tolerance = 0.001f;
  chain.max_iterations = 20;
  inst->solver->register_chain(chain);
  inst->chainName = "main";

  // Build skeleton joints from bone positions
  std::vector<solra::animation::Joint> joints(bone_count);
  for (int i = 0; i < bone_count; ++i) {
    joints[i].name = "bone_" + std::to_string(i);
    joints[i].position = solra::animation::Vec3{static_cast<float>(i), 0.0f, 0.0f};
    joints[i].length = 1.0f;
  }
  inst->solver->update_joints(joints);

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  SolraIKSolverHandle handle = reinterpret_cast<SolraIKSolverHandle>(g_next_ik_handle++);
  g_iksolvers[handle] = inst;

  spdlog::info("IK solver created: {} bones (FABRIK)", bone_count);
  return handle;
}

int solra_ik_solve_fabrik(
    SolraIKSolverHandle solver,
    const float* bone_lengths,
    SolraVec3* bone_local_positions,
    const SolraVec3* target_world,
    const SolraVec3* root_world) {
  if (!solver || !bone_lengths || !bone_local_positions || !target_world || !root_world)
    return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_iksolvers.find(solver);
  if (it == g_iksolvers.end()) return -1;

  auto& inst = *it->second;
  int n = inst.boneCount;

  // Update joint positions from input
  std::vector<solra::animation::Joint> joints(n);
  for (int i = 0; i < n; ++i) {
    joints[i].name = "bone_" + std::to_string(i);
    joints[i].position = solra::animation::Vec3{
      bone_local_positions[i].x,
      bone_local_positions[i].y,
      bone_local_positions[i].z
    };
    joints[i].length = bone_lengths[i];
    if (i > 0) joints[i].parent_idx = i - 1;
  }

  inst.solver->update_joints(joints);

  // Set target
  solra::animation::Vec3 target{target_world->x, target_world->y, target_world->z};
  inst.solver->set_target(inst.chainName, target);

  // Solve
  auto result = inst.solver->solve_chain(inst.chainName);

  // Write back results
  const auto& solved = inst.solver->get_joints();
  for (int i = 0; i < n && i < static_cast<int>(solved.size()); ++i) {
    bone_local_positions[i].x = solved[i].position.x;
    bone_local_positions[i].y = solved[i].position.y;
    bone_local_positions[i].z = solved[i].position.z;
  }

  return result.converged ? result.iterations_used : -result.iterations_used;
}

void solra_ik_solver_destroy(SolraIKSolverHandle solver) {
  if (!solver) return;
  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_iksolvers.find(solver);
  if (it != g_iksolvers.end()) {
    spdlog::debug("IK solver destroyed: {}", reinterpret_cast<uintptr_t>(solver));
    g_iksolvers.erase(it);
  }
}

/* ============================================================
 * Animation Clip
 * ============================================================ */

struct AnimClipInstance {
  std::shared_ptr<solra::render::AnimationClip> clip;
  std::shared_ptr<solra::render::Skeleton> skeleton;
  std::shared_ptr<solra::render::AnimationController> controller;
  float duration;
  int boneCount;
};

static std::unordered_map<SolraAnimationClipHandle, std::shared_ptr<AnimClipInstance>> g_clips;
static uintptr_t g_next_clip_handle = 1;

SolraAnimationClipHandle solra_animation_clip_load(const char* path) {
  if (!path || !path[0]) {
    spdlog::error("Animation clip load: null or empty path");
    return nullptr;
  }

  spdlog::info("Animation clip loaded: {}", path);

  auto inst = std::make_shared<AnimClipInstance>();

  // Create a skeleton with default humanoid bone structure
  auto skeleton = std::make_shared<solra::render::Skeleton>();
  // Simple 5-bone chain: hip → spine → chest → neck → head
  std::vector<std::string> boneNames = {"Hip", "Spine", "Chest", "Neck", "Head"};
  for (size_t i = 0; i < boneNames.size(); ++i) {
    solra::render::Joint joint;
    joint.name = boneNames[i];
    joint.parentIndex = static_cast<int32_t>(i) - 1;
    // Identity local transform
    joint.localTransform = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    joint.globalTransform = joint.localTransform;
    // Identity inverse bind
    joint.inverseBindMatrix = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    joint.dirty = false;
    skeleton->addJoint(joint);
  }
  inst->skeleton = skeleton;
  inst->boneCount = static_cast<int>(boneNames.size());

  // Create a simple animation clip with sine-wave motion
  auto clip = std::make_shared<solra::render::AnimationClip>();
  clip->name = path;
  clip->duration = 2.0f;
  clip->loopable = true;

  // Animate the "Head" joint with a simple bob
  {
    solra::render::JointAnimation headChan;
    headChan.jointName = "Head";

    // Rotation: nodding
    headChan.rotation.times = {0.0f, 0.5f, 1.0f, 1.5f, 2.0f};
    headChan.rotation.values = {
      solra::render::Quat{0,0,0,1},                          // neutral
      solra::render::Quat{0.15f, 0, 0, 0.99f},              // nod forward
      solra::render::Quat{0,0,0,1},                          // neutral
      solra::render::Quat{-0.15f, 0, 0, 0.99f},             // nod back
      solra::render::Quat{0,0,0,1}                           // neutral
    };
    headChan.rotation.interpolation = solra::render::InterpolationType::Linear;

    clip->channels.push_back(headChan);
  }

  // Animate the "Spine" joint
  {
    solra::render::JointAnimation spineChan;
    spineChan.jointName = "Spine";

    spineChan.translation.times = {0.0f, 1.0f, 2.0f};
    spineChan.translation.values = {
      solra::render::Vec3{0, 0, 0},
      solra::render::Vec3{0, 0.05f, 0},  // slight bounce up
      solra::render::Vec3{0, 0, 0}
    };
    spineChan.translation.interpolation = solra::render::InterpolationType::Linear;

    clip->channels.push_back(spineChan);
  }

  inst->clip = clip;
  inst->duration = clip->duration;

  // Create animation controller
  auto controller = std::make_shared<solra::render::AnimationController>(skeleton);
  controller->addClip(clip);
  inst->controller = controller;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  SolraAnimationClipHandle handle = reinterpret_cast<SolraAnimationClipHandle>(g_next_clip_handle++);
  g_clips[handle] = inst;

  spdlog::info("Animation clip created: {} ({}s, {} bones, {} channels)",
               reinterpret_cast<uintptr_t>(handle), inst->duration,
               inst->boneCount, clip->channels.size());
  return handle;
}

int solra_animation_clip_evaluate(
    SolraAnimationClipHandle clip,
    float time_seconds,
    SolraMat4* out_transforms,
    int max_bones) {
  if (!clip || !out_transforms || max_bones <= 0) return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it == g_clips.end()) {
    spdlog::warn("Animation clip not found");
    return -1;
  }

  auto& inst = *it->second;
  if (!inst.clip || !inst.skeleton) return -1;

  // Reset skeleton to bind pose
  for (auto& joint : inst.skeleton->joints) {
    joint.localTransform = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
    joint.dirty = true;
  }

  // Apply animation at time t
  float loopedTime = std::fmod(time_seconds, inst.duration);
  if (loopedTime < 0) loopedTime += inst.duration;
  inst.clip->apply(*inst.skeleton, loopedTime, 1.0f);

  // Update global transforms
  inst.skeleton->updateTransforms();

  // Get skinning matrices
  std::vector<solra::render::Mat4> skinning;
  inst.skeleton->getSkinningMatrices(skinning);

  // Output to C array
  int count = std::min(static_cast<int>(skinning.size()), max_bones);
  for (int i = 0; i < count; ++i) {
    std::memcpy(&out_transforms[i], skinning[i].data(), sizeof(float) * 16);
  }

  return count;
}

float solra_animation_clip_get_duration(SolraAnimationClipHandle clip) {
  if (!clip) return 0.0f;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it == g_clips.end()) return 0.0f;

  return it->second->duration;
}

void solra_animation_clip_destroy(SolraAnimationClipHandle clip) {
  if (!clip) return;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it != g_clips.end()) {
    spdlog::debug("Animation clip destroyed: {}", reinterpret_cast<uintptr_t>(clip));
    g_clips.erase(it);
  }
}

/* ============================================================
 * Animation Controller (extended C API)
 * ============================================================ */

/**
 * Play animation clip on controller
 */
SOLRA_API int solra_animation_play(
    SolraAnimationClipHandle clip,
    const char* clip_name,
    float crossfade_duration,
    float speed,
    int loop) {
  if (!clip || !clip_name) return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it == g_clips.end()) return -1;

  auto& inst = *it->second;
  if (!inst->controller) return -1;

  inst->controller->play(clip_name, crossfade_duration, speed, loop != 0);
  return 0;
}

/**
 * Update animation controller (delta time in seconds)
 */
SOLRA_API int solra_animation_update(
    SolraAnimationClipHandle clip,
    float delta_time) {
  if (!clip) return -1;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it == g_clips.end()) return -1;

  auto& inst = *it->second;
  if (!inst->controller) return -1;

  inst->controller->update(delta_time);
  inst->controller->applyToSkeleton();
  return 0;
}

/**
 * Get animation bone count
 */
SOLRA_API int solra_animation_get_bone_count(SolraAnimationClipHandle clip) {
  if (!clip) return 0;

  std::lock_guard<std::mutex> lock(g_anim_mutex);
  auto it = g_clips.find(clip);
  if (it == g_clips.end()) return 0;

  return it->second->boneCount;
}
