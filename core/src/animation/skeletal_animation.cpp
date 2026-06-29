#include "skeletal_animation.hpp"
#include <algorithm>
#include <cmath>
#include <stdexcept>

namespace solra::render {

// ---- Quaternion helpers ----
static Quat quatSlerp(const Quat& a, const Quat& b, float t) {
    float cosTheta = a.x*b.x + a.y*b.y + a.z*b.z + a.w*b.w;
    Quat qb = b;
    if (cosTheta < 0) { cosTheta = -cosTheta; qb = {-b.x, -b.y, -b.z, -b.w}; }
    if (cosTheta > 0.9995f) {
        // Linear interpolation for small angles
        float s = 1.0f - t;
        return {s*a.x + t*qb.x, s*a.y + t*qb.y, s*a.z + t*qb.z, s*a.w + t*qb.w};
    }
    float theta = std::acos(cosTheta);
    float sinTheta = std::sin(theta);
    float s0 = std::sin((1.0f-t)*theta) / sinTheta;
    float s1 = std::sin(t*theta) / sinTheta;
    return {s0*a.x + s1*qb.x, s0*a.y + s1*qb.y, s0*a.z + s1*qb.z, s0*a.w + s1*qb.w};
}

static Vec3 vec3Lerp(const Vec3& a, const Vec3& b, float t) {
    return {a.x + (b.x-a.x)*t, a.y + (b.y-a.y)*t, a.z + (b.z-a.z)*t};
}

// ---- Skeleton ----
Joint* Skeleton::findJoint(const std::string& name) {
    for (auto& j : joints)
        if (j.name == name) return &j;
    return nullptr;
}

int32_t Skeleton::jointIndex(const std::string& name) const {
    for (size_t i = 0; i < joints.size(); ++i)
        if (joints[i].name == name) return static_cast<int32_t>(i);
    return -1;
}

void Skeleton::updateTransforms() {
    for (size_t i = 0; i < joints.size(); ++i) {
        if (joints[i].parentIndex < 0) {
            joints[i].globalTransform = joints[i].localTransform;
        } else {
            // global = parent.global * local
            const auto& pg = joints[joints[i].parentIndex].globalTransform;
            const auto& l = joints[i].localTransform;
            Mat4 r{};
            for (int col = 0; col < 4; ++col)
                for (int row = 0; row < 4; ++row)
                    for (int k = 0; k < 4; ++k)
                        r[col*4+row] += pg[k*4+row] * l[col*4+k];
            joints[i].globalTransform = r;
        }
        joints[i].dirty = false;
    }
}

void Skeleton::getSkinningMatrices(std::vector<Mat4>& out) const {
    out.resize(joints.size());
    for (size_t i = 0; i < joints.size(); ++i) {
        // skinning = globalTransform * inverseBindMatrix
        const auto& gt = joints[i].globalTransform;
        const auto& ibm = joints[i].inverseBindMatrix;
        Mat4& r = out[i];
        for (int col = 0; col < 4; ++col)
            for (int row = 0; row < 4; ++row) {
                r[col*4+row] = 0;
                for (int k = 0; k < 4; ++k)
                    r[col*4+row] += gt[k*4+row] * ibm[col*4+k];
            }
    }
}

// ---- JointAnimation apply ----
void AnimationClip::apply(Skeleton& skeleton, float t, float weight) const {
    for (auto& chan : channels) {
        auto* joint = skeleton.findJoint(chan.jointName);
        if (!joint) continue;

        if (!chan.translation.times.empty()) {
            Vec3 tVal = chan.translation.sample(t);
            joint->localTransform = {
                1,0,0,0, 0,1,0,0, 0,0,1,0, tVal.x, tVal.y, tVal.z, 1
            };
        }
        if (!chan.rotation.times.empty()) {
            Quat rVal = chan.rotation.sample(t);
            // Simplified: set rotation part of mat (full impl: quat→mat4)
            joint->localTransform = {
                1-2*(rVal.y*rVal.y+rVal.z*rVal.z), 2*(rVal.x*rVal.y+rVal.w*rVal.z),
                2*(rVal.x*rVal.z-rVal.w*rVal.y), 0,
                2*(rVal.x*rVal.y-rVal.w*rVal.z), 1-2*(rVal.x*rVal.x+rVal.z*rVal.z),
                2*(rVal.y*rVal.z+rVal.w*rVal.x), 0,
                2*(rVal.x*rVal.z+rVal.w*rVal.y), 2*(rVal.y*rVal.z-rVal.w*rVal.x),
                1-2*(rVal.x*rVal.x+rVal.y*rVal.y), 0,
                0,0,0,1
            };
        }
        joint->dirty = true;
    }
}

JointAnimation* AnimationClip::findChannel(const std::string& jointName) {
    for (auto& c : channels)
        if (c.jointName == jointName) return &c;
    return nullptr;
}

// ---- AnimationController ----
void AnimationController::addClip(std::shared_ptr<AnimationClip> clip) {
    clips_[clip->name] = clip;
}

std::shared_ptr<AnimationClip> AnimationController::getClip(const std::string& name) const {
    auto it = clips_.find(name);
    return it != clips_.end() ? it->second : nullptr;
}

void AnimationController::play(const std::string& clipName, float crossfadeDuration,
                               float speed, bool loop) {
    auto clip = getClip(clipName);
    if (!clip) return;

    if (current_.state == AnimPlayState::Playing && crossfadeDuration > 0) {
        previous_ = current_;
        crossfadeDuration_ = crossfadeDuration;
        crossfadeTimer_ = 0;
        crossfading_ = true;
    }

    current_.clipName = clipName;
    current_.state = AnimPlayState::Playing;
    current_.time = 0;
    current_.speed = speed;
    current_.weight = 1.0f;
}

void AnimationController::pause()  { current_.state = AnimPlayState::Paused; }
void AnimationController::resume() { current_.state = AnimPlayState::Playing; }
void AnimationController::stop()   {
    current_.state = AnimPlayState::Stopped;
    crossfading_ = false;
}

void AnimationController::setSpeed(float speed) { current_.speed = speed; }

void AnimationController::update(float deltaTime) {
    if (current_.state != AnimPlayState::Playing) return;

    auto clip = getClip(current_.clipName);
    if (!clip) return;

    current_.time += deltaTime * current_.speed;
    if (current_.time >= clip->duration) {
        if (clip->loopable) {
            current_.time = std::fmod(current_.time, clip->duration);
        } else {
            current_.time = clip->duration;
            current_.state = AnimPlayState::Paused;
        }
    }

    // Crossfade logic
    if (crossfading_) {
        crossfadeTimer_ += deltaTime;
        if (crossfadeTimer_ >= crossfadeDuration_) {
            crossfading_ = false;
            current_.weight = 1.0f;
        } else {
            float t = crossfadeTimer_ / crossfadeDuration_;
            previous_.weight = 1.0f - t;
            current_.weight = t;
        }
    }
}

void AnimationController::applyToSkeleton() {
    if (!skeleton) return;

    auto curClip = getClip(current_.clipName);
    if (curClip)
        curClip->apply(*skeleton, current_.time, current_.weight);

    if (crossfading_) {
        auto prevClip = getClip(previous_.clipName);
        if (prevClip)
            prevClip->apply(*skeleton, previous_.time, previous_.weight);
    }
}

void AnimationController::getSkinningMatrices(std::vector<Mat4>& out) const {
    if (skeleton) skeleton->getSkinningMatrices(out);
}

} // namespace solra::render
