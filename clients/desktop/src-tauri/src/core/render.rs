// 渲染引擎 FFI 桥接
// 对应 core/include/solra/solra_render.h
//
// 通过 ffi.rs 的全局单例调用 C 侧渲染 API。

use crate::core::ffi::{CoreSdk, SolraRenderConfig};
use std::ffi::CStr;

/// 渲染后端类型
#[repr(i32)]
pub enum GpuBackend {
    Auto = 0,
    Metal = 1,
    Vulkan = 2,
    OpenGLES = 3,
}

/// 相机配置
#[repr(C)]
pub struct CameraConfig {
    pub pos_x: f32,
    pub pos_y: f32,
    pub pos_z: f32,
    pub target_x: f32,
    pub target_y: f32,
    pub target_z: f32,
    pub up_x: f32,
    pub up_y: f32,
    pub up_z: f32,
    pub fov: f32,
    pub near: f32,
    pub far: f32,
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

/// GPU 信息
pub struct GpuInfo {
    pub vendor: String,
    pub renderer: String,
    pub version: String,
    pub dedicated_vram_mb: u64,
    pub shared_vram_mb: u64,
}

/// 初始化渲染引擎
pub fn init_render(width: i32, height: i32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;

    let config = SolraRenderConfig {
        backend: GpuBackend::Auto as i32,
        width,
        height,
        vsync: 1,
        msaa_samples: 4,
        enable_hdr: 0,
        clear_color: crate::core::ffi::SolraColor { r: 0.1, g: 0.1, b: 0.15, a: 1.0 },
        native_window: std::ptr::null_mut(),
    };

    let result = unsafe { (sdk.render_init)(&config) };
    if result != 0 {
        return Err(format!("渲染引擎初始化失败: 错误码 {}", result));
    }

    log::info!("Core SDK 渲染引擎初始化成功 ({}x{})", width, height);
    Ok(())
}

/// 开始一帧渲染
pub fn begin_frame() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let result = unsafe { (sdk.render_begin_frame)() };
    if result != 0 {
        return Err(format!("begin_frame 失败: {}", result));
    }
    Ok(())
}

/// 结束一帧渲染
pub fn end_frame() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let result = unsafe { (sdk.render_end_frame)() };
    if result != 0 {
        return Err(format!("end_frame 失败: {}", result));
    }
    Ok(())
}

/// 获取当前 FPS
pub fn get_fps() -> Result<f32, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let fps = unsafe { (sdk.render_get_fps)() };
    Ok(fps)
}

/// 调整渲染尺寸
pub fn resize(width: i32, height: i32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    unsafe { (sdk.render_resize)(width, height) };
    Ok(())
}

/// 获取 GPU 信息
pub fn get_gpu_info() -> Result<GpuInfo, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let mut info: crate::core::ffi::SolraGpuInfo = unsafe { std::mem::zeroed() };

    let result = unsafe { (sdk.render_get_gpu_info)(&mut info) };
    if result != 0 {
        return Err(format!("get_gpu_info 失败: {}", result));
    }

    Ok(GpuInfo {
        vendor: cstr_to_string(&info.vendor),
        renderer: cstr_to_string(&info.renderer),
        version: cstr_to_string(&info.version),
        dedicated_vram_mb: info.dedicated_vram_mb,
        shared_vram_mb: info.shared_vram_mb,
    })
}

/// 关闭渲染引擎
pub fn shutdown() {
    if let Some(sdk) = CoreSdk::get() {
        unsafe { (sdk.render_shutdown)() };
        log::info!("Core SDK 渲染引擎已关闭");
    }
}

fn cstr_to_string(buf: &[u8]) -> String {
    CStr::from_bytes_until_nul(buf)
        .map(|c| c.to_string_lossy().into_owned())
        .unwrap_or_else(|_| "unknown".to_string())
}
