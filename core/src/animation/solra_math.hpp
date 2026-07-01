#pragma once
/**
 * @file solra_math.hpp
 * @brief 动画模块轻量数学库 (Vec3, Quat, Mat4)
 *
 * 避免引入 glm 依赖，提供动画 IK 所需的最小数学运算。
 */

#include <cmath>
#include <array>
#include <cstring>

namespace solra::animation {

// ── Vec3 ─────────────────────────────────────────────
struct Vec3 {
    float x = 0.f, y = 0.f, z = 0.f;

    Vec3() = default;
    Vec3(float x_, float y_, float z_) : x(x_), y(y_), z(z_) {}

    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(float s) const { return {x * s, y * s, z * s}; }
    Vec3 operator/(float s) const { float inv = 1.f / s; return {x * inv, y * inv, z * inv}; }

    float dot(const Vec3& o) const { return x * o.x + y * o.y + z * o.z; }
    Vec3 cross(const Vec3& o) const {
        return {y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x};
    }

    float length() const { return std::sqrt(x * x + y * y + z * z); }
    Vec3 normalized() const {
        float len = length();
        return len > 1e-8f ? *this / len : Vec3{};
    }
};

// ── Quat ─────────────────────────────────────────────
struct Quat {
    float x = 0.f, y = 0.f, z = 0.f, w = 1.f;

    Quat() = default;
    Quat(float x_, float y_, float z_, float w_) : x(x_), y(y_), z(z_), w(w_) {}

    Quat operator*(const Quat& o) const {
        return {
            w * o.x + x * o.w + y * o.z - z * o.y,
            w * o.y - x * o.z + y * o.w + z * o.x,
            w * o.z + x * o.y - y * o.x + z * o.w,
            w * o.w - x * o.x - y * o.y - z * o.z
        };
    }

    void normalize() {
        float len = std::sqrt(x * x + y * y + z * z + w * w);
        if (len > 1e-8f) { x /= len; y /= len; z /= len; w /= len; }
    }

    void from_axis_angle(const Vec3& axis, float angle) {
        float half = angle * 0.5f;
        float s = std::sin(half);
        x = axis.x * s;
        y = axis.y * s;
        z = axis.z * s;
        w = std::cos(half);
    }

    Vec3 to_euler() const {
        // Roll (x), Pitch (y), Yaw (z)
        float sinr_cosp = 2.f * (w * x + y * z);
        float cosr_cosp = 1.f - 2.f * (x * x + y * y);
        float roll = std::atan2(sinr_cosp, cosr_cosp);

        float sinp = 2.f * (w * y - z * x);
        float pitch = std::abs(sinp) >= 1.f ? std::copysign(3.14159265f / 2.f, sinp) : std::asin(sinp);

        float siny_cosp = 2.f * (w * z + x * y);
        float cosy_cosp = 1.f - 2.f * (y * y + z * z);
        float yaw = std::atan2(siny_cosp, cosy_cosp);

        return {roll, pitch, yaw};
    }

    static Quat from_euler(const Vec3& euler) {
        float cx = std::cos(euler.x * 0.5f), sx = std::sin(euler.x * 0.5f);
        float cy = std::cos(euler.y * 0.5f), sy = std::sin(euler.y * 0.5f);
        float cz = std::cos(euler.z * 0.5f), sz = std::sin(euler.z * 0.5f);
        return {
            sx * cy * cz - cx * sy * sz,
            cx * sy * cz + sx * cy * sz,
            cx * cy * sz - sx * sy * cz,
            cx * cy * cz + sx * sy * sz
        };
    }
};

// ── Mat4 (column-major, 16 floats) ──────────────────
using Mat4 = std::array<float, 16>;

inline Mat4 mat4_identity() {
    return {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
}

} // namespace solra::animation
