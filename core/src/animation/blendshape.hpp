#pragma once
// Facial BlendShape System: ≥15 facial expressions with weight accumulation
#include <cstdint>
#include <string>
#include <vector>
#include <array>
#include <unordered_map>

namespace solra::animation {

// ---- Standard ARKit-compatible blendshape set (52 shapes) ----
// We implement the core 20 for virtual human expressiveness
enum class BlendShapeKey : uint8_t {
    // Brow
    BrowDownLeft, BrowDownRight, BrowInnerUp, BrowOuterUpLeft, BrowOuterUpRight,

    // Eye
    EyeBlinkLeft, EyeBlinkRight, EyeSquintLeft, EyeSquintRight,
    EyeWideLeft, EyeWideRight, EyeLookUpLeft, EyeLookUpRight,
    EyeLookDownLeft, EyeLookDownRight, EyeLookInLeft, EyeLookInRight,
    EyeLookOutLeft, EyeLookOutRight,

    // Nose
    NoseSneerLeft, NoseSneerRight,

    // Cheek
    CheekPuff, CheekSquintLeft, CheekSquintRight,

    // Mouth
    MouthSmileLeft, MouthSmileRight, MouthFrownLeft, MouthFrownRight,
    MouthDimpleLeft, MouthDimpleRight,
    MouthUpperUpLeft, MouthUpperUpRight, MouthLowerDownLeft, MouthLowerDownRight,
    MouthPressLeft, MouthPressRight, MouthStretchLeft, MouthStretchRight,
    MouthClose, MouthRollUpper, MouthRollLower,
    MouthShrugUpper, MouthShrugLower,
    MouthLeft, MouthRight, MouthFunnel, MouthPucker,

    // Jaw
    JawOpen, JawLeft, JawRight, JawForward,

    // Tongue
    TongueOut,

    Count
};

constexpr size_t kBlendShapeCount = static_cast<size_t>(BlendShapeKey::Count);

// ---- BlendShape Target ----
struct BlendShapeTarget {
    BlendShapeKey key;
    std::string name;               // human-readable
    float weight = 0.0f;            // 0.0 → 1.0
    bool isBilateral = false;       // paired L/R shapes
    BlendShapeKey counterpart;      // L→R or R→L pairing
};

// ---- BlendShape Weight Set ----
class BlendShapeWeights {
public:
    // Access
    float& operator[](BlendShapeKey key);
    float operator[](BlendShapeKey key) const;
    float& operator[](size_t idx);
    float operator[](size_t idx) const;

    // Operations
    void reset();                              // all to zero
    void lerp(const BlendShapeWeights& other, float t); // interpolate toward other
    void blendAdd(const BlendShapeWeights& other, float alpha = 1.0f);
    void blendMax(const BlendShapeWeights& other);      // per-channel max
    void clamp(float minVal = 0.0f, float maxVal = 1.0f);

    // Query
    bool isEmpty() const;
    float maxWeight() const;
    std::vector<BlendShapeKey> activeShapes(float threshold = 0.001f) const;

    // GPU upload (flat float array, 4 floats per shape for alignment)
    void gpuUpload(std::vector<float>& out, size_t maxShapes = kBlendShapeCount) const;

private:
    std::array<float, kBlendShapeCount> weights_{};
};

// ---- Expression Presets ----
struct ExpressionPreset {
    std::string name;
    BlendShapeWeights weights;
};

class ExpressionLibrary {
public:
    void addPreset(const ExpressionPreset& preset);
    const BlendShapeWeights* find(const std::string& name) const;

    // Standard presets
    static ExpressionPreset neutral();
    static ExpressionPreset happy();
    static ExpressionPreset sad();
    static ExpressionPreset angry();
    static ExpressionPreset surprised();
    static ExpressionPreset fear();
    static ExpressionPreset disgust();
    static ExpressionPreset thinking();       // brow raised one side
    static ExpressionPreset sleepy();         // eyes half-closed
    static ExpressionPreset excited();        // wide eyes + smile
    static ExpressionPreset flirty();         // smirk + raised brow
    static ExpressionPreset confused();
    static ExpressionPreset pain();           // squint + grimace
    static ExpressionPreset laugh();          // jaw open + smile
    static ExpressionPreset concern();        // inner brow up + slight frown

private:
    std::unordered_map<std::string, BlendShapeWeights> presets_;
};

} // namespace solra::animation
