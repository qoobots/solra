// 端侧推理引擎 FFI 桥接
// 对应 core/include/solra/solra_inference.h
//
// 通过 ffi.rs 的全局单例调用 Core SDK C API

use std::ffi::{c_char, CString};

use crate::core::ffi::CoreSdk;

/// 推理回调类型
pub type InferenceCallback = Box<dyn Fn(&str) + Send + 'static>;

// FFI 函数指针类型
type SolraInferenceInitFn = unsafe extern "C" fn(*const c_char) -> i32;
type SolraInferenceGenerateFn = unsafe extern "C" fn(*const c_char, *const c_char, i32, f32, *mut c_char, i32) -> i32;
type SolraInferenceShutdownFn = unsafe extern "C" fn() -> i32;

/// 加载推理模型
pub fn load_model(model_path: &str) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let c_path = CString::new(model_path).map_err(|e| format!("路径编码错误: {}", e))?;

    unsafe {
        let func: libloading::Symbol<'_, SolraInferenceInitFn> =
            sdk.get_symbol(b"solra_inference_init")?;
        let result = func(c_path.as_ptr());
        if result != 0 {
            Err(format!("推理引擎初始化失败: 错误码 {}", result))
        } else {
            log::info!("推理引擎初始化成功: model={}", model_path);
            Ok(())
        }
    }
}

/// 发送消息到端侧推理引擎（同步生成）
pub fn send_message_sync(
    conversation_id: &str,
    message: &str,
    max_tokens: i32,
    temperature: f32,
) -> Result<String, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let c_conv_id = CString::new(conversation_id).map_err(|e| format!("编码错误: {}", e))?;
    let c_message = CString::new(message).map_err(|e| format!("编码错误: {}", e))?;

    let mut buffer = vec![0u8; 4096]; // 最大回复长度

    unsafe {
        let func: libloading::Symbol<'_, SolraInferenceGenerateFn> =
            sdk.get_symbol(b"solra_inference_generate")?;
        let result = func(
            c_conv_id.as_ptr(),
            c_message.as_ptr(),
            max_tokens,
            temperature,
            buffer.as_mut_ptr() as *mut c_char,
            buffer.len() as i32,
        );
        if result < 0 {
            Err(format!("推理生成失败: 错误码 {}", result))
        } else {
            let reply = CString::from_raw(buffer.as_mut_ptr() as *mut c_char)
                .to_string_lossy()
                .into_owned();
            Ok(reply)
        }
    }
}

/// 发送消息到端侧推理引擎（带流式回调）(stub — 异步回调需要 C 侧支持)
pub fn send_message(
    conversation_id: &str,
    message: &str,
    _callback: InferenceCallback,
) -> Result<(), String> {
    // 降级到同步模式
    match send_message_sync(conversation_id, message, 256, 0.7) {
        Ok(reply) => {
            _callback(&reply);
            Ok(())
        }
        Err(e) => {
            log::warn!("Core SDK 推理不可用，使用降级模式: {}", e);
            Err(format!("推理不可用: {}", e))
        }
    }
}

/// 停止推理
pub fn stop() -> Result<(), String> {
    if let Some(sdk) = CoreSdk::get() {
        unsafe {
            if let Ok(func) = sdk.get_symbol::<SolraInferenceShutdownFn>(b"solra_inference_shutdown") {
                func();
                return Ok(());
            }
        }
    }
    Ok(()) // 优雅降级
}
