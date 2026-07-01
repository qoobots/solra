// 动画系统 FFI 桥接
// 对应 core/include/solra/solra_animation.h
//
// 通过 ffi.rs 的全局单例调用 Core SDK C API

use std::ffi::{c_char, CString};

use crate::core::ffi::CoreSdk;

/// 动画句柄类型
type SolraAnimationClipHandle = *mut std::ffi::c_void;

// FFI 函数指针类型
type SolraAnimationClipLoadFn = unsafe extern "C" fn(*const c_char) -> SolraAnimationClipHandle;
type SolraAnimationClipEvaluateFn = unsafe extern "C" fn(SolraAnimationClipHandle, f32, *mut [f32; 16], i32) -> i32;
type SolraAnimationClipGetDurationFn = unsafe extern "C" fn(SolraAnimationClipHandle) -> f32;
type SolraAnimationClipDestroyFn = unsafe extern "C" fn(SolraAnimationClipHandle);
type SolraAnimationPlayFn = unsafe extern "C" fn(SolraAnimationClipHandle, *const c_char, f32, f32, i32) -> i32;
type SolraAnimationUpdateFn = unsafe extern "C" fn(SolraAnimationClipHandle, f32) -> i32;
type SolraAnimationGetBoneCountFn = unsafe extern "C" fn(SolraAnimationClipHandle) -> i32;

/// 加载动画文件
pub fn load_animation(path: &str) -> Result<*mut std::ffi::c_void, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let c_path = CString::new(path).map_err(|e| format!("路径编码错误: {}", e))?;

    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationClipLoadFn> =
            sdk.get_symbol(b"solra_animation_clip_load")?;
        let handle = func(c_path.as_ptr());
        if handle.is_null() {
            Err(format!("加载动画失败: {}", path))
        } else {
            log::info!("动画加载成功: {}", path);
            Ok(handle)
        }
    }
}

/// 播放动画
pub fn play(handle: *mut std::ffi::c_void, clip_name: &str, speed: f32, loop_anim: bool) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let c_name = CString::new(clip_name).map_err(|e| format!("名称编码错误: {}", e))?;

    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationPlayFn> =
            sdk.get_symbol(b"solra_animation_play")?;
        let result = func(handle, c_name.as_ptr(), 0.25, speed, loop_anim as i32);
        if result != 0 {
            Err(format!("播放动画失败: 错误码 {}", result))
        } else {
            Ok(())
        }
    }
}

/// 更新动画（每帧调用）
pub fn update(handle: *mut std::ffi::c_void, delta_time: f32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationUpdateFn> =
            sdk.get_symbol(b"solra_animation_update")?;
        let result = func(handle, delta_time);
        if result != 0 {
            Err(format!("更新动画失败: 错误码 {}", result))
        } else {
            Ok(())
        }
    }
}

/// 获取动画时长（秒）
pub fn get_duration(handle: *mut std::ffi::c_void) -> Result<f32, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationClipGetDurationFn> =
            sdk.get_symbol(b"solra_animation_clip_get_duration")?;
        Ok(func(handle))
    }
}

/// 获取骨骼数量
pub fn get_bone_count(handle: *mut std::ffi::c_void) -> Result<i32, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationGetBoneCountFn> =
            sdk.get_symbol(b"solra_animation_get_bone_count")?;
        Ok(func(handle))
    }
}

/// 采样动画骨骼变换矩阵
pub fn evaluate(
    handle: *mut std::ffi::c_void,
    time: f32,
    transforms: &mut [[f32; 16]],
) -> Result<i32, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let max_bones = transforms.len() as i32;
    unsafe {
        let func: libloading::Symbol<'_, SolraAnimationClipEvaluateFn> =
            sdk.get_symbol(b"solra_animation_clip_evaluate")?;
        let count = func(handle, time, transforms.as_mut_ptr(), max_bones);
        if count < 0 {
            Err(format!("采样动画失败: 错误码 {}", count))
        } else {
            Ok(count)
        }
    }
}

/// 销毁动画
pub fn destroy(handle: *mut std::ffi::c_void) {
    if let Some(sdk) = CoreSdk::get() {
        unsafe {
            if let Ok(func) = sdk.get_symbol::<SolraAnimationClipDestroyFn>(b"solra_animation_clip_destroy") {
                func(handle);
            }
        }
    }
}

/// 停止动画（销毁句柄）
pub fn stop(handle: *mut std::ffi::c_void) -> Result<(), String> {
    destroy(handle);
    Ok(())
}

/// 设置表情混合形状（通过 blendshape API）
pub fn set_blend_shape(_avatar_id: &str, _weights: &[f32]) -> Result<(), String> {
    // 当 Core SDK blendshape C API 可用时调用 solra_blendshape_set_weight
    if let Some(sdk) = CoreSdk::get() {
        // 尝试调用 solra_blendshape_reset / solra_blendshape_set_weight
        if let Ok(_) = sdk.get_symbol::<unsafe extern "C" fn()>(b"solra_blendshape_reset") {
            log::info!("BlendShape API 可用，设置表情权重: {} shapes", _weights.len());
            // TODO: 创建 SolraBlendShapeConfig 并通过 C API 设置
            return Ok(());
        }
    }
    log::warn!("BlendShape API 未就绪（Core SDK 动画模块未加载）");
    Ok(())
}
