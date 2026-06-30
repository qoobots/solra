// 渲染引擎 FFI 桥接
// 对应 core/include/solra/solra_render.h

use super::ffi::{CoreSdk, SolraRawHandle, SolraResult};
use std::ffi::{c_char, c_float, c_int, CString};

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

type SolraRenderCreateSceneFn = unsafe extern "C" fn(SolraRawHandle, *const c_char) -> i32;
type SolraRenderUpdateFn = unsafe extern "C" fn(SolraRawHandle, c_float) -> i32;
type SolraRenderGetFpsFn = unsafe extern "C" fn(SolraRawHandle, *mut c_float) -> i32;
type SolraRenderSetCameraFn = unsafe extern "C" fn(SolraRawHandle, *const CameraConfig) -> i32;
type SolraRenderResizeFn = unsafe extern "C" fn(SolraRawHandle, c_int, c_int) -> i32;

/// 创建 3D 场景
pub fn create_scene(scene_id: &str) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let scene_id_c = CString::new(scene_id).map_err(|e| format!("无效的场景ID: {}", e))?;

    let result = unsafe {
        let func: SolraRenderCreateSceneFn = std::mem::transmute(
            sdk.get_symbol::<SolraRenderCreateSceneFn>(b"solra_render_create_scene")?
        );
        func(handle.as_ptr(), scene_id_c.as_ptr())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("创建场景失败: {}", result));
    }

    Ok(())
}

/// 更新渲染帧
pub fn update(delta_time: f32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraRenderUpdateFn = std::mem::transmute(
            sdk.get_symbol::<SolraRenderUpdateFn>(b"solra_render_update")?
        );
        func(handle.as_ptr(), delta_time)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("渲染更新失败: {}", result));
    }

    Ok(())
}

/// 获取当前 FPS
pub fn get_fps() -> Result<f32, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let mut fps: f32 = 0.0;
    let result = unsafe {
        let func: SolraRenderGetFpsFn = std::mem::transmute(
            sdk.get_symbol::<SolraRenderGetFpsFn>(b"solra_render_get_fps")?
        );
        func(handle.as_ptr(), &mut fps)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("获取FPS失败: {}", result));
    }

    Ok(fps)
}

/// 设置相机
pub fn set_camera(camera: &CameraConfig) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraRenderSetCameraFn = std::mem::transmute(
            sdk.get_symbol::<SolraRenderSetCameraFn>(b"solra_render_set_camera")?
        );
        func(handle.as_ptr(), camera)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("设置相机失败: {}", result));
    }

    Ok(())
}

/// 调整渲染尺寸
pub fn resize(width: i32, height: i32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraRenderResizeFn = std::mem::transmute(
            sdk.get_symbol::<SolraRenderResizeFn>(b"solra_render_resize")?
        );
        func(handle.as_ptr(), width, height)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("调整渲染尺寸失败: {}", result));
    }

    Ok(())
}
