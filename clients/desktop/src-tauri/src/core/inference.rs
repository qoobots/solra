// 端侧推理引擎 FFI 桥接
// 对应 core/include/solra/solra_inference.h
//
// TODO: 当 C 侧推理 API 实现后，取消注释并通过 ffi.rs 的全局单例调用

/// 推理回调类型
pub type InferenceCallback = Box<dyn Fn(&str) + Send + 'static>;

/// 加载推理模型 (stub)
pub fn load_model(_model_path: &str) -> Result<(), String> {
    Err("inference::load_model 尚未实现".into())
}

/// 发送消息到端侧推理引擎（带流式回调）(stub)
pub fn send_message(
    _conversation_id: &str,
    _message: &str,
    _callback: InferenceCallback,
) -> Result<(), String> {
    Err("inference::send_message 尚未实现".into())
}

/// 停止推理 (stub)
pub fn stop() -> Result<(), String> {
    Err("inference::stop 尚未实现".into())
}
