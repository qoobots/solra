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

    // TODO: 初始化 Core SDK 渲染引擎 + 创建 OpenGL 上下文
    // if core_loaded {
    //     crate::core::render::resize(width as i32, height as i32)?;
    // }

    Ok(RendererState {
        initialized: true,
        fps: 60.0,
        width,
        height,
        gpu_backend: if core_loaded { "OpenGL".into() } else { "Mock".into() },
        core_sdk_loaded: core_loaded,
    })
}

/// 调整渲染器尺寸
#[tauri::command]
pub async fn resize_renderer(width: u32, height: u32) -> Result<(), String> {
    log::info!("resize_renderer: {}x{}", width, height);

    // crate::core::render::resize(width as i32, height as i32)?;

    Ok(())
}

/// 获取当前 FPS
#[tauri::command]
pub async fn get_fps() -> Result<f32, String> {
    // crate::core::render::get_fps()
    Ok(60.0)
}
