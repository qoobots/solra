// 动画系统 FFI 桥接
// 对应 core/include/solra/solra_animation.h
//
// TODO: 当 C 侧动画 API 实现后，取消注释并通过 ffi.rs 的全局单例调用

/// 播放动画 (stub)
pub fn play(_avatar_id: &str, _animation_name: &str, _speed: f32) -> Result<(), String> {
    Err("animation::play 尚未实现".into())
}

/// 停止动画 (stub)
pub fn stop(_avatar_id: &str) -> Result<(), String> {
    Err("animation::stop 尚未实现".into())
}

/// 设置表情混合形状 (stub)
pub fn set_blend_shape(_avatar_id: &str, _weights: &[f32]) -> Result<(), String> {
    Err("animation::set_blend_shape 尚未实现".into())
}
