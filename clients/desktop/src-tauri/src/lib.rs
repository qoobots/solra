// Solra Desktop — Rust 库入口
// 负责模块注册、Tauri Builder 配置、Core SDK 初始化

pub mod core;
pub mod ipc;
pub mod services;

use tauri::Manager;

/// 应用启动时的初始化逻辑
/// - 初始化日志
/// - 尝试加载 Core SDK (libsolracore.dll)
/// - 注册 IPC 命令
pub fn run() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_notification::init())
        .setup(|app| {
            log::info!("Solra Desktop 启动中...");

            // 尝试加载 Core SDK
            match core::ffi::load_core_sdk() {
                Ok(_) => log::info!("Core SDK 加载成功"),
                Err(e) => log::warn!("Core SDK 加载失败 (开发模式可忽略): {}", e),
            }

            // 获取主窗口引用
            let _window = app.get_webview_window("main")
                .expect("主窗口未找到");

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            ipc::space_cmd::get_spaces,
            ipc::space_cmd::get_space_detail,
            ipc::space_cmd::enter_space,
            ipc::space_cmd::exit_space,
            ipc::avatar_cmd::start_conversation,
            ipc::avatar_cmd::send_message,
            ipc::avatar_cmd::stop_conversation,
            ipc::render_cmd::init_renderer,
            ipc::render_cmd::resize_renderer,
            ipc::render_cmd::get_fps,
            ipc::system_cmd::get_system_info,
            ipc::system_cmd::get_core_version,
        ])
        .run(tauri::generate_context!())
        .expect("启动 Solra Desktop 失败");
}
