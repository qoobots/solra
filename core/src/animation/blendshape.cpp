#include "blendshape.hpp"
#include <algorithm>
#include <cmath>

namespace solra::animation {

// ---- BlendShapeWeights ----
float& BlendShapeWeights::operator[](BlendShapeKey key) {
    return weights_[static_cast<size_t>(key)];
}
float BlendShapeWeights::operator[](BlendShapeKey key) const {
    return weights_[static_cast<size_t>(key)];
}
float& BlendShapeWeights::operator[](size_t idx) {
    return weights_[idx];
}
float BlendShapeWeights::operator[](size_t idx) const {
    return weights_[idx];
}

void BlendShapeWeights::reset() {
    weights_.fill(0.0f);
}

void BlendShapeWeights::lerp(const BlendShapeWeights& other, float t) {
    t = std::clamp(t, 0.0f, 1.0f);
    for (size_t i = 0; i < kBlendShapeCount; ++i)
        weights_[i] = weights_[i] + (other.weights_[i] - weights_[i]) * t;
}

void BlendShapeWeights::blendAdd(const BlendShapeWeights& other, float alpha) {
    for (size_t i = 0; i < kBlendShapeCount; ++i)
        weights_[i] = std::min(weights_[i] + other.weights_[i] * alpha, 1.0f);
}

void BlendShapeWeights::blendMax(const BlendShapeWeights& other) {
    for (size_t i = 0; i < kBlendShapeCount; ++i)
        weights_[i] = std::max(weights_[i], other.weights_[i]);
}

void BlendShapeWeights::clamp(float minVal, float maxVal) {
    for (auto& w : weights_) w = std::clamp(w, minVal, maxVal);
}

bool BlendShapeWeights::isEmpty() const {
    return std::all_of(weights_.begin(), weights_.end(), [](float w) { return w == 0.0f; });
}

float BlendShapeWeights::maxWeight() const {
    return *std::max_element(weights_.begin(), weights_.end());
}

std::vector<BlendShapeKey> BlendShapeWeights::activeShapes(float threshold) const {
    std::vector<BlendShapeKey> active;
    for (size_t i = 0; i < kBlendShapeCount; ++i)
        if (weights_[i] > threshold)
            active.push_back(static_cast<BlendShapeKey>(i));
    return active;
}

void BlendShapeWeights::gpuUpload(std::vector<float>& out, size_t maxShapes) const {
    size_t n = std::min(maxShapes, kBlendShapeCount);
    out.resize(n);
    std::copy_n(weights_.begin(), n, out.begin());
}

// ---- ExpressionLibrary ----
void ExpressionLibrary::addPreset(const ExpressionPreset& preset) {
    presets_[preset.name] = preset.weights;
}

const BlendShapeWeights* ExpressionLibrary::find(const std::string& name) const {
    auto it = presets_.find(name);
    return it != presets_.end() ? &it->second : nullptr;
}

// ---- Standard presets (ARKit-style weight definitions) ----
#define MAKE_PRESET(name) ExpressionPreset name() { ExpressionPreset p{#name}; p.weights.reset(); return p; }

static void set(BlendShapeWeights& w, BlendShapeKey k, float v) { w[k] = v; }

ExpressionPreset ExpressionLibrary::neutral() {
    ExpressionPreset p{"neutral"};
    p.weights.reset(); // all zero = neutral
    return p;
}

ExpressionPreset ExpressionLibrary::happy() {
    ExpressionPreset p{"happy"};
    set(p.weights, BlendShapeKey::MouthSmileLeft, 0.7f);
    set(p.weights, BlendShapeKey::MouthSmileRight, 0.7f);
    set(p.weights, BlendShapeKey::CheekSquintLeft, 0.3f);
    set(p.weights, BlendShapeKey::CheekSquintRight, 0.3f);
    set(p.weights, BlendShapeKey::EyeSquintLeft, 0.15f);
    set(p.weights, BlendShapeKey::EyeSquintRight, 0.15f);
    return p;
}

ExpressionPreset ExpressionLibrary::sad() {
    ExpressionPreset p{"sad"};
    set(p.weights, BlendShapeKey::MouthFrownLeft, 0.6f);
    set(p.weights, BlendShapeKey::MouthFrownRight, 0.6f);
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.7f);
    set(p.weights, BlendShapeKey::EyeBlinkLeft, 0.2f);
    set(p.weights, BlendShapeKey::EyeBlinkRight, 0.2f);
    return p;
}

ExpressionPreset ExpressionLibrary::angry() {
    ExpressionPreset p{"angry"};
    set(p.weights, BlendShapeKey::BrowDownLeft, 0.8f);
    set(p.weights, BlendShapeKey::BrowDownRight, 0.8f);
    set(p.weights, BlendShapeKey::MouthFrownLeft, 0.5f);
    set(p.weights, BlendShapeKey::MouthFrownRight, 0.5f);
    set(p.weights, BlendShapeKey::NoseSneerLeft, 0.4f);
    set(p.weights, BlendShapeKey::NoseSneerRight, 0.4f);
    set(p.weights, BlendShapeKey::MouthPressLeft, 0.3f);
    set(p.weights, BlendShapeKey::MouthPressRight, 0.3f);
    return p;
}

ExpressionPreset ExpressionLibrary::surprised() {
    ExpressionPreset p{"surprised"};
    set(p.weights, BlendShapeKey::BrowOuterUpLeft, 0.9f);
    set(p.weights, BlendShapeKey::BrowOuterUpRight, 0.9f);
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.6f);
    set(p.weights, BlendShapeKey::EyeWideLeft, 0.8f);
    set(p.weights, BlendShapeKey::EyeWideRight, 0.8f);
    set(p.weights, BlendShapeKey::JawOpen, 0.45f);
    return p;
}

