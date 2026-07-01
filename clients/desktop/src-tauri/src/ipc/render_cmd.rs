// 渲染控制 Tauri IPC 命令

use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct RendererState {
    pub initialized: bool,
    pub fps: f32,
    pub width: u32,
    pub height: u32,
    pub gpu_backend: String,
    /// Core SDK 原生渲染库是否已实际加载（非 Mock 模式）
    #[serde(default)]
    pub core_sdk_loaded: bool,
}

/// 初始化渲染器
#[tauri::command]
pub async fn init_renderer(width: u32, height: u32) -> Result<RendererState, String> {
    log::info!("init_renderer: {}x{}", width, height);

    // 检测 Core SDK 是否真实可用
    let core_loaded = crate::core::ffi::is_core_sdk_loaded();

    let gpu_backend = if core_loaded {
        // 尝试初始化 Core SDK 渲染引擎
        match crate::core::render::init_render(width as i32, height as i32) {
            Ok(()) => {
                log::info!("Core SDK 渲染引擎就绪");
                // 查询 GPU 信息
                match crate::core::render::get_gpu_info() {
                    Ok(info) => format!("Core SDK · {}", info.renderer),
                    Err(_) => "Core SDK · OpenGL".into(),
                }
            }
            Err(e) => {
                log::warn!("Core SDK 渲染引擎初始化失败: {}，降级到 Three.js", e);
                "Three.js WebGL (Core SDK 渲染初始化失败)".into()
            }
        }
    } else {
        "Three.js WebGL".into()
    };

    Ok(RendererState {
        initialized: true,
        fps: if core_loaded {
            crate::core::render::get_fps().unwrap_or(60.0)
        } else {
            60.0
        },
        width,
        height,
        gpu_backend,
        core_sdk_loaded: core_loaded,
    })
}

/// 调整渲染器尺寸
#[tauri::command]
pub async fn resize_renderer(width: u32, height: u32) -> Result<(), String> {
    log::info!("resize_renderer: {}x{}", width, height);

    if crate::core::ffi::is_core_sdk_loaded() {
        crate::core::render::resize(width as i32, height as i32)?;
    }

    Ok(())
}

/// 获取当前 FPS
#[tauri::command]
pub async fn get_fps() -> Result<f32, String> {
    if crate::core::ffi::is_core_sdk_loaded() {
        crate::core::render::get_fps()
    } else {
        Ok(60.0)
    }
}

/// 获取 GPU 信息（供调试/诊断使用）
#[tauri::command]
pub async fn get_gpu_info() -> Result<serde_json::Value, String> {
    if crate::core::ffi::is_core_sdk_loaded() {
        match crate::core::render::get_gpu_info() {
            Ok(info) => Ok(serde_json::json!({
                "vendor": info.vendor,
                "renderer": info.renderer,
                "version": info.version,
                "dedicated_vram_mb": info.dedicated_vram_mb,
                "shared_vram_mb": info.shared_vram_mb,
            })),
            Err(e) => Err(e),
        }
    } else {
        Err("Core SDK 未加载".into())
    }
}
