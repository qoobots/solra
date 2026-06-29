// 渲染控制 Tauri IPC 命令

use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct RendererState {
    pub initialized: bool,
    pub fps: f32,
    pub width: u32,
    pub height: u32,
    pub gpu_backend: String,
}

/// 初始化渲染器
#[tauri::command]
pub async fn init_renderer(width: u32, height: u32) -> Result<RendererState, String> {
    log::info!("init_renderer: {}x{}", width, height);

    // TODO: 初始化 Core SDK 渲染引擎 + 创建 OpenGL 上下文
    // crate::core::ffi::init_core_engine(&config)?;
    // crate::core::render::resize(width as i32, height as i32)?;

    Ok(RendererState {
        initialized: true,
        fps: 60.0,
        width,
        height,
        gpu_backend: "OpenGL".into(),
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
