// 虚拟人相关 Tauri IPC 命令

use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct AvatarInfo {
    pub id: String,
    pub name: String,
    pub personality: String,
    pub emotion: String,
    pub affinity_level: u32,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ConversationState {
    pub id: String,
    pub avatar_id: String,
    pub status: String, // "active" | "paused" | "ended"
    pub turn_count: u32,
}

/// 开始与虚拟人对话
#[tauri::command]
pub async fn start_conversation(avatar_id: String, space_id: String) -> Result<ConversationState, String> {
    log::info!("start_conversation: avatar_id={}, space_id={}", avatar_id, space_id);

    // TODO: 调用后端 API 创建对话 + Core SDK 推理引擎初始化

    Ok(ConversationState {
        id: format!("conv-{}", uuid_simple()),
        avatar_id,
        status: "active".into(),
        turn_count: 0,
    })
}

/// 发送消息到虚拟人
#[tauri::command]
pub async fn send_message(conversation_id: String, message: String) -> Result<String, String> {
    log::info!("send_message: conversation_id={}", conversation_id);

    // TODO: 调用 Core SDK 端侧推理
    // crate::core::inference::send_message(&conversation_id, &message, |token| { ... })?;

    Ok(format!("[Mock] 收到回复: 你说的是「{}」吗？", message))
}

/// 停止对话
#[tauri::command]
pub async fn stop_conversation(conversation_id: String) -> Result<(), String> {
    log::info!("stop_conversation: conversation_id={}", conversation_id);

    // TODO: crate::core::inference::stop()?;

    Ok(())
}

/// 简单 UUID 生成（避免额外依赖）
fn uuid_simple() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let ts = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    format!("{:016x}", ts)
}
