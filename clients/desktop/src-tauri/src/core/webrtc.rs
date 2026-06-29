// WebRTC 引擎 FFI 桥接
// 对应 core/include/solra/solra_webrtc.h

use super::ffi::{CoreSdk, SolraHandle, SolraResult};
use std::ffi::{c_char, CString};

type SolraWebrtcConnectFn = unsafe extern "C" fn(SolraHandle, *const c_char, *const c_char) -> i32;
type SolraWebrtcSendDataFn = unsafe extern "C" fn(SolraHandle, *const u8, usize) -> i32;
type SolraWebrtcDisconnectFn = unsafe extern "C" fn(SolraHandle) -> i32;

/// 连接到 WebRTC 房间
pub fn connect(room_id: &str, token: &str) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let room_c = CString::new(room_id).map_err(|e| format!("无效房间ID: {}", e))?;
    let token_c = CString::new(token).map_err(|e| format!("无效token: {}", e))?;

    let result = unsafe {
        let func: SolraWebrtcConnectFn = std::mem::transmute(
            sdk.lib.get(b"solra_webrtc_connect").map_err(|e| format!("{}", e))?
        );
        func(handle, room_c.as_ptr(), token_c.as_ptr())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("WebRTC连接失败: {}", result));
    }

    log::info!("WebRTC 已连接到房间: {}", room_id);
    Ok(())
}

/// 发送数据
pub fn send_data(data: &[u8]) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraWebrtcSendDataFn = std::mem::transmute(
            sdk.lib.get(b"solra_webrtc_send_data").map_err(|e| format!("{}", e))?
        );
        func(handle, data.as_ptr(), data.len())
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("WebRTC发送数据失败: {}", result));
    }

    Ok(())
}

/// 断开 WebRTC 连接
pub fn disconnect() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;
    let handle = sdk.handle.lock().unwrap();
    let handle = handle.ok_or("Core SDK 未初始化")?;

    let result = unsafe {
        let func: SolraWebrtcDisconnectFn = std::mem::transmute(
            sdk.lib.get(b"solra_webrtc_disconnect").map_err(|e| format!("{}", e))?
        );
        func(handle)
    };

    if result != SolraResult::Success as i32 {
        return Err(format!("WebRTC断开失败: {}", result));
    }

    log::info!("WebRTC 已断开连接");
    Ok(())
}
