#pragma once
/// @file camera_system.hpp
/// @brief 相机系统 —— 第一人称/第三人称相机 + 平滑过渡
/// @ingroup core/render
/// @priority P2 (原型阶段)

#include <cmath>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/quaternion.hpp>
#include <glm/gtx/quaternion.hpp>
#include <vector>
#include <functional>
#include <chrono>

namespace solra::core::render {

// ============================================================================
// 相机模式
// ============================================================================

enum class CameraMode : uint8_t {
  kFirstPerson,     // WASD + 鼠标视角
  kThirdPerson,     // 第三人称跟随
  kOrbit,           // 轨道相机
  kFreeFly,         // 自由飞行
  kCinematic,       // 电影相机（沿路径自动移动）
};

// ============================================================================
// 相机投影类型
// ============================================================================

enum class ProjectionType : uint8_t {
  kPerspective,
  kOrthographic,
};

// ============================================================================
// 相机参数
// ============================================================================

struct CameraParams {
  // 投影
  ProjectionType projection = ProjectionType::kPerspective;
  float fov_y_degrees      = 60.0f;
  float near_plane         = 0.1f;
  float far_plane          = 1000.0f;
  float aspect_ratio       = 16.0f / 9.0f;
  float ortho_size         = 10.0f;   // 正交投影半高

  // 移动
  float move_speed         = 5.0f;    // 单位/秒
  float sprint_multiplier  = 2.5f;
  float look_sensitivity   = 0.15f;   // 鼠标灵敏度
  float zoom_speed         = 1.0f;    // 滚轮缩放

  // 第三人称
  float third_person_distance = 3.0f;
  float third_person_height_offset = 1.5f;
  float third_person_min_distance = 1.0f;
  float third_person_max_distance = 10.0f;

  // 限制
  float pitch_min_degrees  = -89.0f;
  float pitch_max_degrees  = 89.0f;
  bool  invert_y           = false;

  // 平滑
  float position_smooth    = 0.0f;    // 0 = instant, 1 = max smooth
  float rotation_smooth    = 0.0f;
  float zoom_smooth        = 0.0f;
};

// ============================================================================
// 相机关键帧（用于电影相机路径）
// ============================================================================

struct CameraKeyframe {
  glm::vec3 position;
  glm::vec3 target;       // 或使用方向
  float fov_degrees;
  float duration_seconds; // 到达此关键帧的时间
  float hold_seconds;     // 停留时间

  // 缓动函数
  enum class Easing : uint8_t {
    kLinear,
    kEaseIn,
    kEaseOut,
    kEaseInOut,
    kCubicBezier,
  };
  Easing easing = Easing::kEaseInOut;
  float bezier_p1x = 0.42f, bezier_p1y = 0.0f;
  float bezier_p2x = 0.58f, bezier_p2y = 1.0f;
};

// ============================================================================
// 相机过渡动画
// ============================================================================

struct CameraTransition {
  glm::vec3 from_position, to_position;
  glm::vec3 from_target, to_target;
  float from_fov, to_fov;
  float duration_seconds;
  float elapsed_seconds;

  CameraKeyframe::Easing easing;

  bool IsComplete() const { return elapsed_seconds >= duration_seconds; }
  float Progress() const {
    if (duration_seconds <= 0.0f) return 1.0f;
    return std::min(1.0f, elapsed_seconds / duration_seconds);
  }
};

// ============================================================================
// 相机类
// ============================================================================

class Camera {
 public:
  Camera();
  ~Camera() = default;

  // === 参数 ===
  void SetParams(const CameraParams& params) { params_ = params; }
  CameraParams& GetParams() { return params_; }
  const CameraParams& GetParams() const { return params_; }

  // === 模式 ===
  void SetMode(CameraMode mode);
  CameraMode GetMode() const { return mode_; }

  // === 位置/朝向 ===
  void SetPosition(const glm::vec3& pos);
  void SetTarget(const glm::vec3& target);
  void SetLookAt(const glm::vec3& eye, const glm::vec3& center, const glm::vec3& up = {0,1,0});

