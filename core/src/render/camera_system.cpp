#include "camera_system.hpp"

#include <algorithm>
#include <random>
#include <spdlog/spdlog.h>

namespace solra::core::render {

// ============================================================================
// Camera 构造
// ============================================================================

Camera::Camera() {
  current_position_ = {0.0f, 0.0f, 5.0f};
  current_target_   = {0.0f, 0.0f, 0.0f};
  smooth_position_  = current_position_;
  smooth_target_    = current_target_;
}

// ============================================================================
// 模式切换
// ============================================================================

void Camera::SetMode(CameraMode mode) {
  if (mode_ == mode) return;
  spdlog::debug("Camera mode: {} → {}",
                static_cast<int>(mode_), static_cast<int>(mode));
  mode_ = mode;
}

// ============================================================================
// 位置/朝向
// ============================================================================

void Camera::SetPosition(const glm::vec3& pos) {
  current_position_ = pos;
  smooth_position_  = pos;
}

void Camera::SetTarget(const glm::vec3& target) {
  current_target_ = target;
  smooth_target_  = target;
}

void Camera::SetLookAt(const glm::vec3& eye, const glm::vec3& center,
                        const glm::vec3& up) {
  current_position_ = eye;
  current_target_   = center;
  smooth_position_  = eye;
  smooth_target_    = center;

  // 计算 yaw/pitch
  glm::vec3 dir = glm::normalize(center - eye);
  yaw_   = glm::degrees(std::atan2(dir.x, -dir.z));
  pitch_ = glm::degrees(std::asin(dir.y));
}

glm::vec3 Camera::GetForward() const {
  return glm::normalize(current_target_ - current_position_);
}

glm::vec3 Camera::GetRight() const {
  return glm::normalize(glm::cross(GetForward(), glm::vec3(0, 1, 0)));
}

glm::vec3 Camera::GetUp() const {
  return glm::normalize(glm::cross(GetRight(), GetForward()));
}

// ============================================================================
// 矩阵
// ============================================================================

glm::mat4 Camera::GetViewMatrix() const {
  return glm::lookAt(current_position_, current_target_,
                      glm::vec3(0.0f, 1.0f, 0.0f));
}

glm::mat4 Camera::GetProjectionMatrix() const {
  if (params_.projection == ProjectionType::kOrthographic) {
    float half_h = params_.ortho_size;
    float half_w = half_h * params_.aspect_ratio;
    return glm::ortho(-half_w, half_w, -half_h, half_h,
                       params_.near_plane, params_.far_plane);
  }
  return glm::perspective(glm::radians(params_.fov_y_degrees),
                           params_.aspect_ratio,
                           params_.near_plane, params_.far_plane);
}

glm::mat4 Camera::GetViewProjectionMatrix() const {
  return GetProjectionMatrix() * GetViewMatrix();
}

// ============================================================================
// 输入处理
// ============================================================================

void Camera::OnMouseMove(float delta_x, float delta_y) {
  if (mode_ == CameraMode::kCinematic) return;

  float dx = delta_x * params_.look_sensitivity;
  float dy = delta_y * params_.look_sensitivity;
  if (params_.invert_y) dy = -dy;

  yaw_   += dx;
  pitch_ -= dy;

  // 限制 pitch
  pitch_ = std::clamp(pitch_, params_.pitch_min_degrees, params_.pitch_max_degrees);
}

void Camera::OnMouseScroll(float delta) {
  if (mode_ == CameraMode::kThirdPerson) {
    params_.third_person_distance -= delta * params_.zoom_speed;
    params_.third_person_distance = std::clamp(
        params_.third_person_distance,
        params_.third_person_min_distance,
        params_.third_person_max_distance);
  } else if (mode_ == CameraMode::kOrbit) {
    Zoom(delta);
  }
}

void Camera::OnKeyboardMove(bool forward, bool backward,
                              bool left, bool right,
                              bool up, bool down,
                              bool sprint, float delta_time) {
  if (mode_ == CameraMode::kCinematic) return;

  float speed = params_.move_speed;
  if (sprint) speed *= params_.sprint_multiplier;

  glm::vec3 move{0};

  if (mode_ == CameraMode::kFirstPerson || mode_ == CameraMode::kFreeFly) {
    glm::vec3 fwd = GetForward();
    fwd.y = 0; // 第一人称保持水平移动
    if (glm::length(fwd) < 0.001f) fwd = {0, 0, -1};
    fwd = glm::normalize(fwd);

    glm::vec3 rgt = glm::normalize(glm::cross(fwd, glm::vec3(0, 1, 0)));

    if (forward)  move += fwd;
    if (backward) move -= fwd;
    if (right)    move += rgt;
    if (left)     move -= rgt;

    if (mode_ == CameraMode::kFreeFly) {
      if (up)   move += glm::vec3(0, 1, 0);
      if (down) move -= glm::vec3(0, 1, 0);
    }
  }

  if (glm::length(move) > 0.001f) {
    move = glm::normalize(move) * speed * delta_time;
    current_position_ += move;
    current_target_   += move;
  }
}

// ============================================================================
// 第一人称
// ============================================================================

void Camera::SetYaw(float yaw_degrees) { yaw_ = yaw_degrees; }
void Camera::SetPitch(float pitch_degrees) {
  pitch_ = std::clamp(pitch_degrees, params_.pitch_min_degrees,
                       params_.pitch_max_degrees);
}

void Camera::MoveForward(float distance) {
  glm::vec3 dir = GetForward();
  current_position_ += dir * distance;
  current_target_   += dir * distance;
}

void Camera::MoveRight(float distance) {
  glm::vec3 dir = GetRight();
  current_position_ += dir * distance;
  current_target_   += dir * distance;
}

void Camera::MoveUp(float distance) {
  current_position_.y += distance;
  current_target_.y   += distance;
}

// ============================================================================
// 第三人称
// ============================================================================

void Camera::SetFollowTarget(const glm::vec3& target, float height_offset) {
  follow_target_ = target;
  follow_height_offset_ = height_offset;
}

// ============================================================================
// 轨道相机
// ============================================================================

void Camera::Orbit(float delta_yaw, float delta_pitch) {
  if (mode_ != CameraMode::kOrbit) return;

  yaw_   += delta_yaw * params_.look_sensitivity * 0.5f;
  pitch_ += delta_pitch * params_.look_sensitivity * 0.5f;
  pitch_ = std::clamp(pitch_, params_.pitch_min_degrees, params_.pitch_max_degrees);
}

void Camera::Zoom(float delta) {
  if (mode_ != CameraMode::kOrbit && mode_ != CameraMode::kThirdPerson) return;

  float dist = glm::distance(current_position_, current_target_);
  dist -= delta * params_.zoom_speed;
  dist = std::clamp(dist, 1.0f, 50.0f);

  glm::vec3 dir = glm::normalize(current_position_ - current_target_);
  current_position_ = current_target_ + dir * dist;
}

// ============================================================================
// 过渡
// ============================================================================

void Camera::TransitionTo(const glm::vec3& position, const glm::vec3& target,
                           float duration_seconds, CameraKeyframe::Easing easing) {
  CameraTransition t;
  t.from_position    = current_position_;
  t.to_position      = position;
  t.from_target      = current_target_;
  t.to_target        = target;
  t.from_fov         = params_.fov_y_degrees;
  t.to_fov           = params_.fov_y_degrees;
  t.duration_seconds = duration_seconds;
  t.elapsed_seconds  = 0.0f;
  t.easing           = easing;

  transitions_.clear();
  transitions_.push_back(t);

  spdlog::debug("Camera transition: ({:.1f},{:.1f},{:.1f}) → ({:.1f},{:.1f},{:.1f}) over {:.1f}s",
                position.x, position.y, position.z,
                target.x, target.y, target.z,
                duration_seconds);
}

void Camera::TransitionAlongPath(const std::vector<CameraKeyframe>& keyframes) {
  if (keyframes.empty()) return;

  transitions_.clear();
  current_keyframe_ = 0;

  for (size_t i = 0; i < keyframes.size(); ++i) {
    CameraTransition t;
    t.from_position = (i == 0) ? current_position_
                               : keyframes[i - 1].position;
    t.to_position   = keyframes[i].position;
    t.from_target   = (i == 0) ? current_target_
                               : keyframes[i - 1].target;
    t.to_target     = keyframes[i].target;
    t.from_fov      = (i == 0) ? params_.fov_y_degrees
                               : keyframes[i - 1].fov_degrees;
    t.to_fov        = keyframes[i].fov_degrees;
    t.duration_seconds = keyframes[i].duration_seconds;
    t.elapsed_seconds  = 0.0f;
    t.easing        = keyframes[i].easing;

    // 加上停留时间
    if (keyframes[i].hold_seconds > 0.0f) {
      CameraTransition hold;
      hold.from_position = keyframes[i].position;
      hold.to_position   = keyframes[i].position;
      hold.from_target   = keyframes[i].target;
      hold.to_target     = keyframes[i].target;
      hold.from_fov      = keyframes[i].fov_degrees;
      hold.to_fov        = keyframes[i].fov_degrees;
      hold.duration_seconds = keyframes[i].hold_seconds;
      hold.elapsed_seconds  = 0.0f;
      hold.easing = CameraKeyframe::Easing::kLinear;
      transitions_.push_back(hold);
    }

    transitions_.push_back(t);
  }

  spdlog::info("Camera path: {} keyframes, total {} transitions",
               keyframes.size(), transitions_.size());
}

bool Camera::IsTransitioning() const {
  return !transitions_.empty() && current_keyframe_ < transitions_.size();
}

void Camera::CancelTransition() {
  transitions_.clear();
  current_keyframe_ = 0;
}

// ============================================================================
// 震动
// ============================================================================

void Camera::Shake(float intensity, float duration_seconds) {
  shake_intensity_ = intensity;
  shake_duration_  = duration_seconds;
  shake_elapsed_   = 0.0f;
}

// ============================================================================
// 更新
// ============================================================================

void Camera::Update(float delta_time) {
  switch (mode_) {
    case CameraMode::kFirstPerson: UpdateFirstPerson(delta_time); break;
    case CameraMode::kThirdPerson: UpdateThirdPerson(delta_time); break;
    case CameraMode::kOrbit:      UpdateOrbit(delta_time);      break;
    case CameraMode::kFreeFly:    UpdateFreeFly(delta_time);    break;
    case CameraMode::kCinematic:  UpdateCinematic(delta_time);  break;
  }

  ApplySmoothing(delta_time);
  ApplyTransition(delta_time);
  ApplyShake(delta_time);
}

void Camera::UpdateFirstPerson(float dt) {
  (void)dt;
  // 从 yaw/pitch 计算目标方向
  float yaw_rad   = glm::radians(yaw_);
  float pitch_rad = glm::radians(pitch_);

  glm::vec3 direction;
  direction.x = std::cos(pitch_rad) * std::cos(yaw_rad);
  direction.y = std::sin(pitch_rad);
  direction.z = std::cos(pitch_rad) * std::sin(yaw_rad);
  direction = glm::normalize(direction);

  current_target_ = current_position_ + direction;
}

void Camera::UpdateThirdPerson(float dt) {
  (void)dt;
  // 相机位于跟随目标后方
  float yaw_rad   = glm::radians(yaw_);
  float pitch_rad = glm::radians(pitch_);

  glm::vec3 offset;
  offset.x = -std::cos(pitch_rad) * std::cos(yaw_rad);
  offset.y = -std::sin(pitch_rad);
  offset.z = -std::cos(pitch_rad) * std::sin(yaw_rad);

  glm::vec3 target_pos = follow_target_ + glm::vec3(0, follow_height_offset_, 0);
  current_position_ = target_pos + offset * params_.third_person_distance;
  current_target_   = target_pos;
}

void Camera::UpdateOrbit(float dt) {
  (void)dt;
  float yaw_rad   = glm::radians(yaw_);
  float pitch_rad = glm::radians(pitch_);

  float dist = glm::distance(current_position_, current_target_);

  glm::vec3 offset;
  offset.x = std::cos(pitch_rad) * std::sin(yaw_rad);
  offset.y = std::sin(pitch_rad);
  offset.z = std::cos(pitch_rad) * std::cos(yaw_rad);

  current_position_ = current_target_ - offset * dist;
}

void Camera::UpdateFreeFly(float dt) {
  (void)dt;
  float yaw_rad   = glm::radians(yaw_);
  float pitch_rad = glm::radians(pitch_);

  glm::vec3 direction;
  direction.x = std::cos(pitch_rad) * std::cos(yaw_rad);
  direction.y = std::sin(pitch_rad);
  direction.z = std::cos(pitch_rad) * std::sin(yaw_rad);

  current_target_ = current_position_ + glm::normalize(direction);
}

void Camera::UpdateCinematic(float dt) {
  (void)dt;
  // 由过渡系统驱动
}

// ============================================================================
// 平滑
// ============================================================================

void Camera::ApplySmoothing(float dt) {
  if (params_.position_smooth <= 0.0f && params_.rotation_smooth <= 0.0f) {
    smooth_position_ = current_position_;
    smooth_target_   = current_target_;
    return;
  }

  float pos_factor = 1.0f - std::exp(-params_.position_smooth * 10.0f * dt);
  float rot_factor = 1.0f - std::exp(-params_.rotation_smooth * 10.0f * dt);

  smooth_position_ = glm::mix(smooth_position_, current_position_,
                               std::clamp(pos_factor, 0.0f, 1.0f));
  smooth_target_   = glm::mix(smooth_target_, current_target_,
                               std::clamp(rot_factor, 0.0f, 1.0f));
}

// ============================================================================
// 过渡
// ============================================================================

void Camera::ApplyTransition(float dt) {
  if (transitions_.empty()) return;

  auto& t = transitions_[current_keyframe_];
  t.elapsed_seconds += dt;

  float progress = std::clamp(t.Progress(), 0.0f, 1.0f);
  float eased = EaseValue(progress, t.easing,
                           t.from_fov, t.to_fov, 0.0f, 0.0f);

  current_position_ = glm::mix(t.from_position, t.to_position, eased);
  current_target_   = glm::mix(t.from_target, t.to_target, eased);
  params_.fov_y_degrees = glm::mix(t.from_fov, t.to_fov, eased);

  // 当前过渡完成，推进到下一个
  if (t.IsComplete()) {
    current_keyframe_++;
    if (current_keyframe_ >= transitions_.size()) {
      transitions_.clear();
      current_keyframe_ = 0;
      spdlog::debug("Camera transition complete");
    }
  }
}

// ============================================================================
// 震动
// ============================================================================

void Camera::ApplyShake(float dt) {
  if (shake_elapsed_ >= shake_duration_) {
    shake_intensity_ = 0.0f;
    return;
  }

  shake_elapsed_ += dt;
  float progress = shake_elapsed_ / shake_duration_;
  float decay = 1.0f - progress; // 线性衰减

  static std::mt19937 rng(std::random_device{}());
  static std::uniform_real_distribution<float> dist(-1.0f, 1.0f);

  glm::vec3 shake_offset(
      dist(rng) * shake_intensity_ * decay,
      dist(rng) * shake_intensity_ * decay * 0.5f, // 垂直震动减半
      dist(rng) * shake_intensity_ * decay
  );

  current_position_ += shake_offset;
  current_target_   += shake_offset * 0.5f;
}

// ============================================================================
// 缓动函数
// ============================================================================

float Camera::EaseValue(float t, CameraKeyframe::Easing easing,
                         float p1x, float p1y, float p2x, float p2y) {
  t = std::clamp(t, 0.0f, 1.0f);

  switch (easing) {
    case CameraKeyframe::Easing::kLinear:
      return t;

    case CameraKeyframe::Easing::kEaseIn:
      return t * t;

    case CameraKeyframe::Easing::kEaseOut:
      return t * (2.0f - t);

    case CameraKeyframe::Easing::kEaseInOut:
      return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;

    case CameraKeyframe::Easing::kCubicBezier:
      {
        // Newton-Raphson 求解 cubic bezier
        float guess = t;
        for (int i = 0; i < 8; ++i) {
          float x = (1.0f - guess) * (1.0f - guess) * (1.0f - guess) * 0.0f
                  + 3.0f * (1.0f - guess) * (1.0f - guess) * guess * p1x
                  + 3.0f * (1.0f - guess) * guess * guess * p2x
                  + guess * guess * guess * 1.0f;
          float dx = -3.0f * (1.0f - guess) * (1.0f - guess) * 0.0f
                   + 3.0f * (1.0f - guess) * (1.0f - guess) * p1x
                   - 6.0f * (1.0f - guess) * guess * p1x
                   + 6.0f * (1.0f - guess) * guess * p2x
                   - 3.0f * guess * guess * p2x
                   + 3.0f * guess * guess * 1.0f;
          if (std::abs(dx) < 1e-7f) break;
          guess -= (x - t) / dx;
        }
        float y = (1.0f - guess) * (1.0f - guess) * (1.0f - guess) * 0.0f
                + 3.0f * (1.0f - guess) * (1.0f - guess) * guess * p1y
                + 3.0f * (1.0f - guess) * guess * guess * p2y
                + guess * guess * guess * 1.0f;
        return y;
      }
  }

  return t;
}

} // namespace solra::core::render
