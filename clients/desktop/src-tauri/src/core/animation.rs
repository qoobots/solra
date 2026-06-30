// 动画系统 FFI 桥接
// 对应 core/include/solra/solra_animation.h

use super::ffi::{CoreSdk, SolraRawHandle, SolraResult};
use std::ffi::{c_char, CString};

type SolraAnimationPlayFn = unsafe extern "C" fn(SolraRawHandle, *const c_char, *const c_char, f32) -> i32;
type SolraAnimationStopFn = unsafe extern "C" fn(SolraRawHandle, *const c_char) -> i32;
type SolraAnimationSetBlendShapeFn = unsafe extern "C" fn(SolraRawHandle, *const c_char, *const f32, i32) -> i32;

/// 播放动画
pub fn play(avatar_id: &str, animation_name: &str, speed: f32) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let avatar_c = CString::new(avatar_id).map_err(|e| format!("无效ID: {}", e))?;
    let anim_c = CString::new(animation_name).map_err(|e| format!("无效动画名: {}", e))?;

    let result = unsafe {
        let func: SolraAnimationPlayFn = std::mem::transmute(
            sdk.get_symbol::<SolraAnimationPlayFn>(b"solra_animation_play")?
        );
        func(handle.as_ptr(), avatar_c.as_ptr(), anim_c.as_ptr(), speed)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("播放动画失败: {}", result));
    }

    Ok(())
}

/// 停止动画
pub fn stop(avatar_id: &str) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let avatar_c = CString::new(avatar_id).map_err(|e| format!("无效ID: {}", e))?;

    let result = unsafe {
        let func: SolraAnimationStopFn = std::mem::transmute(
            sdk.get_symbol::<SolraAnimationStopFn>(b"solra_animation_stop")?
        );
        func(handle.as_ptr(), avatar_c.as_ptr())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("停止动画失败: {}", result));
    }

    Ok(())
}

/// 设置表情混合形状
pub fn set_blend_shape(avatar_id: &str, weights: &[f32]) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let avatar_c = CString::new(avatar_id).map_err(|e| format!("无效ID: {}", e))?;

    let result = unsafe {
        let func: SolraAnimationSetBlendShapeFn = std::mem::transmute(
            sdk.get_symbol::<SolraAnimationSetBlendShapeFn>(b"solra_animation_set_blend_shape")?
        );
        func(handle.as_ptr(), avatar_c.as_ptr(), weights.as_ptr(), weights.len() as i32)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("设置表情失败: {}", result));
    }

    Ok(())
}
