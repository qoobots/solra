#pragma once
// Lip-Sync: phoneme → viseme mapping + audio-driven facial animation
#include <cstdint>
#include <string>
#include <vector>
#include <unordered_map>
#include <array>
#include "blendshape.hpp"

namespace solra::animation {

// ---- Viseme: visual representation of a phoneme ----
enum class Viseme : uint8_t {
    // English phoneme-viseme mapping (22 visemes, Microsoft / Oculus standard)
    Silence,    // _
    AE,         // æ (cat)
    AH,         // ʌ (cut)
    AO,         // ɔ (caught)
    AW,         // aʊ (cow)
    AY,         // aɪ (bite)
    B_M_P,      // b, m, p (bilabial)
    CH_J_SH,    // tʃ, dʒ, ʃ (chin, gin, shin)
    D_S_T,      // d, s, t
    E,          // e (bed)   [or æ]
    EH,         // ɛ (bet)
    ER,         // ɝ (bird)
    EY,         // eɪ (bait)
    F_V,        // f, v (labiodental)
    G_K_NG,     // g, k, ŋ (sing)
    IY,         // i (beat)
    L,          // l
    N,          // n
    OH,         // oʊ (boat)
    OW,         // aʊ (bout)
    R,          // r
    TH,         // θ, ð (thin, this)
    UW,         // u (boot)
    W,          // w
    Count
};

constexpr size_t kVisemeCount = static_cast<size_t>(Viseme::Count);

// ---- Viseme → BlendShape mapping ----
// Each viseme maps to a weighted combination of blendshapes
struct VisemeTarget {
    Viseme viseme;
    BlendShapeWeights weights;
    float dominanceDuration = 0.15f; // seconds viseme stays dominant
};

class PhonemeVisemeMap {
public:
    static PhonemeVisemeMap& instance();

    // ARPABET phoneme → Viseme
    Viseme phonemeToViseme(const std::string& arpabet) const;

    // Viseme → blend shape weights
    const BlendShapeWeights& visemeWeights(Viseme v) const;

private:
    PhonemeVisemeMap();
    std::unordered_map<std::string, Viseme> phonemeMap_;
    std::array<BlendShapeWeights, kVisemeCount> visemeWeights_;
};

// ---- Lip-sync engine ----
struct PhonemeEvent {
    Viseme viseme;
    float startTime;  // seconds
    float endTime;
    float confidence;  // 0-1
};

class LipSyncEngine {
public:
    // Feed phoneme timeline (from TTS or speech recognition)
    void setPhonemeTimeline(const std::vector<PhonemeEvent>& timeline);

    // Update at 60Hz
    void update(float deltaTime);

    // Get current blendshape weights for rendering
    const BlendShapeWeights& currentWeights() const { return currentWeights_; }

    // Parameters
    void setSmoothing(float smoothMs) { smoothHalfLife_ = smoothMs / 1000.0f; }
    void setIntensity(float intensity) { intensity_ = std::clamp(intensity, 0.0f, 1.5f); }

    // Debug
    Viseme currentViseme() const { return currentViseme_; }
    float currentConfidence() const { return currentConfidence_; }

private:
    std::vector<PhonemeEvent> timeline_;
    size_t currentIdx_ = 0;
    float elapsed_ = 0.0f;
    Viseme currentViseme_ = Viseme::Silence;
    float currentConfidence_ = 0.0f;
    BlendShapeWeights currentWeights_;
    BlendShapeWeights targetWeights_;
    float smoothHalfLife_ = 0.05f; // 50ms default
    float intensity_ = 1.0f;

    void applyViseme(Viseme v, float confidence);
};

} // namespace solra::animation
