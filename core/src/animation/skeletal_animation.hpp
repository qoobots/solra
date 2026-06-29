#pragma once
// Skeletal Animation System: skinning + animation clip playback + blending
#include <cstdint>
#include <string>
#include <vector>
#include <array>
#include <unordered_map>
#include <memory>

namespace solra::render {

// ---- Skeleton ----
using Mat4 = std::array<float, 16>;
struct Vec3 { float x=0,y=0,z=0; };
struct Quat { float x=0,y=0,z=0,w=1; };

struct Joint {
    std::string name;
    int32_t parentIndex = -1;       // -1 = root
    Mat4 inverseBindMatrix;         // model-space → joint-local
    Mat4 localTransform;            // current local transform
    Mat4 globalTransform;           // cached world-space (model-relative)
    bool dirty = true;
};

class Skeleton {
public:
    std::vector<Joint> joints;

    Joint* findJoint(const std::string& name);
    int32_t jointIndex(const std::string& name) const;
    void addJoint(const Joint& j) { joints.push_back(j); }

    // Compute global transforms for all joints (top-down)
    void updateTransforms();

    // Output: flat array of skinning matrices (jointCount × 4×4)
    // skinningMatrix[i] = globalTransform[i] * inverseBindMatrix[i]
    void getSkinningMatrices(std::vector<Mat4>& out) const;

    size_t jointCount() const { return joints.size(); }
};

// ---- Animation Clip ----
enum class InterpolationType { Linear, Step, CubicSpline };

template<typename T>
struct AnimationSampler {
    std::vector<float> times; // keyframe timestamps in seconds
    std::vector<T> values;
    InterpolationType interpolation = InterpolationType::Linear;

    T sample(float t, bool loop = true) const;
};

struct JointAnimation {
    std::string jointName;
    AnimationSampler<Vec3> translation;
    AnimationSampler<Quat> rotation;
    AnimationSampler<Vec3> scale;
};

class AnimationClip {
public:
    std::string name;
    float duration = 0.0f; // seconds
    bool loopable = true;
    std::vector<JointAnimation> channels;

    // Sample all animated joints at time t, write results to skeleton
    void apply(Skeleton& skeleton, float t, float weight = 1.0f) const;

    JointAnimation* findChannel(const std::string& jointName);
};

// ---- Animation State Machine ----
enum class AnimPlayState { Stopped, Playing, Paused };

class AnimationState {
public:
    std::string clipName;
    AnimPlayState state = AnimPlayState::Stopped;
    float time = 0.0f;
    float speed = 1.0f;
    float weight = 1.0f;
    float transitionTimer = 0.0f; // 0→1 during crossfade
};

// ---- Animation Controller ----
class AnimationController {
public:
    AnimationController() = default;
    explicit AnimationController(std::shared_ptr<Skeleton> skel) : skeleton(skel) {}

    void setSkeleton(std::shared_ptr<Skeleton> s) { skeleton = s; }

    // Clip management
    void addClip(std::shared_ptr<AnimationClip> clip);
    std::shared_ptr<AnimationClip> getClip(const std::string& name) const;

    // Playback
    void play(const std::string& clipName, float crossfadeDuration = 0.25f,
              float speed = 1.0f, bool loop = true);
    void pause();
    void resume();
    void stop();
    void setSpeed(float speed);

    // Per-frame update
    void update(float deltaTime);

    // Apply current animation to skeleton
    void applyToSkeleton();

    // Skeleton access for skinning output
    void getSkinningMatrices(std::vector<Mat4>& out) const;

private:
    std::shared_ptr<Skeleton> skeleton;
    std::unordered_map<std::string, std::shared_ptr<AnimationClip>> clips_;
    AnimationState current_;
    AnimationState previous_; // for crossfade
    float crossfadeDuration_ = 0.0f;
    float crossfadeTimer_ = 0.0f;
    bool crossfading_ = false;
};

} // namespace solra::render
