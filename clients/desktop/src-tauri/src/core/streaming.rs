// 流式加载引擎 FFI 桥接
// 对应 core/include/solra/solra_streaming.h
//
// TODO: 当 C 侧流式加载 API 实现后，取消注释并通过 ffi.rs 的全局单例调用

/// 加载进度回调
pub type ProgressCallback = Box<dyn Fn(f32, &str) + Send + 'static>;

/// 流式加载空间 (stub)
pub fn load_space(
    _space_id: &str,
    _callback: ProgressCallback,
) -> Result<(), String> {
    Err("streaming::load_space 尚未实现".into())
}

/// 取消流式加载 (stub)
pub fn cancel() -> Result<(), String> {
    Err("streaming::cancel 尚未实现".into())
}

/// 获取缓存大小 (stub)
pub fn get_cache_size() -> Result<usize, String> {
    Err("streaming::get_cache_size 尚未实现".into())
}
