// Core SDK FFI 桥接模块
// 提供 Rust → C ABI 的 unsafe 桥接，连接 C++17 Core SDK

pub mod ffi;
pub mod render;
pub mod inference;
pub mod streaming;
pub mod webrtc;
pub mod animation;
