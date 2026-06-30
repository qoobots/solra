// WebRTC 引擎 FFI 桥接
// 对应 core/include/solra/solra_webrtc.h
//
// TODO: 当 C 侧 WebRTC API 实现后，取消注释并通过 ffi.rs 的全局单例调用

/// 连接到 WebRTC 房间 (stub)
pub fn connect(_room_id: &str, _token: &str) -> Result<(), String> {
    Err("webrtc::connect 尚未实现".into())
}

/// 发送数据 (stub)
pub fn send_data(_data: &[u8]) -> Result<(), String> {
    Err("webrtc::send_data 尚未实现".into())
}

/// 断开 WebRTC 连接 (stub)
pub fn disconnect() -> Result<(), String> {
    Err("webrtc::disconnect 尚未实现".into())
}