ExpressionPreset ExpressionLibrary::fear() {
    ExpressionPreset p{"fear"};
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.7f);
    set(p.weights, BlendShapeKey::EyeWideLeft, 0.9f);
    set(p.weights, BlendShapeKey::EyeWideRight, 0.9f);
    set(p.weights, BlendShapeKey::MouthStretchLeft, 0.3f);
    set(p.weights, BlendShapeKey::MouthStretchRight, 0.3f);
    set(p.weights, BlendShapeKey::MouthFrownLeft, 0.15f);
    set(p.weights, BlendShapeKey::MouthFrownRight, 0.15f);
    return p;
}

ExpressionPreset ExpressionLibrary::disgust() {
    ExpressionPreset p{"disgust"};
    set(p.weights, BlendShapeKey::NoseSneerLeft, 0.8f);
    set(p.weights, BlendShapeKey::NoseSneerRight, 0.8f);
    set(p.weights, BlendShapeKey::BrowDownLeft, 0.3f);
    set(p.weights, BlendShapeKey::BrowDownRight, 0.3f);
    set(p.weights, BlendShapeKey::MouthUpperUpLeft, 0.5f);
    set(p.weights, BlendShapeKey::MouthUpperUpRight, 0.5f);
    return p;
}

ExpressionPreset ExpressionLibrary::thinking() {
    ExpressionPreset p{"thinking"};
    set(p.weights, BlendShapeKey::BrowOuterUpLeft, 0.6f);
    set(p.weights, BlendShapeKey::BrowDownRight, 0.3f);
    set(p.weights, BlendShapeKey::MouthShrugUpper, 0.4f);
    set(p.weights, BlendShapeKey::EyeLookUpRight, 0.2f);
    return p;
}

ExpressionPreset ExpressionLibrary::sleepy() {
    ExpressionPreset p{"sleepy"};
    set(p.weights, BlendShapeKey::EyeBlinkLeft, 0.6f);
    set(p.weights, BlendShapeKey::EyeBlinkRight, 0.6f);
    set(p.weights, BlendShapeKey::JawOpen, 0.15f);
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.15f);
    return p;
}

ExpressionPreset ExpressionLibrary::excited() {
    ExpressionPreset p{"excited"};
    set(p.weights, BlendShapeKey::MouthSmileLeft, 0.8f);
    set(p.weights, BlendShapeKey::MouthSmileRight, 0.8f);
    set(p.weights, BlendShapeKey::EyeWideLeft, 0.6f);
    set(p.weights, BlendShapeKey::EyeWideRight, 0.6f);
    set(p.weights, BlendShapeKey::BrowOuterUpLeft, 0.5f);
    set(p.weights, BlendShapeKey::BrowOuterUpRight, 0.5f);
    return p;
}

ExpressionPreset ExpressionLibrary::flirty() {
    ExpressionPreset p{"flirty"};
    set(p.weights, BlendShapeKey::MouthSmileLeft, 0.5f);
    set(p.weights, BlendShapeKey::MouthSmileRight, 0.5f);
    set(p.weights, BlendShapeKey::BrowOuterUpLeft, 0.3f);
    set(p.weights, BlendShapeKey::EyeSquintLeft, 0.15f);
    set(p.weights, BlendShapeKey::EyeSquintRight, 0.15f);
    set(p.weights, BlendShapeKey::MouthDimpleLeft, 0.2f);
    return p;
}

ExpressionPreset ExpressionLibrary::confused() {
    ExpressionPreset p{"confused"};
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.5f);
    set(p.weights, BlendShapeKey::BrowDownLeft, 0.3f);
    set(p.weights, BlendShapeKey::MouthFrownLeft, 0.25f);
    set(p.weights, BlendShapeKey::MouthFrownRight, 0.25f);
    set(p.weights, BlendShapeKey::MouthShrugUpper, 0.3f);
    return p;
}

ExpressionPreset ExpressionLibrary::pain() {
    ExpressionPreset p{"pain"};
    set(p.weights, BlendShapeKey::BrowDownLeft, 0.6f);
    set(p.weights, BlendShapeKey::BrowDownRight, 0.6f);
    set(p.weights, BlendShapeKey::EyeSquintLeft, 0.7f);
    set(p.weights, BlendShapeKey::EyeSquintRight, 0.7f);
    set(p.weights, BlendShapeKey::MouthStretchLeft, 0.45f);
    set(p.weights, BlendShapeKey::MouthStretchRight, 0.45f);
    set(p.weights, BlendShapeKey::NoseSneerLeft, 0.2f);
    return p;
}

ExpressionPreset ExpressionLibrary::laugh() {
    ExpressionPreset p{"laugh"};
    set(p.weights, BlendShapeKey::MouthSmileLeft, 0.9f);
    set(p.weights, BlendShapeKey::MouthSmileRight, 0.9f);
    set(p.weights, BlendShapeKey::JawOpen, 0.6f);
    set(p.weights, BlendShapeKey::CheekSquintLeft, 0.5f);
    set(p.weights, BlendShapeKey::CheekSquintRight, 0.5f);
    set(p.weights, BlendShapeKey::EyeSquintLeft, 0.3f);
    set(p.weights, BlendShapeKey::EyeSquintRight, 0.3f);
    return p;
}

ExpressionPreset ExpressionLibrary::concern() {
    ExpressionPreset p{"concern"};
    set(p.weights, BlendShapeKey::BrowInnerUp, 0.8f);
    set(p.weights, BlendShapeKey::MouthFrownLeft, 0.3f);
    set(p.weights, BlendShapeKey::MouthFrownRight, 0.3f);
    set(p.weights, BlendShapeKey::MouthPressLeft, 0.1f);
    return p;
}

} // namespace solra::animation
