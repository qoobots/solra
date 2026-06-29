#pragma once
/**
 * @file spatial_audio.hpp
 * @brief 空间音频通道 — E-086
 *
 * 基于 WebRTC AudioTrack 的空间音频渲染管线，支持：
 * - 3D 位置音频（方位角、仰角、距离衰减）
 * - HRTF 双耳渲染
 * - 房间声学（早期反射 + 混响）
 * - 多人混音（最多 32 路同时）
 * - 音频遮挡/衍射模拟
 */

#include <array>
#include <functional>
#include <memory>
#include <vector>

namespace solra::webrtc {

// ── 三维向量 ────────────────────────────────────────
struct Vec3 {
    float x = 0.f, y = 0.f, z = 0.f;

    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
    float length() const { return std::sqrt(x * x + y * y + z * z); }
    float dot(const Vec3& o) const { return x * o.x + y * o.y + z * o.z; }
    Vec3 normalized() const {
        float len = length();
        return len > 0 ? (*this * (1.f / len)) : Vec3{};
    }
};

// ── 音频源属性 ──────────────────────────────────────
struct AudioSource {
    std::string source_id;       // 说话者标识
    Vec3 position;               // 世界坐标位置
    Vec3 velocity;               // 速度（用于多普勒效应）
    float gain = 1.0f;           // 音量增益 (0-1)
    float directivity = 0.0f;    // 指向性 (0=全向, 1=心形)
    float occlusion = 0.0f;      // 遮挡系数 (0=无遮挡)
    bool is_self = false;        // 是否是自己
};

// ── 听者属性 ────────────────────────────────────────
struct Listener {
    Vec3 position;
    Vec3 forward;                // 前方方向
    Vec3 up;                     // 上方方向
};

// ── 房间声学参数 ────────────────────────────────────
struct RoomAcoustics {
    float room_size = 10.0f;         // 房间尺寸 (m)
    float reverb_time = 0.5f;        // RT60 混响时间 (s)
    float early_reflections = 0.3f;  // 早期反射增益
    float late_reverb = 0.2f;        // 后期混响增益
    float diffusion = 0.7f;          // 扩散系数
    Vec3 room_dimensions{10, 6, 4};  // 房间长宽高
};

// ── 空间音频渲染器接口 ──────────────────────────────
class SpatialAudioRenderer {
public:
    virtual ~SpatialAudioRenderer() = default;

    // 初始化：采样率 + 帧大小
    virtual bool init(int sample_rate = 48000, int frames_per_buffer = 480) = 0;

    // 注册/注销音频源
    virtual bool add_source(const AudioSource& source) = 0;
    virtual bool remove_source(const std::string& source_id) = 0;
    virtual bool update_source(const AudioSource& source) = 0;

    // 提交 PCM 音频帧 (16-bit mono, interleaved)
    virtual bool submit_frame(const std::string& source_id,
                               const int16_t* data,
                               size_t sample_count) = 0;

    // 渲染混音输出 (立体声 16-bit interleaved)
    virtual size_t render(int16_t* output, size_t max_samples) = 0;

    // 更新听者位置
    virtual void set_listener(const Listener& listener) = 0;

    // 房间声学
    virtual void set_room_acoustics(const RoomAcoustics& acoustics) = 0;

    // 全局静音
    virtual void set_master_gain(float gain) = 0;
};

// ── HRTF 数据 ───────────────────────────────────────
struct HRTFEntry {
    float azimuth;               // 方位角 (弧度)
    float elevation;             // 仰角 (弧度)
    std::vector<float> left_ir;  // 左耳冲激响应
    std::vector<float> right_ir; // 右耳冲激响应
};

// ── 距离衰减模型 ────────────────────────────────────
enum class DistanceModel {
    Linear,      // gain = 1 - distance/max_distance
    Inverse,     // gain = ref_distance / (ref_distance + rolloff * (d - ref))
    Exponential, // gain = (d / ref_distance)^(-rolloff)
};

struct DistanceAttenuation {
    DistanceModel model = DistanceModel::Inverse;
    float ref_distance = 1.0f;   // 参考距离 (m)
    float max_distance = 50.0f;  // 最大衰减距离 (m)
    float rolloff = 1.0f;        // 衰减系数
};

// ── 空间音频混音器实现 ──────────────────────────────
class WebRTCSpatialAudioMixer : public SpatialAudioRenderer {
public:
    WebRTCSpatialAudioMixer();
    ~WebRTCSpatialAudioMixer() override;

    bool init(int sample_rate = 48000, int frames_per_buffer = 480) override;

    bool add_source(const AudioSource& source) override;
    bool remove_source(const std::string& source_id) override;
    bool update_source(const AudioSource& source) override;

    bool submit_frame(const std::string& source_id,
                       const int16_t* data,
                       size_t sample_count) override;

    size_t render(int16_t* output, size_t max_samples) override;

    void set_listener(const Listener& listener) override;
    void set_room_acoustics(const RoomAcoustics& acoustics) override;
    void set_master_gain(float gain) override;

    // 扩展接口
    void set_distance_attenuation(const DistanceAttenuation& atten);
    void enable_doppler(bool enable);
    void load_hrtf_dataset(const std::vector<HRTFEntry>& dataset);

    // 统计
    int active_source_count() const;
    float cpu_load_percent() const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace solra::webrtc
