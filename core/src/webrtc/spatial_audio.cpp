#include "spatial_audio.hpp"

#include <algorithm>
#include <cmath>
#include <map>
#include <mutex>
#include <vector>

namespace solra::webrtc {

// ── 内部实现 ────────────────────────────────────────
struct WebRTCSpatialAudioMixer::Impl {
    int sample_rate = 48000;
    int frames_per_buffer = 480;
    float master_gain = 1.0f;

    Listener listener;
    RoomAcoustics room;
    DistanceAttenuation distance_atten;
    bool doppler_enabled = false;

    struct SourceState {
        AudioSource source;
        std::vector<int16_t> buffer; // 输入缓冲
    };

    std::mutex mtx;
    std::map<std::string, SourceState> sources;

    // HRTF 数据集（可选，加载后启用）
    std::vector<HRTFEntry> hrtf_data;
    bool hrtf_enabled = false;

    // CPU 负载估算
    float cpu_load = 0.f;
};

WebRTCSpatialAudioMixer::WebRTCSpatialAudioMixer()
    : impl_(std::make_unique<Impl>()) {}

WebRTCSpatialAudioMixer::~WebRTCSpatialAudioMixer() = default;

bool WebRTCSpatialAudioMixer::init(int sample_rate, int frames_per_buffer) {
    impl_->sample_rate = sample_rate;
    impl_->frames_per_buffer = frames_per_buffer;
    return true;
}

bool WebRTCSpatialAudioMixer::add_source(const AudioSource& source) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    Impl::SourceState state;
    state.source = source;
    state.buffer.reserve(impl_->frames_per_buffer * 2);
    impl_->sources[source.source_id] = std::move(state);
    return true;
}

bool WebRTCSpatialAudioMixer::remove_source(const std::string& source_id) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    return impl_->sources.erase(source_id) > 0;
}

bool WebRTCSpatialAudioMixer::update_source(const AudioSource& source) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    auto it = impl_->sources.find(source.source_id);
    if (it == impl_->sources.end()) return false;
    it->second.source = source;
    return true;
}

bool WebRTCSpatialAudioMixer::submit_frame(const std::string& source_id,
                                            const int16_t* data,
                                            size_t sample_count) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    auto it = impl_->sources.find(source_id);
    if (it == impl_->sources.end()) return false;

    it->second.buffer.assign(data, data + sample_count);
    return true;
}

// ── 核心渲染 ────────────────────────────────────────
size_t WebRTCSpatialAudioMixer::render(int16_t* output, size_t max_samples) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    std::fill(output, output + max_samples * 2, 0); // stereo

    const Listener& lis = impl_->listener;
    const DistanceAttenuation& atten = impl_->distance_atten;

    for (auto& [id, state] : impl_->sources) {
        if (state.buffer.empty()) continue;

        const AudioSource& src = state.source;
        if (src.is_self) continue; // 跳过自己的声音

        // ── 计算空间参数 ──
        Vec3 rel = src.position - lis.position;
        float distance = rel.length();

        // 距离衰减
        float distance_gain = 1.0f;
        switch (atten.model) {
        case DistanceModel::Linear:
            distance_gain = std::max(0.f,
                1.0f - distance / atten.max_distance);
            break;
        case DistanceModel::Inverse:
            distance_gain = atten.ref_distance /
                (atten.ref_distance + atten.rolloff *
                     std::max(0.f, distance - atten.ref_distance));
            break;
        case DistanceModel::Exponential:
            if (distance > 0) {
                distance_gain = std::pow(
                    distance / atten.ref_distance, -atten.rolloff);
            }
            break;
        }

        // 遮挡衰减
        distance_gain *= (1.0f - src.occlusion);

        // 方位角/仰角计算
        Vec3 dir = rel.normalized();
        float azimuth = std::atan2(dir.x, dir.z);  // 水平角
        float elevation = std::asin(dir.y);         // 垂直角

        // HRTF 近似 (简化公式，完整 HRTF 需要卷积)
        // Inter-aural Time Difference (ITD) + 频率相关衰减
        const float head_radius = 0.0875f; // m
        const float speed_of_sound = 343.f; // m/s
        float itd_seconds = head_radius * std::sin(azimuth) / speed_of_sound;
        int itd_samples = static_cast<int>(itd_seconds * impl_->sample_rate);

        // Inter-aural Level Difference (ILD) — 头部阴影
        float ild_left = 1.0f - std::max(0.f, std::sin(azimuth)) * 0.7f;
        float ild_right = 1.0f - std::max(0.f, -std::sin(azimuth)) * 0.7f;
        ild_left = std::clamp(ild_left, 0.1f, 1.0f);
        ild_right = std::clamp(ild_right, 0.1f, 1.0f);

        // 指向性 (心形模式)
        if (src.directivity > 0) {
            Vec3 source_forward = lis.forward; // 简化：朝向听者
            float angle = std::acos(dir.dot(source_forward));
            float directivity_gain =
                (1.0f - src.directivity) +
                src.directivity * 0.5f * (1.0f + std::cos(angle));
            distance_gain *= directivity_gain;
        }

        // 总增益
        float total_gain = impl_->master_gain * src.gain * distance_gain;

        // ── 混音到立体声输出 ──
        size_t frame_count = std::min(state.buffer.size(), max_samples);
        for (size_t i = 0; i < frame_count; i++) {
            float sample = state.buffer[i] / 32768.0f * total_gain;

            // 左声道 (带 ITD)
            int left_idx = i * 2;
            float left_val = sample * ild_left;
            if (left_idx < static_cast<int>(max_samples * 2)) {
                output[left_idx] = std::clamp(
                    output[left_idx] + static_cast<int16_t>(left_val * 32767),
                    static_cast<int16_t>(-32768),
                    static_cast<int16_t>(32767));
            }

            // 右声道 (ITD 延迟)
            int right_idx = (i + std::max(0, itd_samples)) * 2 + 1;
            float right_val = sample * ild_right;
            if (right_idx < static_cast<int>(max_samples * 2)) {
                output[right_idx] = std::clamp(
                    output[right_idx] + static_cast<int16_t>(right_val * 32767),
                    static_cast<int16_t>(-32768),
                    static_cast<int16_t>(32767));
            }
        }
    }

    return max_samples;
}

// ── 属性设置 ────────────────────────────────────────
void WebRTCSpatialAudioMixer::set_listener(const Listener& listener) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    impl_->listener = listener;
}

void WebRTCSpatialAudioMixer::set_room_acoustics(
    const RoomAcoustics& acoustics) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    impl_->room = acoustics;
}

void WebRTCSpatialAudioMixer::set_master_gain(float gain) {
    impl_->master_gain = std::clamp(gain, 0.0f, 1.0f);
}

void WebRTCSpatialAudioMixer::set_distance_attenuation(
    const DistanceAttenuation& atten) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    impl_->distance_atten = atten;
}

void WebRTCSpatialAudioMixer::enable_doppler(bool enable) {
    impl_->doppler_enabled = enable;
}

void WebRTCSpatialAudioMixer::load_hrtf_dataset(
    const std::vector<HRTFEntry>& dataset) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    impl_->hrtf_data = dataset;
    impl_->hrtf_enabled = !dataset.empty();
}

int WebRTCSpatialAudioMixer::active_source_count() const {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    int count = 0;
    for (const auto& [id, state] : impl_->sources) {
        if (!state.buffer.empty()) count++;
    }
    return count;
}

float WebRTCSpatialAudioMixer::cpu_load_percent() const {
    return impl_->cpu_load;
}

}  // namespace solra::webrtc
