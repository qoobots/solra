// 渲染引擎 FFI 桥接
// 对应 core/include/solra/solra_render.h
//
// TODO: 当 C 侧渲染 API 实现后，取消注释并通过 ffi.rs 的全局单例调用

use std::ffi::c_float;

/// 渲染后端类型
#[repr(i32)]
pub enum GpuBackend {
    OpenGL = 0,
    Vulkan = 1,
    DirectX = 2,
}

/// 相机配置
#[repr(C)]
pub struct CameraConfig {
    pub pos_x: c_float,
    pub pos_y: c_float,
    pub pos_z: c_float,
    pub target_x: c_float,
    pub target_y: c_float,
    pub target_z: c_float,
    pub up_x: c_float,
    pub up_y: c_float,
    pub up_z: c_float,
    pub fov: c_float,
    pub near: c_float,
    pub far: c_float,
}

impl Default for CameraConfig {
    fn default() -> Self {
        Self {
            pos_x: 0.0, pos_y: 1.6, pos_z: 5.0,
            target_x: 0.0, target_y: 0.0, target_z: 0.0,
            up_x: 0.0, up_y: 1.0, up_z: 0.0,
            fov: 60.0,
            near: 0.1,
            far: 1000.0,
        }
    }
}

/// 创建 3D 场景 (stub)
pub fn create_scene(_scene_id: &str) -> Result<(), String> {
    Err("render::create_scene 尚未实现".into())
}

/// 更新渲染帧 (stub)
pub fn update(_delta_time: f32) -> Result<(), String> {
    Err("render::update 尚未实现".into())
}

/// 获取当前 FPS (stub)
pub fn get_fps() -> Result<f32, String> {
    Err("render::get_fps 尚未实现".into())
}

/// 设置相机 (stub)
pub fn set_camera(_camera: &CameraConfig) -> Result<(), String> {
    Err("render::set_camera 尚未实现".into())
}

/// 调整渲染尺寸 (stub)
pub fn resize(_width: i32, _height: i32) -> Result<(), String> {
    Err("render::resize 尚未实现".into())
}
