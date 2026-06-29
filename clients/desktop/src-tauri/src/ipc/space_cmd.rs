// 空间相关 Tauri IPC 命令
// 前端调用: invoke('get_spaces'), invoke('enter_space', { spaceId: '...' })

use serde::{Deserialize, Serialize};

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

/// 获取空间推荐列表
#[tauri::command]
pub async fn get_spaces(page: u32, page_size: u32) -> Result<Vec<SpaceSummary>, String> {
    log::info!("get_spaces: page={}, page_size={}", page, page_size);

    // TODO: 调用后端 API 获取空间列表
    // 当前返回 Mock 数据
    Ok(vec![
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
    ])
}

/// 获取空间详情
#[tauri::command]
pub async fn get_space_detail(space_id: String) -> Result<SpaceDetail, String> {
    log::info!("get_space_detail: space_id={}", space_id);

    // TODO: 调用后端 API
    Ok(SpaceDetail {
        id: space_id.clone(),
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
    })
}

/// 进入空间（触发流式加载 + 场景创建）
#[tauri::command]
pub async fn enter_space(space_id: String) -> Result<(), String> {
    log::info!("enter_space: space_id={}", space_id);

    // TODO: 调用 Core SDK 流式加载 + 创建场景
    // crate::core::streaming::load_space(&space_id, |progress, status| { ... })?;
    // crate::core::render::create_scene(&space_id)?;

    Ok(())
}

/// 退出空间
#[tauri::command]
pub async fn exit_space(space_id: String) -> Result<(), String> {
    log::info!("exit_space: space_id={}", space_id);

    // TODO: 清理场景 + 取消加载
    // crate::core::streaming::cancel()?;

    Ok(())
}
