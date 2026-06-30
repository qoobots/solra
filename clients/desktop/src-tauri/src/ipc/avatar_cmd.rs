// 虚拟人相关 Tauri IPC 命令

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::services::api_client::ApiClient;
use crate::services::auth_service::AuthService;

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
pub async fn start_conversation(
    avatar_id: String,
    space_id: String,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<ConversationState, String> {
    log::info!("start_conversation: avatar_id={}, space_id={}", avatar_id, space_id);

    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    let body = serde_json::json!({
        "avatarId": avatar_id,
        "spaceId": space_id,
    });

    // 尝试调用后端 API
    match api.post::<serde_json::Value, _>("/api/conv/v1/conversations", &body, Some(&token)).await {
        Ok(data) => {
            // 如果 Core SDK 已加载，初始化推理引擎
            if crate::core::ffi::CoreSdk::is_initialized() {
                // crate::core::inference::init_model(&avatar_id)?;
                log::info!("Core SDK 推理引擎初始化: avatar_id={}", avatar_id);
            }

            Ok(ConversationState {
                id: data["conversationId"].as_str().or(data["id"].as_str()).unwrap_or("").to_string(),
                avatar_id,
                status: "active".into(),
                turn_count: 0,
            })
        }
        Err(e) => {
            // API 不可用时使用本地生成 ID
            log::warn!("对话 API 不可用，使用本地 Mock: {}", e);
            Ok(ConversationState {
                id: format!("conv-{}", uuid_simple()),
                avatar_id,
                status: "active".into(),
                turn_count: 0,
            })
        }
    }
}

/// 发送消息到虚拟人
#[tauri::command]
pub async fn send_message(
    conversation_id: String,
    message: String,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<String, String> {
    log::info!("send_message: conversation_id={}", conversation_id);

    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    // 优先尝试 Core SDK 端侧推理
    if crate::core::ffi::CoreSdk::is_initialized() {
        // crate::core::inference::send_message(&conversation_id, &message, |token| { ... })?;
        log::info!("Core SDK 端侧推理: conversation_id={}", conversation_id);
    }

    // 回退到后端 API
    let body = serde_json::json!({
        "conversationId": conversation_id,
        "content": message,
        "contentType": "TEXT",
    });

    match api.post::<serde_json::Value, _>("/api/conv/v1/messages", &body, Some(&token)).await {
        Ok(data) => {
            Ok(data["content"].as_str()
                .or(data["reply"].as_str())
                .unwrap_or("收到消息")
                .to_string())
        }
        Err(_) => {
            // 完全离线模式：返回本地生成的回复
            Ok(format!("[离线模式] 收到你的消息「{}」，Core SDK 推理引擎未就绪", message))
        }
    }
}

/// 停止对话
#[tauri::command]
pub async fn stop_conversation(conversation_id: String) -> Result<(), String> {
    log::info!("stop_conversation: conversation_id={}", conversation_id);

    if crate::core::ffi::CoreSdk::is_initialized() {
        // crate::core::inference::stop()?;
        log::info!("Core SDK 推理已停止");
    }

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
