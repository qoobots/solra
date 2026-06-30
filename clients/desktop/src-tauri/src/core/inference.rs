// 端侧推理引擎 FFI 桥接
// 对应 core/include/solra/solra_inference.h

use super::ffi::{CoreSdk, SolraRawHandle, SolraResult};
use std::ffi::{c_char, c_void, CString};

type SolraInferenceLoadModelFn = unsafe extern "C" fn(SolraRawHandle, *const c_char) -> i32;
type SolraInferenceSendMessageFn = unsafe extern "C" fn(
    SolraRawHandle, *const c_char, *const c_char,
    Option<unsafe extern "C" fn(*const c_char, *mut c_void)>,
    *mut c_void,
) -> i32;
type SolraInferenceStopFn = unsafe extern "C" fn(SolraRawHandle) -> i32;

/// 推理回调类型（从 C 回调转为 Rust 闭包）
pub type InferenceCallback = Box<dyn Fn(&str) + Send + 'static>;

/// 加载推理模型
pub fn load_model(model_path: &str) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let path_c = CString::new(model_path).map_err(|e| format!("无效路径: {}", e))?;

    let result = unsafe {
        let func: SolraInferenceLoadModelFn = std::mem::transmute(
            sdk.get_symbol::<SolraInferenceLoadModelFn>(b"solra_inference_load_model")?
        );
        func(handle.as_ptr(), path_c.as_ptr())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("加载模型失败: {}", result));
    }

    Ok(())
}

/// 发送消息到端侧推理引擎（带流式回调）
pub fn send_message(
    conversation_id: &str,
    message: &str,
    callback: InferenceCallback,
) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let conv_id_c = CString::new(conversation_id).map_err(|e| format!("无效ID: {}", e))?;
    let msg_c = CString::new(message).map_err(|e| format!("无效消息: {}", e))?;

    // 将 Rust 闭包转为原始指针传给 C 回调
    let callback_box = Box::new(callback);
    let user_data = Box::into_raw(callback_box) as *mut c_void;

    unsafe extern "C" fn trampoline(token: *const c_char, user_data: *mut c_void) {
        if token.is_null() || user_data.is_null() {
            return;
        }
        let callback: &mut InferenceCallback = &mut *(user_data as *mut InferenceCallback);
        let token_str = std::ffi::CStr::from_ptr(token).to_string_lossy();
        callback(&token_str);
    }

    let result = unsafe {
        let func: SolraInferenceSendMessageFn = std::mem::transmute(
            sdk.get_symbol::<SolraInferenceSendMessageFn>(b"solra_inference_send_message")?
        );
        func(
            handle.as_ptr(),
            conv_id_c.as_ptr(),
            msg_c.as_ptr(),
            Some(trampoline),
            user_data,
        )
    };

    // 回收 callback box（注意：C 端不应再使用 user_data）
    unsafe {
        let _ = Box::from_raw(user_data as *mut InferenceCallback);
    }

    if result != SolraResult::Success as i32 {
        return Err(format!("发送消息失败: {}", result));
    }

    Ok(())
}

/// 停止推理
pub fn stop() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraInferenceStopFn = std::mem::transmute(
            sdk.get_symbol::<SolraInferenceStopFn>(b"solra_inference_stop")?
        );
        func(handle.as_ptr())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("停止推理失败: {}", result));
    }

    Ok(())
}
