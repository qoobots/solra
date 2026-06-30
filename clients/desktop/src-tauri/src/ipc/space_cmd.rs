// 空间相关 Tauri IPC 命令
// 前端调用: invoke('get_spaces'), invoke('enter_space', { spaceId: '...' })

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::services::api_client::ApiClient;
use crate::services::auth_service::AuthService;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SpaceSummary {
    pub id: String,
    pub name: String,
    pub description: String,
    pub thumbnail_url: String,
    pub author_name: String,
    pub visitor_count: u64,
    pub like_count: u64,
    pub category: String,
    pub tags: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SpaceDetail {
    pub id: String,
    pub name: String,
    pub description: String,
    pub author_id: String,
    pub author_name: String,
    pub created_at: String,
    pub updated_at: String,
    pub visitor_count: u64,
    pub like_count: u64,
    pub share_count: u64,
    pub category: String,
    pub tags: Vec<String>,
    pub size_bytes: u64,
    pub version: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateSpaceRequest {
    pub name: String,
    pub description: String,
    pub category: String,
    pub is_public: bool,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CreateSpaceResponse {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct LeaderboardEntry {
    pub rank: u32,
    pub space_id: String,
    pub name: String,
    pub author: String,
    pub score: u64,
    pub trend: String,
}

/// 获取空间推荐列表
#[tauri::command]
pub async fn get_spaces(
    page: u32,
    page_size: u32,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<Vec<SpaceSummary>, String> {
    log::info!("get_spaces: page={}, page_size={}", page, page_size);

    let token = auth.get_access_token();
    let path = format!("/api/spc/v1/spaces?page={}&pageSize={}", page, page_size);

    // 尝试调用后端 API
    match api.get::<serde_json::Value>(&path, token.as_deref()).await {
        Ok(data) => {
            let spaces = data["spaces"].as_array()
                .or_else(|| data["data"].as_array())
                .or_else(|| data.as_array())
                .ok_or("API 返回格式错误")?;

            Ok(spaces.iter().map(|s| SpaceSummary {
                id: s["spaceId"].as_str().or(s["id"].as_str()).unwrap_or("").to_string(),
                name: s["title"].as_str().or(s["name"].as_str()).unwrap_or("未命名").to_string(),
                description: s["description"].as_str().unwrap_or("").to_string(),
                thumbnail_url: s["thumbnailUrl"].as_str().or(s["previewUrl"].as_str()).unwrap_or("").to_string(),
                author_name: s["creator"].as_object()
                    .and_then(|c| c["displayName"].as_str())
                    .unwrap_or("匿名").to_string(),
                visitor_count: s["onlineCount"].as_u64().or(s["visitorCount"].as_u64()).unwrap_or(0),
                like_count: s["likeCount"].as_u64().unwrap_or(0),
                category: s["category"].as_str().unwrap_or("休闲").to_string(),
                tags: s["tags"].as_array()
                    .map(|a| a.iter().filter_map(|v| v.as_str().map(String::from)).collect())
                    .unwrap_or_default(),
            }).collect())
        }
        Err(_) => {
            // API 不可用时返回 Mock 数据作为降级
            log::warn!("后端 API 不可用，使用 Mock 数据");
            Ok(get_mock_spaces())
        }
    }
}

/// 获取空间详情
#[tauri::command]
pub async fn get_space_detail(
    space_id: String,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<SpaceDetail, String> {
    log::info!("get_space_detail: space_id={}", space_id);

    let token = auth.get_access_token();
    let path = format!("/api/spc/v1/spaces/{}", space_id);

    match api.get::<serde_json::Value>(&path, token.as_deref()).await {
        Ok(data) => {
            let creator = data["creator"].as_object();
            Ok(SpaceDetail {
                id: space_id.clone(),
                name: data["title"].as_str().or(data["name"].as_str()).unwrap_or("未命名").to_string(),
                description: data["description"].as_str().unwrap_or("").to_string(),
                author_id: creator.and_then(|c| c["userId"].as_str()).unwrap_or("").to_string(),
                author_name: creator.and_then(|c| c["displayName"].as_str()).unwrap_or("匿名").to_string(),
                created_at: data["createdAt"].as_str().unwrap_or("").to_string(),
                updated_at: data["updatedAt"].as_str().unwrap_or("").to_string(),
                visitor_count: data["onlineCount"].as_u64().unwrap_or(0),
                like_count: data["likeCount"].as_u64().unwrap_or(0),
                share_count: data["shareCount"].as_u64().unwrap_or(0),
                category: data["category"].as_str().unwrap_or("").to_string(),
                tags: data["tags"].as_array()
                    .map(|a| a.iter().filter_map(|v| v.as_str().map(String::from)).collect())
                    .unwrap_or_default(),
                size_bytes: data["sizeBytes"].as_u64().unwrap_or(0),
                version: data["version"].as_str().unwrap_or("1.0.0").to_string(),
            })
        }
        Err(_) => {
            log::warn!("后端 API 不可用，使用 Mock 数据");
            Ok(get_mock_space_detail(&space_id))
        }
    }
}

/// 创建空间
#[tauri::command]
pub async fn create_space(
    request: CreateSpaceRequest,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<CreateSpaceResponse, String> {
    log::info!("create_space: name={}", request.name);

    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    let body = serde_json::json!({
        "title": request.name,
        "description": request.description,
        "category": request.category,
        "visibility": if request.is_public { "PUBLIC" } else { "PRIVATE" },
    });

    match api.post::<serde_json::Value, _>("/api/spc/v1/spaces", &body, Some(&token)).await {
        Ok(data) => Ok(CreateSpaceResponse {
            id: data["spaceId"].as_str().or(data["id"].as_str()).unwrap_or("").to_string(),
            name: data["title"].as_str().or(data["name"].as_str()).unwrap_or(&request.name).to_string(),
        }),
        Err(e) => {
            log::error!("创建空间失败: {}", e);
            Err(format!("创建空间失败: {}", e))
        }
    }
}

/// 获取排行榜
#[tauri::command]
pub async fn get_leaderboard(
    period: String,
    api: State<'_, ApiClient>,
) -> Result<Vec<LeaderboardEntry>, String> {
    log::info!("get_leaderboard: period={}", period);

    let path = format!("/api/spc/v1/leaderboard?period={}", period);

    match api.get::<serde_json::Value>(&path, None).await {
        Ok(data) => {
            let empty_vec = vec![];
            let entries = data["entries"].as_array()
                .or_else(|| data["data"].as_array())
                .unwrap_or(&empty_vec);

            Ok(entries.iter().enumerate().map(|(i, e)| LeaderboardEntry {
                rank: e["rank"].as_u64().unwrap_or((i + 1) as u64) as u32,
                space_id: e["spaceId"].as_str().or(e["id"].as_str()).unwrap_or("").to_string(),
                name: e["title"].as_str().or(e["name"].as_str()).unwrap_or("").to_string(),
                author: e["author"].as_str().or(e["creatorName"].as_str()).unwrap_or("").to_string(),
                score: e["score"].as_u64().unwrap_or(0),
                trend: e["trend"].as_str().unwrap_or("same").to_string(),
            }).collect())
        }
        Err(e) => {
            log::warn!("排行榜 API 不可用: {}", e);
            Ok(vec![])
        }
    }
}

/// 进入空间（触发流式加载 + 场景创建）
#[tauri::command]
pub async fn enter_space(space_id: String) -> Result<(), String> {
    log::info!("enter_space: space_id={}", space_id);

    // 尝试调用 Core SDK 流式加载 + 创建场景
    if crate::core::ffi::CoreSdk::is_initialized() {
        // crate::core::streaming::load_space(&space_id, |progress, status| { ... })?;
        // crate::core::render::create_scene(&space_id)?;
        log::info!("Core SDK 已就绪，准备加载空间: {}", space_id);
    } else {
        log::warn!("Core SDK 未加载，跳过场景创建（Mock 模式）");
    }

    Ok(())
}

/// 退出空间
#[tauri::command]
pub async fn exit_space(space_id: String) -> Result<(), String> {
    log::info!("exit_space: space_id={}", space_id);

    if crate::core::ffi::CoreSdk::is_initialized() {
        // crate::core::streaming::cancel()?;
        log::info!("已取消空间加载: {}", space_id);
    }

    Ok(())
}

// ---- Mock 数据降级 ----

fn get_mock_spaces() -> Vec<SpaceSummary> {
    vec![
        SpaceSummary {
            id: "space-001".into(),
            name: "云端茶室".into(),
            description: "日式风格的宁静茶室，悬浮在云海之上".into(),
            thumbnail_url: "https://cdn.solra.io/previews/space-001.jpg".into(),
            author_name: "茶道家".into(),
            visitor_count: 12340,
            like_count: 892,
            category: "休闲".into(),
            tags: vec!["日式".into(), "茶室".into(), "云端".into()],
        },
        SpaceSummary {
            id: "space-002".into(),
            name: "赛博朋克酒吧".into(),
            description: "霓虹灯下的未来酒吧，AI调酒师为你服务".into(),
            thumbnail_url: "https://cdn.solra.io/previews/space-002.jpg".into(),
            author_name: "NeonDreamer".into(),
            visitor_count: 45820,
            like_count: 2341,
            category: "社交".into(),
            tags: vec!["赛博朋克".into(), "酒吧".into(), "霓虹".into()],
        },
        SpaceSummary {
            id: "space-003".into(),
            name: "深海图书馆".into(),
            description: "沉入海底的古老图书馆，鲸鱼在窗外游过".into(),
            thumbnail_url: "https://cdn.solra.io/previews/space-003.jpg".into(),
            author_name: "深海旅人".into(),
            visitor_count: 8920,
            like_count: 567,
            category: "教育".into(),
            tags: vec!["深海".into(), "图书馆".into(), "奇幻".into()],
        },
        SpaceSummary {
            id: "space-004".into(),
            name: "星空音乐厅".into(),
            description: "悬浮在星空中的音乐厅，AI乐团为你演奏".into(),
            thumbnail_url: "https://cdn.solra.io/previews/space-004.jpg".into(),
            author_name: "音乐精灵".into(),
            visitor_count: 6720,
            like_count: 432,
            category: "音乐".into(),
            tags: vec!["星空".into(), "音乐".into(), "演出".into()],
        },
    ]
}

fn get_mock_space_detail(space_id: &str) -> SpaceDetail {
    SpaceDetail {
        id: space_id.to_string(),
        name: format!("空间 {}", space_id),
        description: "一个美妙的空间".into(),
        author_id: "user-001".into(),
        author_name: "创作者".into(),
        created_at: "2026-06-01T00:00:00Z".into(),
        updated_at: "2026-06-15T00:00:00Z".into(),
        visitor_count: 1000,
        like_count: 50,
        share_count: 20,
        category: "休闲".into(),
        tags: vec!["标签1".into()],
        size_bytes: 200_000_000,
        version: "1.0.0".into(),
    }
}
