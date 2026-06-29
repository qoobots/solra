// 流式加载引擎 FFI 桥接
// 对应 core/include/solra/solra_streaming.h

use super::ffi::{CoreSdk, SolraHandle, SolraResult};
use std::ffi::{c_char, c_void, CString};

type SolraStreamingLoadSpaceFn = unsafe extern "C" fn(
    SolraHandle, *const c_char,
    Option<unsafe extern "C" fn(f32, *const c_char, *mut c_void)>,
    *mut c_void,
) -> i32;
type SolraStreamingCancelFn = unsafe extern "C" fn(SolraHandle) -> i32;
type SolraStreamingGetCacheSizeFn = unsafe extern "C" fn(SolraHandle, *mut usize) -> i32;

/// 加载进度回调
pub type ProgressCallback = Box<dyn Fn(f32, &str) + Send + 'static>;

/// 流式加载空间
pub fn load_space(
    space_id: &str,
    callback: ProgressCallback,
) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let space_id_c = CString::new(space_id).map_err(|e| format!("无效空间ID: {}", e))?;

    let callback_box = Box::new(callback);
    let user_data = Box::into_raw(callback_box) as *mut c_void;

    unsafe extern "C" fn trampoline(progress: f32, status: *const c_char, user_data: *mut c_void) {
        if user_data.is_null() {
            return;
        }
        let callback: &mut ProgressCallback = &mut *(user_data as *mut ProgressCallback);
        let status_str = if status.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(status).to_str().unwrap_or("")
        };
        callback(progress, status_str);
    }

    let result = unsafe {
        let func: SolraStreamingLoadSpaceFn = std::mem::transmute(
            sdk.lib.get(b"solra_streaming_load_space").map_err(|e| format!("{}", e))?
        );
        func(handle, space_id_c.as_ptr(), Some(trampoline), user_data)
    };

    unsafe {
        let _ = Box::from_raw(user_data as *mut ProgressCallback);
    }

    if result != SolraResult::Success as i32 {
        return Err(format!("加载空间失败: {}", result));
    }

    Ok(())
}

/// 取消流式加载
pub fn cancel() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraStreamingCancelFn = std::mem::transmute(
            sdk.lib.get(b"solra_streaming_cancel").map_err(|e| format!("{}", e))?
        );
        func(handle)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("取消加载失败: {}", result));
    }

    Ok(())
}

/// 获取缓存大小
pub fn get_cache_size() -> Result<usize, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let mut size: usize = 0;
    let result = unsafe {
        let func: SolraStreamingGetCacheSizeFn = std::mem::transmute(
            sdk.lib.get(b"solra_streaming_get_cache_size").map_err(|e| format!("{}", e))?
        );
        func(handle, &mut size)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("获取缓存大小失败: {}", result));
    }

    Ok(size)
}
