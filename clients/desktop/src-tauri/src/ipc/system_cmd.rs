// 系统信息 Tauri IPC 命令

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::services::api_client::ApiClient;
use crate::services::auth_service::{AuthService, UserProfile};

#[derive(Debug, Serialize, Deserialize)]
pub struct SystemInfo {
    pub os: String,
    pub arch: String,
    pub cpu_cores: u32,
    pub total_memory_gb: f32,
    pub gpu_name: String,
    pub core_sdk_loaded: bool,
    pub core_sdk_version: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ProfileUpdateRequest {
    pub display_name: Option<String>,
    pub bio: Option<String>,
    pub avatar_url: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct StoreItem {
    pub id: String,
    pub name: String,
    pub description: String,
    pub price: f64,
    pub currency: String,
    pub category: String,
    pub thumbnail_url: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct MessageItem {
    pub id: String,
    pub msg_type: String,
    pub title: String,
    pub content: String,
    pub sender: String,
    pub timestamp: String,
    pub read: bool,
}

/// 获取系统信息
#[tauri::command]
pub async fn get_system_info() -> Result<SystemInfo, String> {
    let core_loaded = super::super::core::ffi::CoreSdk::is_initialized();
    let core_version = if core_loaded {
        "0.1.0".to_string()
    } else {
        "未加载".to_string()
    };

    Ok(SystemInfo {
        os: std::env::consts::OS.to_string(),
        arch: std::env::consts::ARCH.to_string(),
        cpu_cores: num_cpus::get() as u32,
        total_memory_gb: detect_total_memory(),
        gpu_name: detect_gpu_name(),
        core_sdk_loaded: core_loaded,
        core_sdk_version: core_version,
    })
}

/// 获取 Core SDK 版本
#[tauri::command]
pub async fn get_core_version() -> Result<String, String> {
    use std::ffi::CStr;

    let sdk = super::super::core::ffi::CoreSdk::get()
        .ok_or("Core SDK 未加载")?;

    let version = unsafe {
        let ptr = (sdk.version)();
        if ptr.is_null() {
            "unknown"
        } else {
            CStr::from_ptr(ptr).to_str().unwrap_or("unknown")
        }
    };

    Ok(version.to_string())
}

/// 更新用户个人资料
#[tauri::command]
pub async fn update_profile(
    request: ProfileUpdateRequest,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<UserProfile, String> {
    log::info!("update_profile: display_name={:?}", request.display_name);

    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    let body = serde_json::json!({
        "displayName": request.display_name,
        "bio": request.bio,
        "avatarUrl": request.avatar_url,
    });

    match api.post::<serde_json::Value, _>("/api/auth/v1/profile", &body, Some(&token)).await {
        Ok(data) => {
            let profile = UserProfile {
                id: data["userId"].as_str().unwrap_or("").to_string(),
                username: data["username"].as_str().unwrap_or("").to_string(),
                display_name: data["displayName"].as_str().unwrap_or("").to_string(),
                avatar_url: data["avatarUrl"].as_str().map(String::from),
                subscription_tier: data["subscriptionTier"].as_str().unwrap_or("Free").to_string(),
            };
            auth.set_profile(profile.clone());
            Ok(profile)
        }
        Err(e) => {
            // API 不可用时本地更新
            log::warn!("更新资料 API 不可用，本地保存: {}", e);
            let profile = UserProfile {
                id: "local-user".to_string(),
                username: "local_user".to_string(),
                display_name: request.display_name.unwrap_or_default(),
                avatar_url: request.avatar_url,
                subscription_tier: "Free".to_string(),
            };
            auth.set_profile(profile.clone());
            Ok(profile)
        }
    }
}

/// 获取用户个人资料
#[tauri::command]
pub async fn get_profile(
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<UserProfile, String> {
    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    match api.get::<serde_json::Value>("/api/auth/v1/profile", Some(&token)).await {
        Ok(data) => {
            let profile = UserProfile {
                id: data["userId"].as_str().unwrap_or("").to_string(),
                username: data["username"].as_str().unwrap_or("").to_string(),
                display_name: data["displayName"].as_str().unwrap_or("").to_string(),
                avatar_url: data["avatarUrl"].as_str().map(String::from),
                subscription_tier: data["subscriptionTier"].as_str().unwrap_or("Free").to_string(),
            };
            auth.set_profile(profile.clone());
            Ok(profile)
        }
        Err(e) => {
            log::warn!("获取资料 API 不可用: {}", e);
            Err("无法获取用户资料".to_string())
        }
    }
}

/// 获取商城商品列表
#[tauri::command]
pub async fn get_store_items(
    category: Option<String>,
    api: State<'_, ApiClient>,
) -> Result<Vec<StoreItem>, String> {
    log::info!("get_store_items: category={:?}", category);

    let mut path = "/api/store/v1/items".to_string();
    if let Some(cat) = &category {
        path = format!("{}?category={}", path, cat);
    }

    match api.get::<serde_json::Value>(&path, None).await {
        Ok(data) => {
            let empty_vec = vec![];
            let items = data["items"].as_array()
                .or_else(|| data["data"].as_array())
                .unwrap_or(&empty_vec);

            Ok(items.iter().map(|i| StoreItem {
                id: i["itemId"].as_str().or(i["id"].as_str()).unwrap_or("").to_string(),
                name: i["name"].as_str().or(i["title"].as_str()).unwrap_or("").to_string(),
                description: i["description"].as_str().unwrap_or("").to_string(),
                price: i["price"].as_f64().unwrap_or(0.0),
                currency: i["currency"].as_str().unwrap_or("CNY").to_string(),
                category: i["category"].as_str().unwrap_or("").to_string(),
                thumbnail_url: i["thumbnailUrl"].as_str().unwrap_or("").to_string(),
            }).collect())
        }
        Err(e) => {
            log::warn!("商城 API 不可用: {}", e);
            Ok(get_mock_store_items())
        }
    }
}

/// 获取消息列表
#[tauri::command]
pub async fn get_messages(
    tab: Option<String>,
    api: State<'_, ApiClient>,
    auth: State<'_, AuthService>,
) -> Result<Vec<MessageItem>, String> {
    log::info!("get_messages: tab={:?}", tab);

    let token = auth.get_access_token()
        .ok_or("未登录，请先登录")?;

    let mut path = "/api/msg/v1/messages".to_string();
    if let Some(t) = &tab {
        path = format!("{}?tab={}", path, t);
    }

    match api.get::<serde_json::Value>(&path, Some(&token)).await {
        Ok(data) => {
            let empty_vec = vec![];
            let messages = data["messages"].as_array()
                .or_else(|| data["data"].as_array())
                .unwrap_or(&empty_vec);

            Ok(messages.iter().map(|m| MessageItem {
                id: m["messageId"].as_str().or(m["id"].as_str()).unwrap_or("").to_string(),
                msg_type: m["type"].as_str().unwrap_or("system").to_string(),
                title: m["title"].as_str().unwrap_or("").to_string(),
                content: m["content"].as_str().unwrap_or("").to_string(),
                sender: m["sender"].as_str().unwrap_or("系统").to_string(),
                timestamp: m["timestamp"].as_str().unwrap_or("").to_string(),
                read: m["read"].as_bool().unwrap_or(false),
            }).collect())
        }
        Err(e) => {
            log::warn!("消息 API 不可用: {}", e);
            Ok(vec![])
        }
    }
}

// ---- 系统信息检测 ----

#[cfg(target_os = "windows")]
fn detect_total_memory() -> f32 {
    use std::mem;
    unsafe {
        #[allow(non_snake_case)]
        extern "system" {
            fn GlobalMemoryStatusEx(lpBuffer: *mut MEMORYSTATUSEX) -> i32;
        }
        #[repr(C)]
        #[derive(Debug)]
        #[allow(non_snake_case)]
        struct MEMORYSTATUSEX {
            dwLength: u32,
            dwMemoryLoad: u32,
            ullTotalPhys: u64,
            ullAvailPhys: u64,
            ullTotalPageFile: u64,
            ullAvailPageFile: u64,
            ullTotalVirtual: u64,
            ullAvailVirtual: u64,
            ullAvailExtendedVirtual: u64,
        }
        let mut mem = MEMORYSTATUSEX {
            dwLength: mem::size_of::<MEMORYSTATUSEX>() as u32,
            dwMemoryLoad: 0,
            ullTotalPhys: 0,
            ullAvailPhys: 0,
            ullTotalPageFile: 0,
            ullAvailPageFile: 0,
            ullTotalVirtual: 0,
            ullAvailVirtual: 0,
            ullAvailExtendedVirtual: 0,
        };
        if GlobalMemoryStatusEx(&mut mem) != 0 {
            (mem.ullTotalPhys as f64 / (1024.0 * 1024.0 * 1024.0)) as f32
        } else {
            0.0
        }
    }
}

#[cfg(not(target_os = "windows"))]
fn detect_total_memory() -> f32 {
    // Linux/macOS: 读取 /proc/meminfo 或使用 sysctl
    if let Ok(content) = std::fs::read_to_string("/proc/meminfo") {
        for line in content.lines() {
            if line.starts_with("MemTotal:") {
                let kb: u64 = line.split_whitespace()
                    .nth(1)
                    .and_then(|v| v.parse().ok())
                    .unwrap_or(0);
                return (kb as f64 / (1024.0 * 1024.0)) as f32;
            }
        }
    }
    0.0
}

#[cfg(target_os = "windows")]
fn detect_gpu_name() -> String {
    // 通过 WMI 或注册表查询 GPU 信息
    // 简单方案：尝试用命令行查询
    if let Ok(output) = std::process::Command::new("wmic")
        .args(["path", "win32_VideoController", "get", "name"])
        .output()
    {
        let text = String::from_utf8_lossy(&output.stdout);
        for line in text.lines().skip(1) {
            let trimmed = line.trim();
            if !trimmed.is_empty() {
                return trimmed.to_string();
            }
        }
    }
    "Unknown GPU".to_string()
}

#[cfg(target_os = "macos")]
fn detect_gpu_name() -> String {
    if let Ok(output) = std::process::Command::new("system_profiler")
        .args(["SPDisplaysDataType"])
        .output()
    {
        let text = String::from_utf8_lossy(&output.stdout);
        for line in text.lines() {
            if line.contains("Chipset Model:") {
                return line.split(':').nth(1).unwrap_or("Unknown GPU").trim().to_string();
            }
        }
    }
    "Unknown GPU".to_string()
}

#[cfg(not(any(target_os = "windows", target_os = "macos")))]
fn detect_gpu_name() -> String {
    // Linux: lspci
    if let Ok(output) = std::process::Command::new("lspci")
        .output()
    {
        let text = String::from_utf8_lossy(&output.stdout);
        for line in text.lines() {
            if line.contains("VGA") || line.contains("3D") {
                return line.split(':').nth(2).unwrap_or("Unknown GPU").trim().to_string();
            }
        }
    }
    "Unknown GPU".to_string()
}

fn get_mock_store_items() -> Vec<StoreItem> {
    vec![
        StoreItem {
            id: "item-001".into(),
            name: "虚拟人-小雅".into(),
            description: "温柔知性的AI虚拟人，擅长聊天和情感陪伴".into(),
            price: 0.0,
            currency: "CNY".into(),
            category: "avatar".into(),
            thumbnail_url: "https://cdn.solra.io/store/avatar-xiaoya.jpg".into(),
        },
        StoreItem {
            id: "item-002".into(),
            name: "赛博朋克城市".into(),
            description: "未来风格的城市场景模板，包含霓虹灯和全息广告".into(),
            price: 29.9,
            currency: "CNY".into(),
            category: "scene".into(),
            thumbnail_url: "https://cdn.solra.io/store/scene-cyberpunk.jpg".into(),
        },
        StoreItem {
            id: "item-003".into(),
            name: "烟花特效包".into(),
            description: "10种烟花粒子特效，可自定义颜色和绽放模式".into(),
            price: 9.9,
            currency: "CNY".into(),
            category: "effect".into(),
            thumbnail_url: "https://cdn.solra.io/store/effect-firework.jpg".into(),
        },
        StoreItem {
            id: "item-004".into(),
            name: "魔法棒道具".into(),
            description: "交互式魔法棒，可在空间中释放光效".into(),
            price: 6.9,
            currency: "CNY".into(),
            category: "prop".into(),
            thumbnail_url: "https://cdn.solra.io/store/prop-wand.jpg".into(),
        },
    ]
}
