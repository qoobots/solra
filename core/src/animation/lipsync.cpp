#include "lipsync.hpp"
#include <algorithm>
#include <cmath>

namespace solra::animation {

// ---- PhonemeVisemeMap ----
PhonemeVisemeMap& PhonemeVisemeMap::instance() {
    static PhonemeVisemeMap map;
    return map;
}

PhonemeVisemeMap::PhonemeVisemeMap() {
    // ARPABET → Viseme mapping (Microsoft SAPI / Oculus Lipsync standard)
    phonemeMap_ = {
        // Vowels
        {"AA", Viseme::AO}, {"AE", Viseme::AE}, {"AH", Viseme::AH},
        {"AO", Viseme::AO}, {"AW", Viseme::AW}, {"AY", Viseme::AY},
        {"EH", Viseme::EH}, {"ER", Viseme::ER}, {"EY", Viseme::EY},
        {"IH", Viseme::AE}, {"IY", Viseme::IY}, {"OW", Viseme::OW},
        {"OY", Viseme::OH}, {"UH", Viseme::UW}, {"UW", Viseme::UW},

        // Stops
        {"B",  Viseme::B_M_P}, {"P", Viseme::B_M_P}, {"D", Viseme::D_S_T},
        {"T",  Viseme::D_S_T}, {"G", Viseme::G_K_NG}, {"K", Viseme::G_K_NG},

        // Affricates
        {"CH", Viseme::CH_J_SH}, {"JH", Viseme::CH_J_SH},

        // Fricatives
        {"S", Viseme::D_S_T}, {"Z",  Viseme::D_S_T},
        {"SH", Viseme::CH_J_SH}, {"ZH", Viseme::CH_J_SH},
        {"F",  Viseme::F_V}, {"V", Viseme::F_V},
        {"TH", Viseme::TH}, {"DH", Viseme::TH},
        {"HH", Viseme::AE},  // /h/ - jaw slightly open

        // Nasals
        {"M", Viseme::B_M_P}, {"N", Viseme::N}, {"NG", Viseme::G_K_NG},

        // Liquids + Glides
        {"L", Viseme::L},  {"R",  Viseme::R},
        {"W", Viseme::W},  {"Y",  Viseme::IY},
    };

    // ---- Viseme → BlendShape weights (empirical tuning) ----
    // Silence
    visemeWeights_[static_cast<size_t>(Viseme::Silence)] = {};

    // AE /æ/ - wide open mouth, neutral lips
    auto& ae = visemeWeights_[static_cast<size_t>(Viseme::AE)];
    ae[BlendShapeKey::JawOpen] = 0.55f;
    ae[BlendShapeKey::MouthStretchLeft] = 0.2f;
    ae[BlendShapeKey::MouthStretchRight] = 0.2f;

    // AH /ʌ/ - moderate open
    auto& ahw = visemeWeights_[static_cast<size_t>(Viseme::AH)];
    ahw[BlendShapeKey::JawOpen] = 0.4f;
    ahw[BlendShapeKey::MouthLowerDownLeft] = 0.2f;
    ahw[BlendShapeKey::MouthLowerDownRight] = 0.2f;

    // AO /ɔ/ - rounded, medium open
    auto& ao = visemeWeights_[static_cast<size_t>(Viseme::AO)];
    ao[BlendShapeKey::JawOpen] = 0.35f;
    ao[BlendShapeKey::MouthFunnel] = 0.4f;
    ao[BlendShapeKey::MouthPucker] = 0.3f;

    // AW /aʊ/
    auto& aw = visemeWeights_[static_cast<size_t>(Viseme::AW)];
    aw[BlendShapeKey::JawOpen] = 0.5f;
    aw[BlendShapeKey::MouthFunnel] = 0.3f;

    // AY /aɪ/
    auto& ay = visemeWeights_[static_cast<size_t>(Viseme::AY)];
    ay[BlendShapeKey::JawOpen] = 0.5f;
    ay[BlendShapeKey::MouthStretchLeft] = 0.3f;
    ay[BlendShapeKey::MouthStretchRight] = 0.3f;

    // B_M_P (bilabial) - lips pressed together
    auto& bmp = visemeWeights_[static_cast<size_t>(Viseme::B_M_P)];
    bmp[BlendShapeKey::MouthPressLeft] = 1.0f;
    bmp[BlendShapeKey::MouthPressRight] = 1.0f;

    // CH_J_SH - lips slightly rounded
    auto& ch = visemeWeights_[static_cast<size_t>(Viseme::CH_J_SH)];
    ch[BlendShapeKey::MouthFunnel] = 0.35f;
    ch[BlendShapeKey::JawOpen] = 0.1f;

    // D_S_T - teeth together, tongue tip
    auto& dst = visemeWeights_[static_cast<size_t>(Viseme::D_S_T)];
    dst[BlendShapeKey::JawOpen] = 0.15f;
    dst[BlendShapeKey::MouthStretchLeft] = 0.15f;
    dst[BlendShapeKey::MouthStretchRight] = 0.15f;

    // EH /ɛ/
    auto& eh = visemeWeights_[static_cast<size_t>(Viseme::EH)];
    eh[BlendShapeKey::JawOpen] = 0.45f;
    eh[BlendShapeKey::MouthStretchLeft] = 0.15f;
    eh[BlendShapeKey::MouthStretchRight] = 0.15f;

    // ER /ɝ/
    auto& er = visemeWeights_[static_cast<size_t>(Viseme::ER)];
    er[BlendShapeKey::JawOpen] = 0.35f;
    er[BlendShapeKey::MouthFunnel] = 0.15f;

    // EY /eɪ/
    auto& ey = visemeWeights_[static_cast<size_t>(Viseme::EY)];
    ey[BlendShapeKey::JawOpen] = 0.45f;
    ey[BlendShapeKey::MouthStretchLeft] = 0.3f;
    ey[BlendShapeKey::MouthStretchRight] = 0.3f;

    // F_V (labiodental) - bottom lip under top teeth
    auto& fv = visemeWeights_[static_cast<size_t>(Viseme::F_V)];
    fv[BlendShapeKey::MouthLowerDownLeft] = 0.6f;
    fv[BlendShapeKey::MouthLowerDownRight] = 0.6f;
    fv[BlendShapeKey::MouthUpperUpLeft] = 0.1f;
    fv[BlendShapeKey::MouthUpperUpRight] = 0.1f;

    // G_K_NG
    auto& gk = visemeWeights_[static_cast<size_t>(Viseme::G_K_NG)];
    gk[BlendShapeKey::JawOpen] = 0.2f;
    gk[BlendShapeKey::MouthLowerDownLeft] = 0.2f;
    gk[BlendShapeKey::MouthLowerDownRight] = 0.2f;

    // IY /i/ - wide smile, teeth close
    auto& iy = visemeWeights_[static_cast<size_t>(Viseme::IY)];
    iy[BlendShapeKey::MouthStretchLeft] = 0.5f;
    iy[BlendShapeKey::MouthStretchRight] = 0.5f;
    iy[BlendShapeKey::JawOpen] = 0.1f;

    // L - tongue tip to alveolar ridge
    auto& l = visemeWeights_[static_cast<size_t>(Viseme::L)];
    l[BlendShapeKey::JawOpen] = 0.25f;
    l[BlendShapeKey::TongueOut] = 0.15f;

    // N
    auto& n = visemeWeights_[static_cast<size_t>(Viseme::N)];
    n[BlendShapeKey::JawOpen] = 0.1f;

    // OH /oʊ/
    auto& oh = visemeWeights_[static_cast<size_t>(Viseme::OH)];
    oh[BlendShapeKey::MouthFunnel] = 0.5f;
    oh[BlendShapeKey::MouthPucker] = 0.4f;
    oh[BlendShapeKey::JawOpen] = 0.15f;

    // OW /aʊ/
    auto& ow = visemeWeights_[static_cast<size_t>(Viseme::OW)];
    ow[BlendShapeKey::JawOpen] = 0.5f;
    ow[BlendShapeKey::MouthFunnel] = 0.35f;

    // R - lips rounded
    auto& r = visemeWeights_[static_cast<size_t>(Viseme::R)];
    r[BlendShapeKey::MouthFunnel] = 0.3f;
    r[BlendShapeKey::MouthPucker] = 0.2f;
    r[BlendShapeKey::JawOpen] = 0.15f;

    // TH
    auto& th = visemeWeights_[static_cast<size_t>(Viseme::TH)];
    th[BlendShapeKey::JawOpen] = 0.1f;
    th[BlendShapeKey::TongueOut] = 0.3f;

    // UW /u/ - tight rounded
    auto& uw = visemeWeights_[static_cast<size_t>(Viseme::UW)];
    uw[BlendShapeKey::MouthFunnel] = 0.6f;
    uw[BlendShapeKey::MouthPucker] = 0.5f;

    // W
    auto& w = visemeWeights_[static_cast<size_t>(Viseme::W)];
    w[BlendShapeKey::MouthFunnel] = 0.5f;
    w[BlendShapeKey::MouthPucker] = 0.4f;
}

Viseme PhonemeVisemeMap::phonemeToViseme(const std::string& arpabet) const {
    auto it = phonemeMap_.find(arpabet);
    return it != phonemeMap_.end() ? it->second : Viseme::Silence;
}

const BlendShapeWeights& PhonemeVisemeMap::visemeWeights(Viseme v) const {
    return visemeWeights_[static_cast<size_t>(v)];
}

// ---- LipSyncEngine ----
void LipSyncEngine::setPhonemeTimeline(const std::vector<PhonemeEvent>& timeline) {
    timeline_ = timeline;
    currentIdx_ = 0;
    elapsed_ = 0.0f;
}

void LipSyncEngine::update(float deltaTime) {
    if (timeline_.empty()) {
        applyViseme(Viseme::Silence, 0.0f);
        return;
    }

    elapsed_ += deltaTime;

    // Find current phoneme event
    while (currentIdx_ < timeline_.size() && elapsed_ >= timeline_[currentIdx_].endTime) {
        currentIdx_++;
    }

    if (currentIdx_ >= timeline_.size()) {
        applyViseme(Viseme::Silence, 0.0f);
        return;
    }

    const auto& event = timeline_[currentIdx_];
    if (elapsed_ >= event.startTime && elapsed_ < event.endTime) {
        applyViseme(event.viseme, event.confidence);
    } else if (elapsed_ < event.startTime) {
        // Between events: interpolate toward next
        applyViseme(Viseme::Silence, 0.0f);
    }
}

void LipSyncEngine::applyViseme(Viseme v, float confidence) {
    currentViseme_ = v;
    currentConfidence_ = confidence;

    const auto& vmWeights = PhonemeVisemeMap::instance().visemeWeights(v);
    targetWeights_ = vmWeights;

    // Scale by intensity and confidence
    for (size_t i = 0; i < kBlendShapeCount; ++i)
        targetWeights_[i] *= intensity_ * confidence;

    // Exponential smoothing
    float alpha = 1.0f - std::exp(-deltaTime_ / smoothHalfLife_);
    currentWeights_.lerp(targetWeights_, alpha);
}

} // namespace solra::animation