  glm::vec3 GetPosition() const { return current_position_; }
  glm::vec3 GetTarget() const { return current_target_; }
  glm::vec3 GetForward() const;
  glm::vec3 GetRight() const;
  glm::vec3 GetUp() const;

  // === 投影 ===
  void SetFov(float fov_degrees) { params_.fov_y_degrees = fov_degrees; }
  void SetAspectRatio(float ratio) { params_.aspect_ratio = ratio; }
  void SetClipPlanes(float near, float far) {
    params_.near_plane = near;
    params_.far_plane = far;
  }

  // === 矩阵 ===
  glm::mat4 GetViewMatrix() const;
  glm::mat4 GetProjectionMatrix() const;
  glm::mat4 GetViewProjectionMatrix() const;

  // === 输入处理 ===
  /// 处理鼠标移动 (delta in pixels)
  void OnMouseMove(float delta_x, float delta_y);

  /// 处理滚轮
  void OnMouseScroll(float delta);

  /// 处理键盘移动 (WASD + QE)
  void OnKeyboardMove(bool forward, bool backward,
                       bool left, bool right,
                       bool up, bool down,
                       bool sprint, float delta_time);

  // === 第一人称 ===
  /// 设置第一人称 Yaw/Pitch (度)
  void SetYaw(float yaw_degrees);
  void SetPitch(float pitch_degrees);
  float GetYaw() const { return yaw_; }
  float GetPitch() const { return pitch_; }

  /// 第一人称向前移动
  void MoveForward(float distance);
  void MoveRight(float distance);
  void MoveUp(float distance);

  // === 第三人称 ===
  /// 设置跟随目标（第三人称模式）
  void SetFollowTarget(const glm::vec3& target, float height_offset = 1.5f);

  // === 轨道相机 ===
  void Orbit(float delta_yaw, float delta_pitch);
  void Zoom(float delta);

  // === 过渡 ===
  /// 平滑过渡到目标位置
  void TransitionTo(const glm::vec3& position, const glm::vec3& target,
                     float duration_seconds,
                     CameraKeyframe::Easing easing = CameraKeyframe::Easing::kEaseInOut);

  /// 沿关键帧路径过渡
  void TransitionAlongPath(const std::vector<CameraKeyframe>& keyframes);

  /// 是否正在过渡中
  bool IsTransitioning() const;

  /// 取消当前过渡
  void CancelTransition();

  // === 更新 ===
  /// 每帧调用，处理平滑和过渡
  void Update(float delta_time);

  // === 震动效果 ===
  void Shake(float intensity, float duration_seconds);

 private:
  CameraMode mode_ = CameraMode::kFreeFly;
  CameraParams params_;

  // 当前状态
  glm::vec3 current_position_{0, 0, 5};
  glm::vec3 current_target_{0, 0, 0};
  float yaw_   = -90.0f;
  float pitch_ = 0.0f;

  // 平滑目标（用于平滑跟随）
  glm::vec3 smooth_position_{0, 0, 5};
  glm::vec3 smooth_target_{0, 0, 0};

  // 第三人称跟随
  glm::vec3 follow_target_{0, 0, 0};
  float follow_height_offset_ = 1.5f;

  // 过渡
  std::vector<CameraTransition> transitions_;
  size_t current_keyframe_ = 0;

  // 震动
  float shake_intensity_ = 0.0f;
  float shake_duration_ = 0.0f;
  float shake_elapsed_ = 0.0f;

  // 缓动函数
  static float EaseValue(float t, CameraKeyframe::Easing easing,
                          float p1x = 0.42f, float p1y = 0.0f,
                          float p2x = 0.58f, float p2y = 1.0f);

  void UpdateFirstPerson(float dt);
  void UpdateThirdPerson(float dt);
  void UpdateOrbit(float dt);
  void UpdateFreeFly(float dt);
  void UpdateCinematic(float dt);

  void ApplySmoothing(float dt);
  void ApplyTransition(float dt);
  void ApplyShake(float dt);
};

} // namespace solra::core::render
